/**
  Query Compiler II (Scala)
  ========================

  Outline:
  <div id="tableofcontents"></div>

  */
package scala.lms.tutorial

import scala.virtualization.lms.common._

object query_staged {
  trait QueryCompiler extends Dsl with StagedQueryProcessor
      with ScannerBase {
    override def version = "query_staged"

    //method to access element in Vector using Rep[Int] as subscript
    def access[T:Manifest](i: Rep[Int], len: Int)(f: Int => Rep[T]): Rep[T] = {
      if(i >= len)
        f(0)
      else
        accessHelp[T](i, len, 0)(f)
    }
    def accessHelp[T:Manifest](i: Rep[Int], len: Int, count: Int)(f: Int => Rep[T]): Rep[T] = {
      if (i == count)
        f(count)
      else if (count < len - 1)
        accessHelp[T](i, len, count + 1)(f)
      else
        f(len-1)
    }
    //the if-then-else branch in get/update function can be eliminated.
    def getValue(buf: Vector[Rep[Array[String]]], i: Rep[Int], j: Rep[Int]): Rep[String] = access[String](i, buf.length){i =>
      if (i < buf.length)
        buf(i)(j)
      else
        unit("")
    }
    def getIndex(buf: Vector[Rep[Array[Int]]], i: Rep[Int], j: Rep[Int]): Rep[Int] = access[Int](i, buf.length){i =>
      if (i < buf.length)
        buf(i)(j)
      else
        unit(0)
    }
    def updateValue(buf: Vector[Rep[Array[String]]], i: Rep[Int], j: Rep[Int], v: Rep[String]): Rep[Unit] = access[Unit](i, buf.length){i =>
      if (i < buf.length)
        buf(i)(j) = v
    }
    def updateIndex(buf: Vector[Rep[Array[Int]]], i: Rep[Int], j: Rep[Int], v: Rep[Int]): Rep[Unit] = access[Unit](i, buf.length){i =>
      if (i < buf.length)
        buf(i)(j) = v
    }
    /**
      Low-Level Processing Logic
      --------------------------
      */
    type Fields = Vector[Rep[String]]

    case class Record(fields: Fields, schema: Schema) {
      def apply(key: String): Rep[String] = fields(schema indexOf key)
      def apply(keys: Schema): Fields = keys.map(this apply _)
    }

    def processCSV(filename: Rep[String], schema: Schema, fieldDelimiter: Char, externalSchema: Boolean)(yld: Record => Rep[Unit]): Rep[Unit] = {
      val s = newScanner(filename)
      val last = schema.last
      def nextRecord = Record(schema.map{x => s.next(if (x==last) '\n' else fieldDelimiter)}, schema)
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

    def printFields(fields: Fields) = printf(fields.map{_ => "%s"}.mkString("", defaultFieldDelimiter.toString, "\n"), fields: _*)

    /*
     Note: this two comparison func doesn't work for empty Fields object
     */
    def fieldsEqual(a: Fields, b: Fields) = (a zip b).foldLeft(unit(true)) { (a,b) => a && b._1 == b._2 }

    def fieldsLessThan(a: Fields, b: Fields) = (a zip b).foldLeft(unit(false)) { (a,b) => a || b._1 < b._2}

    def fieldsHash(a: Fields) = a.foldLeft(unit(0L)) { _ * 41L + _.HashCode }

    /**
      Query Interpretation = Compilation
      ----------------------------------
      */
    def evalPred(p: Predicate)(rec: Record): Rep[Boolean] = p match {
      case Eq(a1, a2) => evalRef(a1)(rec) == evalRef(a2)(rec)
    }
    def evalRef(r: Ref)(rec: Record): Rep[String] = r match {
      case Field(name) => rec(name)
      case Value(x) => x.toString
    }
    def resultSchema(o: Operator): Schema = o match {
      case Scan(_, schema, _, _)   => schema
      case Filter(pred, parent)    => resultSchema(parent)
      case Project(schema, _, _)   => schema
      case Join(left, right)       => resultSchema(left) ++ resultSchema(right)
      case Group(keys, agg, parent)=> keys ++ agg
      case HashJoin(left, right)   => resultSchema(left) ++ resultSchema(right)
      case PrintCSV(parent)        => Schema()
      //Only for test
      case LFTJoin(parents)        =>
        //This is incorrect because it only guarantees the order or attributes within each table,
        //but not among tables.
        /*
        val schema = scala.collection.mutable.ArrayBuffer[String]()
        parents foreach {p => schema ++= resultSchema(p)}
        schema.toVector.distinct    //no repeated attributes
        */ 
        val schema = Schema("ORDERKEY","CUSTKEY","PARTKEY","SUPPKEY","NATIONKEY","REGIONKEY")
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
        //build TrieArrays
        val dataSize = Vector(25+1,5+1,10000+1,150000+1,1500000+1,6001215+1)
        val schemaOfResult = resultSchema(LFTJoin(parents))
        val trieArrays = (parents,dataSize).zipped.map { (p,size) =>
          val buf = new TrieArray(size, resultSchema(p), schemaOfResult)
          execOp(p) {rec => buf += rec.fields}
          buf
        }
        //        trieArrays foreach {arr => arr.output}
        trieArrays foreach {arr => arr.toTrieArray}
        val join = new LFTJmain(trieArrays, schemaOfResult)
        join.run(yld)
        
      case PrintCSV(parent) =>
        val schema = resultSchema(parent)
        printSchema(schema)
        execOp(parent) { rec => printFields(rec.fields)}
    }
    def execQuery(q: Operator): Unit = execOp(q) { _ => }

    /**
      Data Structure Implementations
      ------------------------------
      */
    class TrieArray (dataSize: Int, schema: Schema, schemaOfResult: Schema) {
      var len = 0
      val buf = schema.map(f => NewArray[String](dataSize))
      val valueArray = schema.map(f => NewArray[String](dataSize))
      val indexArray = schema.map(f => NewArray[Int](dataSize))
      val lenArray = NewArray[Int](schema.length)
      //should be Vector[Boolean]?
      val flagTable = NewArray[Boolean](schemaOfResult.length)
      flagTableCons
      def flagTableCons = {
        var i = 0
        schemaOfResult foreach { attr =>
          flagTable(i) = schema.exists(x => x == attr)
          i += 1
        }
      }
      def hasCol(i: Rep[Int]) : Rep[Boolean] = flagTable(i)

      def apply(i: Rep[Int]) = {
        buf.map(b => b(i))
      }
      def +=(x: Seq[Rep[String]]) = {
        this(len) = x
        len += 1
      }
      def update(i: Rep[Int], x: Seq[Rep[String]]) = {
        (buf,x).zipped.foreach((b,x) => b(i) = x)
      }

      def output: Rep[Unit] = {
        var a = 0
        while (a < len) {
          buf foreach {line =>
            print(line(a) + " ")
          }
          print('\n')
          a += 1
        }
      }

      def toTrieArray: Rep[Unit] = {
        //generate indexArray
        val lastRecord = NewArray[String](schema.length)
        lastRecord.foreach(x => x = "")
        val next = NewArray[Int](schema.length)
        var i = 0; var j = 0
        var diff = false
        while(i < len) {
          diff = false
          //here. could refactor the code.
          while (j < schema.length) {
            access[Unit](j, schema.length){j =>
              val curr_value = buf(j)(i)
              if (diff || lastRecord(j) != curr_value) {
                valueArray(j)(next(j)) = curr_value
                if (j != schema.length - 1)
                  indexArray(j)(next(j)) = next(j + 1)
                lastRecord(j) = curr_value
                next(j) = next(j) + 1
                diff = true
              }
            }
            /*
            val curr_value = getValue(buf, j, i)
            if (diff || lastRecord(j) != curr_value) {
              updateValue(valueArray, j, next(j), curr_value)
              if (j != schema.length - 1)   //no index for last attribute
                updateIndex(indexArray, j, next(j), next(j + 1))
              lastRecord(j) = curr_value
              next(j) = next(j) + 1
              diff = true
            }
            */
            j += 1
          }
          //          print('\n')
          i += 1
          j = 0
        }
        //for the last row
        while (j < schema.length - 1){
          /*
            How to return an array which can be modified?
          */
          //val index = getArray[Int](indexArray, j)
          //index(next(j)) = next(j + 1)
          updateIndex(indexArray, j, next(j), next(j + 1))
          lenArray(j) = next(j)
          j += 1
        }
        lenArray(j) = next(j)
        //j = 0
        //println("After initialization: j = " + j)
        //while (j < schema.length){println(lenArray(j)); j = j + 1; println("In loop: " + j)}
        //done generating TrieArray
      }
      val cursor = NewArray[Int](schema.length)
      var currLv = -1

      def key: Rep[String] = {getValue(valueArray, currLv, cursor(currLv))}
      def next: Rep[Unit] ={cursor(currLv) = cursor(currLv) + 1}

      def atEnd: Rep[Boolean] = {
        if (currLv == 0 && cursor(currLv) == lenArray(currLv)) true
        else if (currLv != 0 && cursor(currLv) == getIndex(indexArray, currLv - 1, cursor(currLv - 1) + 1)) true
        else false
      }
      def seek(seekKey: Rep[String]): Rep[Unit] = {   //find the first of repetitions
        //println("searchKey = " + seekKey)
        val start = cursor(currLv)
        val end = if (currLv == 0) lenArray(0) else getIndex(indexArray, currLv - 1, cursor(currLv - 1) + 1)
        //println("start = " + start + ", end = " + end)
        bsearch(seekKey, start, end)
      }
      def open: Rep[Unit] = {
        currLv += 1
        cursor(currLv) = if(currLv == 0) 0 else getIndex(indexArray, currLv - 1, cursor(currLv - 1))
      }
      def up: Rep[Unit] = currLv -= 1

      def bsearch(seekKey: Rep[String], start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
        //println("start = " + start + ", end = " + end)
        var vstart = start
        var vend = end
        while(vstart != vend) {
          //Shall we transform it into access[Unit](){func}? This introduce more code. 
          val pivot = getValue(valueArray, currLv, (vstart + vend) / 2)
          if (pivot == seekKey) {vstart = (vstart + vend) / 2; vend = vstart}
          else if (pivot < seekKey) {vstart = (vstart + vend) / 2 + 1}
          else {vend = (vstart + vend) / 2}
        }
        cursor(currLv) = vstart
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
      
      val keys = new ArrayBuffer[String](keysSize, keySchema)
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
              val keyPos = keyCount: Rep[Int] // force read. But seems that it doesn't affect code.
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

      val values = new ArrayBuffer[Int](keysSize, schema) // assuming all summation fields are numeric

      def apply(k: Fields) = new {
        def +=(v: Fields) = {
          val keyPos = lookupOrUpdate(k) { keyPos =>
            values(keyPos) = schema.map(_ => 0:Rep[Int])
          }
          values(keyPos) = (values(keyPos), v.map(_.toInt)).zipped map (_ + _)
        }
      }

      def foreach(f: (Fields,Fields) => Rep[Unit]): Rep[Unit] = {
        for (i <- 0 until keyCount) {
          f(keys(i),values(i).map(_.ToString))
        }
      }

    }

    // hash table for joins, storing lists of records

    class HashMapBuffer(keySchema: Schema, schema: Schema) extends HashMapBase(keySchema: Schema, schema: Schema) {
      import hashDefaults._

      val data = new ArrayBuffer[String](dataSize, schema)
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

    class ArrayBuffer[T:Manifest](dataSize: Int, schema: Schema) {
      val buf = schema.map(f => NewArray[T](dataSize))
      var len = 0

      def +=(x: Seq[Rep[T]]) = {
        this(len) = x
        len += 1
      }
      def update(i: Rep[Int], x: Seq[Rep[T]]) = {
        (buf,x).zipped.foreach((b,x) => b(i) = x)
      }
      def apply(i: Rep[Int]) = {
        buf.map(b => b(i))
      }
    }
    /**
      Trie-Join
      --------------------------
      */

    class LFTJmain (rels : List[TrieArray], schema: Schema){
      var currLv = -1
      //var p = 0
      val res = NewArray[String](schema.length)
      var k = 0  //res
      def run(yld: Record => Rep[Unit]) = {
        open
        search
        while (currLv != -1) {
          if (atEnd) {//2000lines in this branch
            //println("up")
            //rels foreach {r => if(r.hasCol(currLv)) println("currlv = " + r.currLv + ", key = " + r.key + ", atEnd = " + r.atEnd)}
            k -= 1
            up
            if (currLv != -1) next
          }
          else if (currLv != schema.length - 1) {
            //println("open: (currLv,key) = ("+currLv+","+key+")")
            pushIntoRes(key)
            open
          }
          else {
            //println("find 1 record: key = " + key)
            /* don't forget to call NEXT */
            pushIntoRes(key)
            yld(makeRecord)
            k -= 1
            next
          }
          if (currLv != -1 && !atEnd) search
        }
      }
      //check atEnd after calling next()
      def next: Rep[Unit] = {
        //call iterator.next for every iterator in vector
        rels foreach {r => if(r.hasCol(currLv)) r.next}
      }
      def search: Rep[Unit] = {
        //find the max key
        var maxkey = ""
        var minkey = ""
        while({
          var flag = atEnd
          if (flag == true) {false} 
          else {
            maxkey = ""
            var tmp = ""
            rels foreach {r =>
              if (r.hasCol(currLv)) {
                tmp = r.key
                if (tmp > maxkey) maxkey = tmp
              }
            }
            minkey = maxkey
            rels foreach {r =>
              if (r.hasCol(currLv)) {
                tmp = r.key
                if (tmp < minkey) minkey = tmp
              }
            }
            maxkey != minkey
          }
        }) {
          //println("minkey = " + minkey)
          //println("maxkey = " + maxkey)
          rels foreach {r => if(r.hasCol(currLv)) r.seek(maxkey)}
        }
        //println("LV: " + currLv + ", key = " + maxkey)
      }
      /*
      def init: Rep[Unit] = {
        search
      }*/
      def key: Rep[String] = {
        var key = ""
        rels foreach {r =>
          if (r.hasCol(currLv)) {
            key = r.key
          }
        }
        key
      }
      def atEnd(): Rep[Boolean] = rels.foldLeft(unit(false))((a, x) => a || (x.hasCol(currLv) && x.atEnd))
      def open: Rep[Unit] = {
        currLv += 1
        rels foreach {r => if(r.hasCol(currLv)) r.open}
      }
      def up: Rep[Unit] = {
        rels foreach {r => if(r.hasCol(currLv)) r.up}        
        currLv -= 1
      }
      def makeRecord: Record = {
        //Record(res, schemaOfResult)
        var i = -1;
        val key = schema.map{x => {i += 1; res(i)}}
        Record(key, schema)
      }
      def pushIntoRes(key: Rep[String]) = {
        res.update(k, key)
        k += 1
      }
    }
  }
}
