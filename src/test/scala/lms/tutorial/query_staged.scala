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
    def access[T:Manifest](i: Rep[Int])(f: Int => Rep[T]): Rep[T] = {
      if (i == 0) f(0)
      else if (i == 1) f(1)
      else if (i == 2) f(2)
      else if (i == 3) f(3)
      else if (i == 4) f(4)
      else f(0)
    }
    def getValue(buf: Vector[Rep[Array[String]]], i: Rep[Int], j: Rep[Int]): Rep[String] = access[String](i){i =>
      if (i < buf.length)
        buf(i)(j)
      else
        unit("")
    }
    def getIndex(buf: Vector[Rep[Array[Int]]], i: Rep[Int], j: Rep[Int]): Rep[Int] = access[Int](i){i =>
      if (i < buf.length)
        buf(i)(j)
      else
        unit(0)
    }
    def updateValue(buf: Vector[Rep[Array[String]]], i: Rep[Int], j: Rep[Int], v: Rep[String]): Rep[Unit] = access[Unit](i){i =>
      if (i < buf.length)
        buf(i)(j) = v
    }
    def updateIndex(buf: Vector[Rep[Array[Int]]], i: Rep[Int], j: Rep[Int], v: Rep[Int]): Rep[Unit] = access[Unit](i){i =>
      if (i < buf.length)
        buf(i)(j) = v
    }
    
    def callHasCol(i: Int, j:Rep[Int], vec: List[TrieArray]): Rep[Boolean] = {
      if (i < vec.length) {
        return vec(i).hasCol(j)
      }
      else
        return unit(false)
    }

    def callKey(i: Int, vec: List[TrieArray]): Rep[String] = {
      if (i < vec.length) {
        return vec(i).key
      }
      else
        return unit("")
    }

    def callNext(i: Int, vec: List[TrieArray]): Rep[Unit] = {
      if (i < vec.length) {
        return vec(i).next
      }
    }

    def callAtEnd(i: Int, vec: List[TrieArray]): Rep[Boolean] = {
      if (i < vec.length) {
        return vec(i).atEnd
      }
      else
        return true
    }

    def callOpen(i: Int, vec: List[TrieArray]): Rep[Unit] = {
      if (i < vec.length) {
        vec(i).open
      }
    }

    def callUp(i: Int, vec: List[TrieArray]): Rep[Unit] = {
      if (i < vec.length) {
        vec(i).up
      }
    }

    def callSeek(i: Int, x: Rep[String], vec: List[TrieArray]): Rep[Unit] = {
      if (i < vec.length) {
        vec(i).seek(x)
      }
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
        val schema = scala.collection.mutable.ArrayBuffer[String]()
        parents foreach {p => schema ++= resultSchema(p)}
        schema.toVector.distinct    //no repeated attributes
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

        val schemaOfResult = resultSchema(LFTJoin(parents))
        val trieArrays = parents.map { p =>
          val buf = new TrieArray(1 << 16, resultSchema(p), schemaOfResult)
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
          while (j < schema.length) {
            if (diff || lastRecord(j) != getValue(buf, j, i)) {
              updateValue(valueArray, j, next(j), getValue(buf, j, i))
              if (j != schema.length - 1)   //no index for last attribute
                updateIndex(indexArray, j, next(j), next(j + 1))
              lastRecord(j) = getValue(buf, j, i)
              next(j) = next(j) + 1
              diff = true
            }
            /*print(getValue(valueArray, j, next(j) - 1) + " "); print(getIndex(indexArray, j, next(j) - 1) + " ")*/
            j += 1
          }
//          print('\n')
          i += 1
          j = 0
        }
        //for the last row
        while (j < schema.length - 1){
          updateIndex(indexArray, j, next(j), next(j + 1))
          lenArray(j) = next(j)
          j += 1
        }
//        flagTable foreach {t => print(t + " ")}; print('\n')
            //done generating TrieArray
      }
      val cursor = NewArray[Int](schema.length)
      var currLv = 0

      def key: Rep[String] = {getValue(valueArray, currLv, cursor(currLv))}
      def next: Rep[Unit] ={cursor(currLv) = cursor(currLv) + 1}

      def atEnd: Rep[Boolean] = {
        if (currLv == 0 && cursor(currLv) == lenArray(currLv)) true
        else if (currLv != 0 && cursor(currLv) == getIndex(indexArray, currLv - 1, cursor(currLv - 1) + 1)) true
        else false
      }
      def seek(seekKey: Rep[String]): Rep[Unit] = {   //find the first of repetitions 
        val start = if (currLv == 0) 0 else getIndex(indexArray, currLv - 1, cursor(currLv - 1))
        val end = if (currLv == 0) lenArray(0) else getIndex(indexArray, currLv - 1, cursor(currLv - 1) + 1)
        bsearch(seekKey, start, end)
      }
      def open: Rep[Unit] = {
        cursor(currLv + 1) = getIndex(indexArray, currLv, cursor(currLv))
        currLv += 1
      }
      def up: Rep[Unit] = currLv -= 1

      def bsearch(seekKey: Rep[String], start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
        //Do we support recursion??? check tiark's tutorial
        var vstart = start
        var vend = end
        while(vstart != vend) {
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
      var currLv = 0
      var p = 0
      val res = NewArray[String](schema.length)
      var k = 0  //res
      def run(yld: Record => Rep[Unit]) = {
        while (currLv != -1) {
          if (atEnd(currLv)) {
            k -= 1
            up
            if (currLv != -1) next
          }
          else if (currLv != schema.length - 1) {
            pushIntoRes(key)
            open
          } 
          else {
            /* don't forget to call NEXT */
            pushIntoRes(key)
            yld(makeRecord)
            k -= 1
            next
          } 
        }
      }

      def inc(): Rep[Unit] = {
        //if no loop in while condition? the same code volume
        while ({
          p = (p + 1) % rels.length
          access[Boolean](p){i => callHasCol(i, currLv, rels)} == false
          }){}
      }
      //check atEnd after calling next()
      def next: Rep[Unit] = { 
        access[Unit](p){i => callNext(i, rels)}
//        println(key)
        if (access[Boolean](p){i => callAtEnd(i, rels)} == false) {
          inc
          search
//          println("after search: " + key)
        }
      }
      //check atEnd after calling seek()
      def seek(seekKey: Rep[String]): Rep[Unit] = {
        access[Unit](p){i => callSeek(i, seekKey, rels)}
        if (false == access[Boolean](p){i => callAtEnd(i, rels)}) {
          inc
          search
        }
      }
      def search: Rep[Unit] = {
        var maxp = p
        //check if atEnd pls!!!
        while ({
          if (maxp == 0) maxp = rels.length - 1
          else maxp = (maxp - 1) % rels.length
          access[Boolean](maxp){i => callHasCol(i, currLv, rels)} == false
          }){}
        var x = access[String](maxp){i => callKey(i, rels)}
        var flag = true
        while({
          var y = access[String](p){i => callKey(i, rels)}
          if (x == y) {flag = false}
          else {
            access[Unit](p){i => callSeek(i, x, rels)}
            //access[Unit](p){i => callNext(i, rels)}
            if (access[Boolean](p){i => callAtEnd(i, rels)}) {flag = false}
            else {
              x = access[String](p){i => callKey(i, rels)}
              inc
              flag = true
            }
          }
          flag
        }){}
      }
      def init: Rep[Unit] = { //find the largest element and put p as the next relation -- search gets corrct maxp 
         if (!atEnd(currLv)) {
          p = rels.length - 1
          inc
          var start = p
          var maxp = p
          var maxv = access[String](p){i => callKey(i, rels)}
          inc
          while (p > start) {
            val tmp = access[String](p){i => callKey(i, rels)}
            if(maxv < tmp) {
              maxp = p
              maxv = tmp
            }
            inc
          }
          p = maxp
          inc
          search
        }
      }
      def key: Rep[String] = access[String](p){i => callKey(i, rels)}
      def atEnd(lv: Rep[Int]): Rep[Boolean] = rels.foldLeft(unit(false))((a, x) => a || (x.hasCol(lv) && x.atEnd))      
      //don't forget to relcoate p to any eligible relation in open/up. This can be done in init()
      def open: Rep[Unit] = {
        //Must be Rep[Int]. Otherwise it can't generate code. Compilation is ok but no generated code
        val start = p:Rep[Int]
        access[Unit](p){i => callOpen(i, rels)}
        inc
        while(p != start) {
          access[Unit](p){i => callOpen(i, rels)}
//          println("p: " + p + ", " + start)
          inc
        }
        currLv += 1
        init
      }
      def up: Rep[Unit] = {
        //Must be Rep[Int]. Otherwise it can't generate code. Compilation is ok but no generated code
        val start = p:Rep[Int]
        access[Unit](p){i => callUp(i, rels)}
        inc
        while(p != start) {
          access[Unit](p){i => callUp(i, rels)}
          inc
        }
        currLv -= 1
        //init
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

    /*
     class LFTJoinAlgo {
     var schemaOfResult: Schema = null
     var array: Vector[TrieArray] = null
     var currLv = 0
     var k = 0
     var p = 0
     var res = NewArray[String](0)

     def load(array: Vector[TrieArray], schema: Schema): Rep[Unit] = {
     println("load()")
     schemaOfResult = schema
     this.array = array
     println(schemaOfResult.length)
     res = NewArray[String](schemaOfResult.length)
     }
     def run(yld: Record => Rep[Unit]): Rep[Unit] = {
     init
     println("init done!")
     //level starts from 0 and ends at -1
     while (currLv != -1) {
     println(currLv + " " + key)
     if (atEnd(currLv)) {currLv -= 1; k -= 1; array foreach {a =>
     if (a.hasCol(currLv)) a.up}; println("at end: level" + currLv);
     if (currLv != -1) next
     }
     else if (currLv != schemaOfResult.length - 1) {
     println("open: level" + currLv)
     pushIntoRes(key)
     open
     }
     else {
     /* don't forget to call NEXT */
     println("find 1 rec, call next: level" + currLv)
     pushIntoRes(key)
     yld(makeRecord)
     k -= 1
     next
     }
     print("")
     }
     }

     def inc(): Rep[Unit] = {p = (p + 1) % array.length; println("p = " + p + " length = " + array.length)}
     
     def next: Rep[Unit] = {
     access[Unit](p){i => callNext(i, array)}
     if (false == access[Boolean](p){i => callAtEnd(i, array)}) {
     inc
     search
     }
     }
     def seek(seekKey: Rep[String]): Rep[Unit] = {
     access[Unit](p){i => callSeek(i, seekKey, array)}
     if (false == access[Boolean](p){i => callAtEnd(i, array)}) {
     inc
     search
     }
     }
     //Don't use return/break/continue!!! use while(condition statement do all staff) {empty statement} to replace do...while()
     def search: Rep[Unit] = {
     var maxp = 0
     if (p == 0) maxp = array.length - 1
     else maxp = (p - 1) % array.length
     var x = access[String](maxp){i => callKey(i, array)}
     //inc
     println("p = " + maxp + " key = " + x)
     var flag = true
     while({
     var y = access[String](p){i => callKey(i, array)}
     println("p = " + p + " key = " + y + " Boolean = " + (x == y))
     if (x == y) {flag = false}
     else {
     access[Unit](p){i => callSeek(i, x, array)}
     if (access[Boolean](p){i => callAtEnd(i, array)}) {flag = false}
     else {
     x = access[String](p){i => callKey(i, array)}
     inc
     flag = true
     }
     }
     flag
     }){ }
     }
     def init: Rep[Unit] = {
     if (!atEnd(currLv)) {
     sort
     //          arr_sorted foreach {a => a.output; print("\n")}
     p = 0
     search
     }
     }

     def key: Rep[String] = array(0).key
     def atEnd(lv: Rep[Int]): Rep[Boolean] = array.foldLeft(unit(false))((a, x) => a || (x.hasCol(lv) && x.atEnd))

     def open: Rep[Unit] = {
     currLv += 1
     var i = 0
     while(i < array.length) {
     access[Unit](i){i => callOpen(i, array)}
     i += 1
     }
     init
     }
     def up: Rep[Unit] = {
     var i = 0
     while(i < array.length) {
     access[Unit](i){i => callUp(i, array)}
     i += 1
     }
     currLv -= 1
     init
     }
     def sort: Rep[Unit] = {
     //set the cursor of each relation to the 1st common element
     }

     def makeRecord: Record = {
     //Record(res, schemaOfResult)
     var i = -1;
     val key = schemaOfResult.map{x => {i += 1; res(i)}}
     Record(key, schemaOfResult)
     }
     def pushIntoRes(key: Rep[String]) = {
     res.update(k, key)
     k += 1
     }
     }*/
  }
}

//Compilation error: write to non-mutable Sym(1151) -> Some(TP(Sym(1151),Reflect(ReadVar(Variable(Sym(245))),Summary(false,false,false,false,false,false,List(Sym(245), Sym(244)),List(Sym(245), Sym(244)),List(),List()),List())))

