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
        else f(3)
        /*
        else if (i == 4) f(4)
        else if (i == 5) f(5)
        else if (i == 6) f(6)
        else if (i == 7) f(7)
        else if (i == 8) f(8)
        else if (i == 9) f(9)
        else if (i == 10) f(10)
        else if (i == 11) f(11)
        else if (i == 12) f(12)
        else if (i == 13) f(13)
        else if (i == 14) f(14)
        else f(15)
        */
      }

      def getIndex(i: Int, j: Rep[Int], vec: Vector[Rep[Array[Int]]]): Rep[Int] = {
        if (i < vec.length) {
          return vec(i)(j)
        }
        else
          return unit(0)
      }

      def getElem(i: Int, j: Rep[Int], vec: Vector[Rep[Array[String]]]): Rep[String] = {
        if (i < vec.length) {
          return vec(i)(j)
        }
        else
          return unit("")
      }

      def callKey(i: Int, vec: Vector[TrieArray]): Rep[String] = {
        if (i < vec.length) {
          return vec(i).key
        }
        else
          return unit("")
      }

      def callNext(i: Int, vec: Vector[TrieArray]): Rep[Unit] = {
        if (i < vec.length) {
          return vec(i).next
        }
        else
          return unit()
      }

      def callAtEnd(i: Int, vec: Vector[TrieArray]): Rep[Boolean] = {
        if (i < vec.length) {
          return vec(i).atEnd
        }
        else
          return unit(true)
      }

      def callOpen(i: Int, vec: Vector[TrieArray]): Rep[Unit] = {
        if (i < vec.length) {
          return vec(i).open
        }
        else
          return unit()
      }

      def callUp(i: Int, vec: Vector[TrieArray]): Rep[Unit] = {
        if (i < vec.length) {
          return vec(i).up
        }
        else
          return unit()
      }

      def callSeek(i: Int, x: Rep[String], vec: Vector[TrieArray]): Rep[Unit] = {
        if (i < vec.length) {
          return vec(i).seek(x)
        }
        else
          return unit()
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
      case LFTJoin(parents)        => val schema = scala.collection.mutable.ArrayBuffer[String]()
        val hs = scala.collection.mutable.HashSet[String]()
        parents foreach {
          resultSchema(_) foreach { vname =>
            if (!hs.contains(vname)) {
              hs += vname
              schema += vname
            }
          }
        }
        schema.toVector
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
      case PrintCSV(parent) =>
        val schema = resultSchema(parent)
        printSchema(schema)
        execOp(parent) { rec => printFields(rec.fields) }
      case LFTJoin(parents) =>
        println("LFTJoin starts!")
        println("Number of relations: " + parents.length)
        
        val schemaOfResult = resultSchema(o)
        val buf = parents.map(p => new TrieArrayBuffer(1 << 16, resultSchema(p), schemaOfResult))
        (parents, buf).zipped foreach {(p, b) =>
          val schema = resultSchema(p)
          //Let's assume schema keeps in order
          execOp(p) {rec => b += rec.fields}
        }

        val array = buf.toVector.map(b => b.toTrieArray)
        println("load(): ")
        array foreach {a => a.output; print('\n')}
        
        val lftjoin = new LFTJoinAlgo
        lftjoin.load(array, schemaOfResult)
        lftjoin.run(yld)
        
    }
    def execQuery(q: Operator): Unit = execOp(q) { _ => }

    /**
      Data Structure Implementations
      ------------------------------
      */

    class TrieArrayBuffer (dataSize: Int, schema: Schema, schemaOfResult: Schema) {
      val buf = schema.map(f => NewArray[String](dataSize))
      var len = 0
      println("This is a TrieArrayBuffer of size: " + dataSize)

      def +=(x: Seq[Rep[String]]) = {
        this(len) = x
        len += 1
      }
      def update(i: Rep[Int], x: Seq[Rep[String]]) = {
        (buf,x).zipped.foreach((b,x) => b(i) = x)
      }
      def apply(i: Rep[Int]) = {
        buf.map(b => b(i))
      }
      def sort: Rep[Unit] = {}

      /*  print the result for the sake of debug  */
      def toTrieArray: TrieArray = {
        val elemArray = schema.map(f => NewArray[String](dataSize))
        val indexArray = schema.map(f => NewArray[Int](dataSize))
        val elem = NewArray[String](schema.length)
        val next = NewArray[Int](schema.length)
        val lenOfArray = NewArray[Int](schema.length)
        //PrimitiveOps.scala:  implicit def intToRepInt(x: Int) = unit(x)
        println("Start toTrieArray. dataSize: " + dataSize)
        var i = 0
        var j: Int = 0
        for (x <- this(i)) {
          elem(j) = x
          //how to get elem?
          elemArray(j)(next(j)) = x
          //which boundary to put?
          //Boundary is the location of its first child
          //For the last attribute, there's no index array
          if (j < schema.length - 1) indexArray(j)(next(j)) = next(j + 1)
          next(j) = next(j) + 1
          j += 1
        }
        i += 1

        var isNew = false
        while (i < len) {
          j = 0
          isNew = false
          for (x <- this(i)) {
            if (elem(j) != x) {isNew = true; elem(j) = x}
            if (isNew) {elemArray(j)(next(j)) = x; if (j < schema.length - 1) indexArray(j)(next(j)) = next(j + 1); next(j) = next(j) + 1}
            j += 1
          }
          i += 1
        }
        
        j = 0
        while (j < schema.length) {
          lenOfArray(j) = next(j) 
          //print("j = " + j + " lenOfArray = " + lenOfArray(j) + " ")
          j += 1
        }
        print('\n')
        /*
         j = 0
         for(x <- elemArray) {
         println("j = " + j + " lenOfArray = " + lenOfArray(j))
         for(i <- 0 until lenOfArray(j)) {
         print(x(i) + " ")
         }
         print("\n")
         for(i <- 0 until lenOfArray(j)) {
         print(indexArray(j)(i) + " ")
         }
         print("\n")
         j += 1
         }
        */
        
        var k = 0
        i = 0
        var track = NewArray[Int](schema.length)
        var res = NewArray[String](schema.length)
        println("track is of size: " + track.length)
        /*
        while (k != 0 || !atEnd(k, i)) {
          if (atEnd(k, i) && k != 0) {k -= 1; i = track(k) + 1 /*  up 1 level, modify i  */}
          else if (k != schema.length - 1) {push(k, i); track(k) = i; k += 1; i = access[Int](k){k => indexArray(k)(i)}}
          else {push(k, i); dumpArray; i += 1}
        }
        
        def atEnd(k: Rep[Int], i: Rep[Int]): Rep[Boolean] = {
          if (k == 0) return i == lenOfArray(0)
          else return i == access[Int](k - 1){k => indexArray(k)(track(k) + 1)}
        }
        def push(k: Rep[Int], i: Rep[Int]): Rep[Unit] = res(k) = access[String](k){k => elemArray(k)(i)}
        def dumpArray: Rep[Unit] = {res foreach (x => print(x + " ")); print('\n')}
        */
        new TrieArray(schema, schemaOfResult, elemArray, indexArray, lenOfArray)
      }
    }

    class TrieArray(schema: Schema, schemaOfResult: Schema, value: Vector[Rep[Array[String]]], index: Vector[Rep[Array[Int]]], len: Rep[Array[Int]]) {
      val col = schema.map (s => s indexOf schemaOfResult)
      var currLv = 0  //  current level in TrieArray
      val currPos = NewArray[Int](schema.length)  //  current pos of cursor in each level
      for (i <- 0 until currPos.length) {currPos(i) = 0; println(currPos(i))}

      def output: Rep[Unit] = {
        println("schema: ")
        schema foreach(x => print(x + " ")); print('\n')
        println("resultSchema: ")
        col foreach {x => print(x + " ")}; print('\n')
        println("value: " + schemaOfResult.length)
        println("lenOfArray: ")
        len foreach {l => print(l + " ")}; print('\n')
        
        var i = 0
        var j = 0
        for (i <- 0 until schemaOfResult.length){
          j = 0
          while (j < len(i)) {
            print(access[String](i){i => getElem(i, j, value)} + " ")
            j += 1
          }
          print(j + " " + i)
          print('\n')
        }
      }

      def hasCol(i: Rep[Int]): Rep[Boolean] = {
        //For test
         true
        //col.foldLeft(unit(false))((a,x) => a || (x == i))
      }
      /**
        Trie iterator interface
        */
      def key: Rep[String] = {
        //access[String](currLv) { i => value(i)(currPos(i))}
        access[String](currLv){i => getElem(i, currPos(i), value)}
      }
      def next: Rep[Unit] = currPos(currLv) = currPos(currLv) + 1
      def atEnd: Rep[Boolean] = {
        if (currPos(currLv) == len(currLv)) true
        else if (currLv != 0 && currPos(currLv) == access[Int](currLv) {i => getIndex(i, currPos(i), index)}) true
        else false
      }
      def seek(seekKey: Rep[String]): Rep[Unit] = {
        //put currPos onto correct position where key is the first key which >= seekKey
        val start = access[Int](currLv){i => getIndex(i - 1, currPos(i - 1), index)}
        val end = access[Int](currLv){i => getIndex(i - 1, currPos(i - 1) + 1, index)}
        binSearch(seekKey, start, end)
        /**
          Helper function
          */
        def binSearch(seekKey: Rep[String], start: Rep[Int], end: Rep[Int]): Rep[Unit] = {
          if (start == end) currPos(currLv) = start
          else {
            val pivot = access[String](currLv){i => getElem(i, (start + end) / 2, value)}
            if (pivot <= seekKey) binSearch(seekKey, (start + end) / 2 + 1, end)
            else binSearch(seekKey, start, (start + end) / 2)
          }
        }
      }
      def open: Rep[Unit] = {currPos(currLv + 1) = access[Int](currLv){i => getIndex(i, currPos(i), index)}; currLv += 1}
      def up: Rep[Unit] = currLv -= 1
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


    class LFTJoinAlgo {
      var schemaOfResult: Schema = null
      var array: Vector[TrieArray] = null
      var arr_sorted: Vector[TrieArray] = null
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
          println(currLv)
          if (atEnd(currLv)) {currLv -= 1; k -= 1; array foreach {a => 
            if (a.hasCol(currLv)) a.up}}
          else if (currLv != schemaOfResult.length - 1) {
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

      def inc(x: Var[Int]): Var[Int] = {p += 1; if (p == k) p = 0; p}
      
      def next: Rep[Unit] = {
        access[Unit](p){i => callNext(i, arr_sorted)}
        if (access[Boolean](p){i => callAtEnd(i, arr_sorted)})
          return;
        else {
          inc(p)
          search
        }        
      }
      def seek(seekKey: Rep[String]): Rep[Unit] = {
        access[Unit](p){i => callSeek(i, seekKey, arr_sorted)}
        if (access[Boolean](p){i => callAtEnd(i, arr_sorted)}) return;
        else {
          inc(p)
          search
        }
      }
      def search: Rep[Unit] = {
        var x = access[String](p){i => callKey(i, arr_sorted)}
        p = inc(p)
        while(true) {
          var y = access[String](p){i => callKey(i, arr_sorted)}
          if (x == y) return;
          else {
            access[Unit](p){i => callSeek(i, x, arr_sorted)}
            if (access[Boolean](p){i => callAtEnd(i, arr_sorted)}) return;
            else {
              x = access[String](p){i => callKey(i, arr_sorted)}
              p = inc(p)
            }
          }
        }
      }
      def init: Rep[Unit] = {
        if (atEnd(currLv)) return;
        else {
          sort
          p = 0
          search
        }
      }
    
      def key: Rep[String] = arr_sorted(0).key
      def atEnd(lv: Rep[Int]): Rep[Boolean] = array.foldLeft(unit(false))((a, x) => a || x.hasCol(lv) && x.atEnd)

      def open: Rep[Unit] = {
        currLv += 1
        p = 0
        while(p < k) {
          access[Unit](p){i => callOpen(i, arr_sorted)}
          p += 1
        }
        init
      }
      def up: Rep[Unit] = {
        p = 0
        while(p < k) {
          access[Unit](p){i => callUp(i, arr_sorted)}
          p += 1
        }
        currLv -= 1
        init
      }
      def sort: Rep[Unit] = {
        //set arr_sorted
        //For test. 
        arr_sorted = array
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
    }
  }
}
