/**
Query Compiler III (C Code)
===========================

Outline:
<div id="tableofcontents"></div>

*/
package scala.lms.tutorial

import scala.lms.common._

object query_optc {
trait QueryCompiler extends Dsl with StagedQueryProcessor
with ScannerLowerBase {
  override def version = "query_optc"
  val queryNumber = "Q5"
  val sf = 1
  val tableSize = sf match {
    case 1 => Vector(10,30,10001,150001,200001,800001,1500001,6001216)
    case 10 => Vector(10,30,100001,1500001,2000001,8000001,15000001,59986053)
  }
  val tablesInQuery = queryNumber match {
    case "Q5" => Vector("NATION","REGION","SUPP","CUSTOMER","ORDERS","LINEITEM")
    case "Q9" => Vector("NATION","SUPP","ORDERS","LINEITEM","PART","PARTSUPP")
  }
  val tpchTables = Vector("REGION","NATION","SUPP","CUSTOMER","PART","PARTSUPP","ORDERS","LINEITEM")

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
      //Special case for tpch dbgen data. Last column contains '|' and '\n'
      pos += 1
      RString(stringFromCharArray(data,start,len), len)
    }

    def nextInt(d: Rep[Char]) = {
      val start = pos: Rep[Int] // force read
      var num = 0
      while (data(pos) != d) {
        //If date format, transfer it to Int
        if (data(pos) != '-')
          num = num * 10 + (data(pos) - '0').toInt
        pos += 1
      }
      pos += 1
      RInt(num)
    }

    def hasNext = pos < fl
    def done = close(fd)
  }

/**
Low-Level Processing Logic
--------------------------
*/
  abstract class RField {
    def print()
    def compare(o: RField): Rep[Boolean]
    def hash: Rep[Long]
    def lessThan(o :RField): Rep[Boolean]
    //def update(o: RField)
    def -(o:RField): Rep[Int]
    def contains(o:RField): Rep[Boolean]
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
    def lessThan(o: RField) = o match { case RString(data2, len2) => 
      var i = 0
      while (i < len && i < len2 && data.charAt(i) == data2.charAt(i)) i += 1
      if (i == len2) false
      else if(i == len) true
      else data.charAt(i) < data2.charAt(i)
    }
    def hash = data.HashCode(len)
    //def update(o: RField) = o match { case RString(data2, len2) => data = data2; len = len2}
    def -(o:RField) = o match { case RString(data2, len2) =>
      var res = 0
      var i = 0
      while (i < len && i < len2) {
        res *= 26
        res = res + (data.charAt(i).toInt - data2.charAt(i).toInt)
        i += 1
      }
      res
    }
    def contains(o:RField) = o match { case RString(data2, len2) =>
      //
      var nmatch = 0
      var i = 0
      //need fix

      while (nmatch != len2 && i < len-len2) {
        while (data.charAt(i+nmatch) == data2.charAt(nmatch)) nmatch += 1
        nmatch = if (nmatch == len2) nmatch else 0
        i += 1
      }
      nmatch == len2
    }
  }
  //make value visilble outside RInt.
  case class RInt(val value: Rep[Int]) extends RField {
    def print() = printf("%d",value)
    def compare(o: RField) = o match { case RInt(v2) => value == v2 }
    def lessThan(o: RField) = o match { case RInt(v2) =>  value < v2 }
    def hash = value.asInstanceOf[Rep[Long]]
    //def update(o: RField) = o match { case RInt(v2) => value = v2}
    def -(o:RField) = o match { case RInt(v2) => value - v2}
    def contains(o:RField) = o match { case RInt(v2) => //no contains support for Int
      v2 <= value
    }
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
    s.done
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
    case LT(a1, a2) => evalRef(a1)(rec) lessThan evalRef(a2)(rec)
    case GTE(a1, a2) => !(evalRef(a1)(rec) lessThan evalRef(a2)(rec))
    case CON(a1, a2) => evalRef(a1)(rec) contains evalRef(a2)(rec)
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
    case LFTJoin(parents)        =>/*
      val schema = queryNumber match {
        case "Q5" => Schema(
          "#REGIONKEY",
          "#NATIONKEY",
          "N_NAME",
          "#CUSTKEY",
          "#ORDERKEY",
          "#SUPPKEY",
          "#PARTKEY")
        case "Q9" => Schema(
          "#NATIONKEY",
          "N_NAME",
          "#SUPPKEY",
          "#PARTKEY",
          "#ORDERKEY"
          )
      }*/
      val schema = Schema("#X","#Y","#Z")
      schema
    case Symbol(_,_)              => Schema() //dummy
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
        if (agg(0) == "#COUNT") hm(rec(keys)) += Vector[RField](RInt(1))
        else hm(rec(keys)) += rec(agg)
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
      val dataSize = Vector(200000)//tablesInQuery.map(t => tableSize(tpchTables indexOf t)) 
      val schemaOfResult = resultSchema(LFTJoin(parents))
      //Measure data loading and preprocessing time
      unchecked[Unit]("clock_t begin, end; double time_spent")
      unchecked[Unit]("begin = clock()")
      //1. create trieArrays for those real relations
      //2. create iterators
      val real = parents.filter(p => p match {
        case Symbol(_,_) => false
        case _ => true
        })
      val trieArrays = (real,dataSize).zipped.map { (p,size) =>
        val buf = new TrieArray(size, resultSchema(p), schemaOfResult)
        execOp(p) {rec => buf += rec.fields} //fields is of type Fields: Vector[RField]
        buf
      }
      val tries = trieArrays.iterator
      val trieArrayIterators = parents.map(p => p match {
        case Symbol(sym, schema) => new TrieArrayIterator(trieArrays(sym), schema)
        case _ => val trie = tries.next(); new TrieArrayIterator(trie, trie.schema)
      }
      )
      unchecked[Unit]("end = clock(); printf(\"Data loading: %f\\n\", (double)(end - begin) / CLOCKS_PER_SEC)")
      //Measure trie building time
      val join = new LFTJmain(trieArrayIterators, schemaOfResult)
      var i = 0
      while(i < 1) {
        unchecked[Unit]("begin = clock()")
        join.run(yld)
        unchecked[Unit]("end = clock(); printf(\"Join: %f\\n\", (double)(end - begin) / CLOCKS_PER_SEC)")
        i += 1
      }
      unchecked[Unit]("printf(\"Expectation triangles: 1612010 * 6 = 9672060\\n\")")
    case PrintCSV(parent) =>
      val schema = resultSchema(parent)
      printSchema(schema)
      execOp(parent) { rec => printFields(rec.fields) }
    case Symbol(_,_) => unit()
  }
  def execQuery(q: Operator): Unit = execOp(q) { _ => }

