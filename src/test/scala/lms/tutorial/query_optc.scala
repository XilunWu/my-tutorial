/**
Query Compiler III (C Code)
===========================

Outline:
<div id="tableofcontents"></div>

*/
package scala.lms.tutorial

import scala.virtualization.lms.common._

object query_optc {
trait QueryCompiler extends Dsl with StagedQueryProcessor
with ScannerLowerBase {
  override def version = "query_optc"

/**
Input File Tokenizer
--------------------
*/
  class Scanner(name: Rep[String]) {
    val fd = open(name)
    val fl = filelen(fd)
    val data = mmap[Char](fd,fl)
    var pos = 0

    def next(d: Rep[Char]) = {
      val start = pos: Rep[Int] // force read
      while (data(pos) != d) pos += 1
      val len = pos - start
      pos += 1
      RString(stringFromCharArray(data,start,len), len)
    }

    def nextInt(d: Rep[Char]) = {
      val start = pos: Rep[Int] // force read
      var num = 0
      while (data(pos) != d) {
        num = num * 10 + (data(pos) - '0').toInt
        pos += 1
      }
      pos += 1
      RInt(num)
    }

    def hasNext = pos < fl
    def close = fclose(fd)
  }

/**
Low-Level Processing Logic
--------------------------
*/
  abstract class RField {
    def print()
    def compare(o: RField): Rep[Boolean]
    def hash: Rep[Long]
  }
  //make data visilble outside RString.
  case class RString(val data: Rep[String], len: Rep[Int]) extends RField {
    def print() = prints(data)
    def compare(o: RField) = o match { case RString(data2, len2) => if (len != len2) false else {
      // TODO: we may or may not want to inline this (code bloat and icache considerations).
      var i = 0
      while (i < len && data.charAt(i) == data2.charAt(i)) {
        i += 1
      }
      i == len
    }}
    def hash = data.HashCode(len)
  }
  //make value visilble outside RInt.
  case class RInt(val value: Rep[Int]) extends RField {
    def print() = printf("%d",value)
    def compare(o: RField) = o match { case RInt(v2) => value == v2 }
    def lessThan(o: RField) = o match { case RInt(v2) =>  value < v2 }
    def hash = value.asInstanceOf[Rep[Long]]
  }

  type Fields = Vector[RField]

  def isNumericCol(s: String) = s.startsWith("#")

  case class Record(fields: Fields, schema: Schema) {
    def apply(key: String): RField = fields(schema indexOf key)
    def apply(keys: Schema): Fields = keys.map(this apply _)
  }

  def processCSV(filename: Rep[String], schema: Schema, fieldDelimiter: Char, externalSchema: Boolean)(yld: Record => Rep[Unit]): Rep[Unit] = {
    val s = new Scanner(filename)
    val last = schema.last
    def nextField(name: String) = {
      val d = if (name==last) '\n' else fieldDelimiter
      if (isNumericCol(name)) s.nextInt(d) else s.next(d)
    }
    def nextRecord = Record(schema.map(nextField), schema)
    if (!externalSchema) {
      // the right thing would be to dynamically re-check the schema,
      // but it clutters the generated code
      // schema.foreach(f => if (s.next != f) println("ERROR: schema mismatch"))
      nextRecord // ignore csv header
    }
    while (s.hasNext) yld(nextRecord)
    s.close
  }

  def printSchema(schema: Schema) = println(schema.mkString(defaultFieldDelimiter.toString))

  def printFields(fields: Fields) = {
    if (fields.nonEmpty) {
      fields.head.print
      fields.tail.foreach { x  => printf(defaultFieldDelimiter.toString); x.print }
    }
    println("")
  }

  def fieldsEqual(a: Fields, b: Fields) = (a zip b).foldLeft(unit(true)) { (a,b) => b._1 compare b._2 }

  def fieldsHash(a: Fields) = a.foldLeft(unit(0L)) { _ * 41L + _.hash }

/**
Query Interpretation = Compilation
----------------------------------
*/
  def evalPred(p: Predicate)(rec: Record): Rep[Boolean] = p match {
    case Eq(a1, a2) => evalRef(a1)(rec) compare evalRef(a2)(rec)
  }

  def evalRef(r: Ref)(rec: Record): RField = r match {
    case Field(name) => rec(name)
    case Value(x:Int) => RInt(x)
    case Value(x) => RString(x.toString,x.toString.length)
  }

  def resultSchema(o: Operator): Schema = o match {
    case Scan(_, schema, _, _)   => schema
    case Filter(pred, parent)    => resultSchema(parent)
    case Project(schema, _, _)   => schema
    case Join(left, right)       => resultSchema(left) ++ resultSchema(right)
    case Group(keys, agg, parent)=> keys ++ agg
    case HashJoin(left, right)   => resultSchema(left) ++ resultSchema(right)
    case PrintCSV(parent)        => Schema()
    case LFTJoin(parents)        =>
        val schema = Schema("#ORDERKEY","#CUSTKEY","#PARTKEY","#SUPPKEY","#NATIONKEY","#REGIONKEY")
        schema
  }

  def execOp(o: Operator)(yld: Record => Rep[Unit]): Rep[Unit] = o match {
    case Scan(filename, schema, fieldDelimiter, externalSchema) =>
      processCSV(filename, schema, fieldDelimiter, externalSchema)(yld)
    case Filter(pred, parent) =>
      execOp(parent) { rec => if (evalPred(pred)(rec)) yld(rec) }
    case Project(newSchema, parentSchema, parent) =>
      execOp(parent) { rec => yld(Record(rec(parentSchema), newSchema)) }
    case Join(left, right) =>
      execOp(left) { rec1 =>
        execOp(right) { rec2 =>
          val keys = rec1.schema intersect rec2.schema
          if (fieldsEqual(rec1(keys), rec2(keys)))
            yld(Record(rec1.fields ++ rec2.fields, rec1.schema ++ rec2.schema))
        }
      }
    case Group(keys, agg, parent) =>
      val hm = new HashMapAgg(keys, agg)
      execOp(parent) { rec =>
        hm(rec(keys)) += rec(agg)
      }
      hm foreach { (k,a) =>
        yld(Record(k ++ a, keys ++ agg))
      }
    case HashJoin(left, right) =>
      val keys = resultSchema(left) intersect resultSchema(right)
      val hm = new HashMapBuffer(keys, resultSchema(left))
      execOp(left) { rec1 =>
        hm(rec1(keys)) += rec1.fields
      }
      execOp(right) { rec2 =>
        hm(rec2(keys)) foreach { rec1 =>
          yld(Record(rec1.fields ++ rec2.fields, rec1.schema ++ rec2.schema))
        }
      }
    case LFTJoin(parents) =>
      val dataSize = Vector(25+1,5+1,10000+1,150000+1,1500000+1,6001215+1)
      //val dataSize = Vector(25+1,5+1,10000+1,6+1,5+1,5+1)
      val schemaOfResult = resultSchema(LFTJoin(parents))
      val trieArrays = (parents,dataSize).zipped.map { (p,size) =>
        val buf = new TrieArray(size, resultSchema(p), schemaOfResult)
        execOp(p) {rec => buf += rec.fields} //fields is of type Fields: Vector[RField]
        buf
      }
      trieArrays foreach {arr => arr.toTrieArray}
      val join = new LFTJmain(trieArrays, schemaOfResult)
      join.run(yld)
    case PrintCSV(parent) =>
      val schema = resultSchema(parent)
      printSchema(schema)
      execOp(parent) { rec => printFields(rec.fields) }
  }
  def execQuery(q: Operator): Unit = execOp(q) { _ => }

/**
Data Structure Implementations
------------------------------
*/
  class TrieArray (dataSize: Int, schema: Schema, schemaOfResult: Schema) {
    var len = 0
    val buf = schema.map {
      case hd if isNumericCol(hd) => IntColBuffer(NewArray[Int](dataSize))
      case _ => StringColBuffer(NewArray[String](dataSize), NewArray[Int](dataSize))
    }
    val valueArray = schema.map {
      case hd if isNumericCol(hd) => IntColBuffer(NewArray[Int](dataSize))
      case _ => StringColBuffer(NewArray[String](dataSize), NewArray[Int](dataSize))
    }
    val indexArray = schema.map(f => NewArray[Int](dataSize))
    val lenArray = NewArray[Int](schema.length)
    //should be Vector[Boolean]?
    val flagTable = schemaOfResult.map(f => schema contains f)
    val levelTable = schema.map(f => schemaOfResult indexOf f)
    def hasCol(i: Int): Boolean = (i >= 0) && (i < schemaOfResult.length) && flagTable(i)
    def levelOf(i : Int): Int = levelTable indexOf i
    def getRowData(data:Vector[ColBuffer], i:Rep[Int]) = data.map {
      case IntColBuffer(b) => RInt(b(i))
      case StringColBuffer(b,l) => RString(b(i),l(i))
    }
    def update(data: Vector[ColBuffer], i: Rep[Int], x: Fields) = (data,x).zipped.foreach {
      case (IntColBuffer(b), RInt(x)) => b(i) = x
      case (StringColBuffer(b,l), RString(x,y)) => b(i) = x; l(i) = y
    }
    def update(data: Vector[ColBuffer], i: Rep[Int], j: Int, x: RField) = (data(j), x) match {
      case (RInt(x1), RInt(x2)) => x1 = x2
      case (RString(b1,l1), RString(b2,l2)) => b1 = b2; l1 = l2
    }
    def +=(x: Fields) = {
      update(buf,len,x)
      len += 1
    }
    def output: Rep[Unit] = {
      var a = 0
      while (a < len) {
        buf foreach {line =>
          line(a).print()
          print(" ")
        }
        print('\n')
        a += 1
      }
    }
    def toTrieArray: Rep[Unit] = {
      //generate indexArray
      val lastRecord = schema.map {
        case hd if isNumericCol(hd) => RInt(0)
        case _ => RString("",0)
      }
      val next = NewArray[Int](schema.length)
      var i = 0; var j = 0
      var diff = false
      while(i < len) {
        diff = false
        while (j < schema.length) {
          access[Unit](j, schema.length){j =>
            val curr_value = getRowData(buf,i)(j)
            if (diff || !(lastRecord(j) compare curr_value)) {
              update(valueArray, next(j), j, curr_value)
              if (j != schema.length - 1)
                  indexArray(j)(next(j)) = next(j+1)
              (lastRecord(j),curr_value) match {
                case (RInt(x1), RInt(x2)) => x1 = x2
                case (RString(b1,l1), RString(b2,l2)) => b1 = b2; l1 = l2
              }
              next(j) = next(j) + 1
              diff = true
            }
          }
          j += 1
        }
        i += 1
        j = 0
      }
      //for the last row
      while (j < schema.length - 1) {
        access[Unit](j, schema.length) { j =>
          indexArray(j)(next(j)) = next(j+1)
        }
        lenArray(j) = next(j)
        j += 1
      }
      lenArray(j) = next(j)
    }
    val cursor = NewArray[Int](schema.length)

    def key(level:Int): RField = {
      val lv:Int = levelOf(level)
      getRowData(valueArray, cursor(lv))(lv)
    }
    def open(level:Int): Rep[Unit] = {
      val lv:Int = levelOf(level)
      if (lv == 0) cursor(lv) = 0
      else cursor(lv) = indexArray(lv-1)(cursor(lv-1))
    }
    def next(level:Int): Rep[Unit] = {
      val lv:Int = levelOf(level)
      cursor(lv) = cursor(lv) + 1
    }
    def atEnd(level:Int): Rep[Boolean] = {
      val lv:Int = levelOf(level)
      if (lv == 0 && cursor(lv) == lenArray(lv)) true
      else if (lv != 0 && cursor(lv) == indexArray(lv - 1)(cursor(lv - 1) + 1)) true
      else false
    }
    def seek(level:Int, seekKey: Rep[String]): Rep[Unit] = {   //find the first of repetitions
      val lv:Int = levelOf(level)
      val start = cursor(lv)
      val end = if (lv == 0) lenArray(0) else indexArray(lv - 1)(cursor(lv - 1) + 1)
      //println("start = " + start + ", end = " + end)
      bsearch(lv, seekKey, start, end)
    }
    def bsearch(lv:Int, seekKey: Rep[String], start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
      //println("start = " + start + ", end = " + end)
      var vstart = start
      var vend = end
      while(vstart != vend) {
        //Shall we transform it into access[Unit](){func}? This introduce more code. 
        val pivot = getRowData(valueArray, (vstart + vend) / 2)(lv)
        if (pivot compare seekKey) {vstart = (vstart + vend) / 2; vend = vstart}
        else if (pivot lessThan seekKey) {vstart = (vstart + vend) / 2 + 1}
        else {vend = (vstart + vend) / 2}
      }
      cursor(lv) = vstart
    }
  }


  // defaults for hash sizes etc

  object hashDefaults {
    val hashSize   = (1 << 8)
    val keysSize   = hashSize
    val bucketSize = (1 << 8)
    val dataSize   = keysSize * bucketSize
  }

  // common base class to factor out commonalities of group and join hash tables

  class HashMapBase(keySchema: Schema, schema: Schema) {
    import hashDefaults._

    val keys = new ArrayBuffer(keysSize, keySchema)
    val keyCount = var_new(0)

    val hashMask = hashSize - 1
    val htable = NewArray[Int](hashSize)
    for (i <- 0 until hashSize) { htable(i) = -1 }

    def lookup(k: Fields) = lookupInternal(k,None)
    def lookupOrUpdate(k: Fields)(init: Rep[Int]=>Rep[Unit]) = lookupInternal(k,Some(init))
    def lookupInternal(k: Fields, init: Option[Rep[Int]=>Rep[Unit]]): Rep[Int] =
    comment[Int]("hash_lookup") {
      val h = fieldsHash(k).toInt
      var pos = h & hashMask
      while (htable(pos) != -1 && !fieldsEqual(keys(htable(pos)),k)) {
        pos = (pos + 1) & hashMask
      }
      if (init.isDefined) {
        if (htable(pos) == -1) {
          val keyPos = keyCount: Rep[Int] // force read
          keys(keyPos) = k
          keyCount += 1
          htable(pos) = keyPos
          init.get(keyPos)
          keyPos
        } else {
          htable(pos)
        }
      } else {
        htable(pos)
      }
    }
  }

  // hash table for groupBy, storing sums

  class HashMapAgg(keySchema: Schema, schema: Schema) extends HashMapBase(keySchema: Schema, schema: Schema) {
    import hashDefaults._

    val values = new ArrayBuffer(keysSize, schema) // assuming all summation fields are numeric

    def apply(k: Fields) = new {
      def +=(v: Fields) = {
        val keyPos = lookupOrUpdate(k) { keyPos =>
          values(keyPos) = schema.map(_ => RInt(0))
        }
        values(keyPos) = (values(keyPos) zip v) map { case (RInt(x), RInt(y)) => RInt(x + y) }
      }
    }

    def foreach(f: (Fields,Fields) => Rep[Unit]): Rep[Unit] = {
      for (i <- 0 until keyCount) {
        f(keys(i),values(i))
      }
    }

  }

  // hash table for joins, storing lists of records

  class HashMapBuffer(keySchema: Schema, schema: Schema) extends HashMapBase(keySchema: Schema, schema: Schema) {
    import hashDefaults._

    val data = new ArrayBuffer(dataSize, schema)
    val dataCount = var_new(0)

    val buckets = NewArray[Int](dataSize)
    val bucketCounts = NewArray[Int](keysSize)

    def apply(k: Fields) = new {
      def +=(v: Fields) = {
        val dataPos = dataCount: Rep[Int] // force read
        data(dataPos) = v
        dataCount += 1

        val bucket = lookupOrUpdate(k)(bucket => bucketCounts(bucket) = 0)
        val bucketPos = bucketCounts(bucket)
        buckets(bucket * bucketSize + bucketPos) = dataPos
        bucketCounts(bucket) = bucketPos + 1
      }

      def foreach(f: Record => Rep[Unit]): Rep[Unit] = {
        val bucket = lookup(k)

        if (bucket != -1) {
          val bucketLen = bucketCounts(bucket)
          val bucketStart = bucket * bucketSize

          for (i <- bucketStart until (bucketStart + bucketLen)) {
            f(Record(data(buckets(i)),schema))
          }
        }
      }
    }
  }

  // column-oriented array buffer, with a row-oriented interface,
  // specialized to data representation

  abstract class ColBuffer
  case class IntColBuffer(data: Rep[Array[Int]]) extends ColBuffer
  case class StringColBuffer(data: Rep[Array[String]], len: Rep[Array[Int]]) extends ColBuffer

  class ArrayBuffer(dataSize: Int, schema: Schema) {
    val buf = schema.map {
      case hd if isNumericCol(hd) => IntColBuffer(NewArray[Int](dataSize))
      case _ => StringColBuffer(NewArray[String](dataSize), NewArray[Int](dataSize))
    }
    var len = 0
    def +=(x: Fields) = {
      this(len) = x
      len += 1
    }
    def update(i: Rep[Int], x: Fields) = (buf,x).zipped.foreach {
      case (IntColBuffer(b), RInt(x)) => b(i) = x
      case (StringColBuffer(b,l), RString(x,y)) => b(i) = x; l(i) = y
    }
    def apply(i: Rep[Int]) = buf.map {
      case IntColBuffer(b) => RInt(b(i))
      case StringColBuffer(b,l) => RString(b(i),l(i))
    }
  }

  /**
      Trie-Join
      --------------------------
      */
  class LFTJmain (rels : List[TrieArray], schema: Schema){
    var currLv = 0
    var lstLv = 0
    var currCursor = 0  //change to Some[Rep[String]] after debug is done
    val resCursor = NewArray[Int](schema.length)
    def run(yld: Record => Rep[Unit]) = {
      while (currLv != -1) {
        lstLv = currLv
        currCursor = unlift(leapFrogJoin)(currLv, schema.length) //return -1 if not found
        if (currCursor != -1) {
          pushIntoRes(currCursor)
          if (lstLv == schema.length - 1) yld(makeRecord)
        }
      }
    }
    //Can we write leapFrogJoin in a reversal style?
    def leapFrogJoin(level: Int): Rep[Int] = {
      val resultCursor = search(level)
      /* need modification here. for each relation, the case is diff! */
      if (level == schema.length - 1) {
        if (atEnd(level)) {currLv -= 1; next(level-1)}
        else next(level)
      }
      else {
        if (atEnd(level)) {currLv -= 1; next(level-1)}
        else {currLv += 1; open(level+1)}
      }
      resultCursor
    }
    def unlift(f: Int => Rep[Int])(numUnLift: Rep[Int], upperBound: Int, lowerBound: Int = 0, count: Int = 0): Rep[Int] = {
      if (numUnLift == count)
        f(count)
      else if (lowerBound <= count && count < upperBound - 1)
        unlift(f)(numUnLift, upperBound, lowerBound, count + 1)
      else
        f(upperBound - 1)
    }
    //check atEnd after calling next()
    def next(level: Int): Rep[Unit] = {
      //call iterator.next for every iterator in vector
      rels.filter(r => r.hasCol(level)) foreach {r => r.next(level)}
    }
    def search(level: Int): Rep[Unit] = {
      var maxkey = schema(level) match {
        case hd if isNumericCol(hd) => RInt(0)
        case _ => RString("",0)
      }
      var minkey = maxkey
      while({
        var flag = atEnd(level)
        if (flag == true) {false} 
        else {
          val kArray = keys(level)
          minkey = kArray(0) match {
            case RInt(value) => RInt(value)
            case RString(b,l) => RString(b,l)
          }
          maxkey = minkey
          kArray foreach {k => 
            if (k lessThan minkey) 
              minkey = k match {
                case RInt(value) => RInt(value)
                case RString(b,l) => RString(b,l)
              }
            else 
              maxkey = k match {
                case RInt(value) => RInt(value)
                case RString(b,l) => RString(b,l)
              }
          }
        }
        !(maxkey compare minkey)
      }) {
        rels.filter(r => r.hasCol(level)) foreach {r => r.seek(level, maxkey)}
      }
      //println("LV: " + currLv + ", key = " + maxkey)
    }
    def key(level: Int) = keys(level)(0)
    def keys(level: Int) = {
      val array = rels.filter(r => r.hasCol(level)).map{r => r.key(level)}
      array
    }
    def atEnd(level: Int): Rep[Boolean] = rels.filter(r => r.hasCol(level)).foldLeft(unit(false))((a, x) => a || x.atEnd(level))
    def open(level:Int): Rep[Unit] = rels.filter(r => r.hasCol(level)).foreach(r => r.open(level))
    def makeRecord: Record = {
      //Record(res, schemaOfResult)
      var i = -1;
      val key = schema.map{x => {i += 1; res(i)}}
      Record(key, schema)
    }
    def pushIntoRes(key: Rep[String]) = {
      res.update(lstLv, key)
    }
  }
}}