/**
Data Structure Implementations
------------------------------
*/
  def access(i: Rep[Int], len: Int)(f: Int => Rep[Unit]): Rep[Unit] = {
    if(i < 0) unit()
    else accessHelp(i, len, 0)(f)
  }
  def accessHelp(i: Rep[Int], len: Int, count: Int)(f: Int => Rep[Unit]): Rep[Unit] = {
    if (i == count)
      f(count)
    else if (count < len - 1)
      accessHelp(i, len, count + 1)(f)
    else
      unit()
  }
  class TrieArrayIterator(ta: TrieArray, schema: Schema) {
    val schemaOfResult = ta.schemaOfResult
    val valueArray = ta.valueArray
    val indexArray = ta.indexArray
    val lenArray = ta.lenArray
    val cursor = schema.map{f => var_new[Int](0)}
    val flagTable = schemaOfResult.map(f => schema contains f)
    val levelTable = schema.map(f => schemaOfResult indexOf f)
    def hasCol(i: Int): Boolean = (i >= 0) && (i < schemaOfResult.length) && flagTable(i)
    def levelOf(i : Int): Int = levelTable indexOf i
    def key(level:Int): RField = {
      val lv:Int = levelOf(level)
      valueArray(cursor(lv),lv)
    }
    def open(level:Int): Rep[Unit] = {
      val lv:Int = levelOf(level)
      if (lv == 0) var_assign(cursor(lv),0)
      else var_assign(cursor(lv),indexArray(lv-1)(cursor(lv-1)))
    }
    def next(level:Int): Rep[Unit] = {
      val lv:Int = levelOf(level)
      cursor(lv) += 1
    }
    def atEnd(level:Int): Rep[Boolean] = {
      val lv:Int = levelOf(level)
      if (lv == 0 && cursor(lv) == lenArray(lv)) true
      else if (lv != 0 && cursor(lv) == indexArray(lv-1)(cursor(lv-1)+1)) true
      else false
    }
    def seek(level:Int,seekKey: RField): Rep[Unit] = {
      val lv:Int = levelOf(level)
      val start = readVar(cursor(lv))
      if (!(valueArray(cursor(lv),lv) compare seekKey)) {
        val end:Rep[Int] = if (lv == 0) lenArray(0) else indexArray(lv-1)(cursor(lv-1)+1)
        interpolation_search(lv,seekKey,start,end)
      }
    }
    //not good for encoded
    def expSearch(lv:Int,seekKey:RField,start:Rep[Int],end:Rep[Int]):Rep[Unit] = {
      val size = end-start
      if (size == 1) {
        if (valueArray(start,lv) lessThan seekKey) var_assign(cursor(lv),end)
        else var_assign(cursor(lv),start)
      } else {
        var bound = 1
        var flag = false
        while(bound < size && (valueArray(start+bound,lv) lessThan seekKey)) {
          if (valueArray(start+bound,lv) compare seekKey) {var_assign(cursor(lv),start+bound);bound = size;flag = true}
          else bound*=2
        }
        if(!flag) {
          val min = if(start+bound+1 < end) start+bound+1 else end        
          bsearch(lv,seekKey,start+bound/2,min)
        }
      }
    }
    def interpolation_search(lv:Int, seekKey: RField, start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
      var vstart = start
      var vend = end
      while(vstart != vend) {
        //if less than 5 elements, do linear search instead of b-search
        /*if (vend - vstart < 5) {vstart = lsearch(lv,seekKey,vstart,vend); vend = vstart}
        else {*/
          var diff = seekKey - valueArray(vstart,lv)
          var range = valueArray(vend-1,lv) - valueArray(vstart,lv)
          if (diff <=0 ) vend = vstart
          else if (diff > range) vstart = vend
          else {
            var mid = vstart + (1.0 * diff * (vend - vstart - 1) 
            / range).toInt
            val pivot = valueArray(mid,lv)
            if (pivot compare seekKey) {vstart = mid; vend = vstart}
            else if (pivot lessThan seekKey) {vstart = mid + 1}
            else {vend = mid}
          }
        //}
      }
      var_assign(cursor(lv),vstart)
    }
    def lsearch(lv:Int, seekKey: RField, start: Rep[Int], end: Rep[Int]): Rep[Int] = {
      var vstart = start
      var vend = end
      while(vstart != vend) {
        if (valueArray(vstart,lv) lessThan seekKey) vstart += 1
        else vend = vstart
      }
      vstart
    }
    def bsearch(lv:Int, seekKey: RField, start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
      //if the current value is the seekKey it means we don't need actually do search.
      //there're many search strategies for searching which are data dependently. 
      //we want to have some optimal strategies for general data.
      var vstart = start
      var vend = end
      while(vstart != vend) {
        //if less than 5 elements, do linear search instead of b-search
        if (vend - vstart < 5) {vstart = lsearch(lv,seekKey,vstart,vend); vend = vstart}
        else {
          val pivot = valueArray((vstart + vend) / 2,lv)
          if (pivot compare seekKey) {vstart = (vstart + vend) / 2; vend = vstart}
          else if (pivot lessThan seekKey) {vstart = (vstart + vend) / 2 + 1}
          else {vend = (vstart + vend) / 2}
        }
      }
      var_assign(cursor(lv),vstart)
    }
    def resetCursor = {
      cursor.foreach{c => 
        var_assign(c, 0)
      }
    }
  }

  class TrieArray (dataSize: Int, val schema: Schema, val schemaOfResult: Schema) {
    val valueArray = new ArrayBuffer(dataSize, schema)
    val indexArray = schema.map(f => NewArray[Int](dataSize))
    val lenArray = schema.map{f => var_new[Int](0)}
    def +=(x: Fields):Rep[Unit] = {
      var diff = false
      x.foreach { xx =>
        val i = x indexOf xx
        if (lenArray(i) == 0) diff = true
        else if (!diff) diff = !(valueArray((lenArray(i)-1),i) compare xx)
        if (diff) {
          valueArray.update(lenArray(i),i,xx)
          if (i != schema.length - 1) indexArray(i)(lenArray(i)) = lenArray(i+1)          
          lenArray(i)+=1
        }
      }
    }
    def output:Rep[Unit] = {
      schema.foreach{ a =>
        val i = schema indexOf a
        var j = 0
        while (j < lenArray(i)) {valueArray(j,i).print; print(" "); j+=1}
        println("")
      }
    }
  }

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
    def update(i: Rep[Int], j: Int, x: RField) = (buf(j),x) match {
      case (IntColBuffer(b), RInt(x)) => b(i) = x
      case (StringColBuffer(b,l), RString(x,y)) => b(i) = x; l(i) = y
    }
    def apply(i: Rep[Int]) = buf.map {
      case IntColBuffer(b) => RInt(b(i))
      case StringColBuffer(b,l) => RString(b(i),l(i))
    }
    def apply(i: Rep[Int], j: Int) = buf(j) match {
      case IntColBuffer(b) => RInt(b(i))
      case StringColBuffer(b,l) => RString(b(i),l(i))
    }
  }

  /**
      Trie-Join
      --------------------------
      */
  class LFTJmain (rels : List[TrieArrayIterator], schema: Schema){
    //ArrayBuffer(1,schema) is inefficient
    val maxkeys = new ArrayBuffer(1, schema)
    val minkeys = new ArrayBuffer(1, schema)
    val res = new ArrayBuffer(1, schema)
    var currLv = 0
    def run(yld: Record => Rep[Unit]) = {
      while (currLv != -1) {
        unlift(leapFrogJoin)(yld, currLv, schema.length) 
      }
      //reset all variables
      currLv = 0
      rels foreach {r => r.resetCursor}
    }
    def leapFrogJoin(level: Int, yld: Record => Rep[Unit]): Rep[Unit] = {
      if (rels.filter(r => r.hasCol(level)).length > 1) search(level)
      if (level == schema.length - 1) {
        if (atEnd(level)) {currLv -= 1; next(level-1)}
        else {pushIntoRes(level, key(level)); next(level); yld(makeRecord)}
      }
      else {
        if (atEnd(level)) {currLv -= 1; next(level-1)}
        else {pushIntoRes(level, key(level)); currLv += 1; open(level+1)}
      }
    }
    def unlift(f: (Int, Record => Rep[Unit]) => Rep[Unit])(yld: Record => Rep[Unit], numUnLift: Rep[Int], upperBound: Int, lowerBound: Int = 0, count: Int = 0): Rep[Unit] = {
      if (numUnLift == count)
        f(count,yld)
      else if (lowerBound <= count && count < upperBound - 1)
        unlift(f)(yld, numUnLift, upperBound, lowerBound, count + 1)
      else unit() //exit
    }
    //check atEnd after calling next()
    def next(level: Int): Rep[Unit] = {
      //call iterator.next for every iterator in vector
      rels.filter(r => r.hasCol(level)) foreach {r => r.next(level)}
    }
    def search(level: Int): Rep[Unit] = {
      val ops = rels.filter(r => r.hasCol(level))
      var keyFound = false
      maxkeys.update(0, level, key(level))
      keys(level).foreach {k => 
        if(maxkeys(0,level) lessThan k)              
          maxkeys.update(0,level,k)    
      }
      while (!keyFound && !atEnd(level)) {
        val kArray = keys(level)
        minkeys.update(0, level, kArray(0))
        kArray foreach { k =>
          if (k lessThan minkeys(0,level))
            minkeys.update(0,level,k)    
        }
        keyFound = maxkeys(0,level) compare minkeys(0,level)
        if (!keyFound) ops.foreach { r => 
          r.seek(level, maxkeys(0,level))
          if (!r.atEnd(level)) maxkeys.update(0,level,r.key(level))
        }
      }
    }
    def key(level: Int) = keys(level)(0)
    def keys(level: Int) = {
      val array = rels.filter(r => r.hasCol(level)).map{r => r.key(level)}
      array
    }
    def atEnd(level: Int): Rep[Boolean] = //rels.filter(r => r.hasCol(level)).foldLeft(unit(false))((a, x) => a || x.atEnd(level))
    {
      val ops = rels.filter(r => r.hasCol(level))
      val length = ops.length
      def F(i:Int):Rep[Boolean] = {
        if (i < length-1) {
          if (ops(i).atEnd(level)) true
          else F(i+1)
        }
        else ops(i).atEnd(level)
      }
      F(0)
    }
    def open(level:Int): Rep[Unit] = rels.filter(r => r.hasCol(level)).foreach(r => r.open(level))
    def makeRecord: Record = {
      Record(res(0), schema)
    }
    def pushIntoRes(level: Int, key: RField) = {
      res.update(0,level,key)
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

}}