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

    def fieldsEqual(a: Fields, b: Fields) = (a zip b).foldLeft(unit(true)) { (a,b) => a && b._1 == b._2 }

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
    }
    def execQuery(q: Operator): Unit = execOp(q) { _ => }

    /**
      Data Structure Implementations
      ------------------------------
      */

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
    object lftJoin{
      import scala.collection.mutable.{ArrayBuffer, ListBuffer, Stack, HashMap}

      class Node(val key: String){
	var parent: Node = null
	var next: Node = null
	val children = ArrayBuffer[Node]()

	def addChild(child: Node) = {
	  //if empty
	  if (children.isEmpty) {
	    val dummy = new Node("")
	    dummy.parent = this
	    child.parent = this
	    child.next = dummy
	    children.insert(0, child)
	  } else if (child.key < children.head.key) {	  //if head or last
	    child.next = children.head
	    children.insert(0, child)
	  } else if (child.key > children.last.key) {
	    child.next = children.last.next
	    children.last.next = child
	    children.insert(children.length, child)
	  } else {
	    val index = binarySearchUpper(child.key, 0, children.length)
	    child.next = children(index)
	    children(index - 1).next = child
	    children.insert(index, child)
	  }
	  child.parent = this
	}

	def findUpper(key: String): Node = {
	  val index = binarySearchUpper(key, 0, children.length)
	  if (index == children.length) children.last.next else children(index)
	}

	def binarySearchUpper(seekKey: String, begin: Int, end: Int): Int = {
	  if (end == begin) begin
          else {
	    val median = begin + (end - begin) / 2
	    if (seekKey == children(median).key) median
	    else if (seekKey < children(median).key) binarySearchUpper(seekKey, begin, median)
            else binarySearchUpper(seekKey, median + 1, end)
	  }
	}

	def find(key: String): Node = binarySearch(key, 0, children.length)

	def binarySearch(seekKey: String, begin: Int, end: Int): Node = {
	  if (end == begin) null
	  else {
	    val median = begin + (end - begin) / 2
	    if (seekKey == children(median).key) children(median)
	    else if (seekKey < children(median).key) binarySearch(seekKey, begin, median)
	    else binarySearch(seekKey, median + 1, end)
	  }
	}
      }

      class Relation (val vars: Schema) {
	var root: Node = new Node("")
	var current: Node = root  //Iterator of Node
		                  //Make sure current = root at the beginning of a new iteration
	/*
	 * Method: Build trie
	 */
	def build(records: ArrayBuffer[Record]) = records.foreach(this add _)
	
	//rec don't need sorted before added into trie.
	def add(rec: Record) = {
	  current = root
	  val datum = rec(vars)
	  for (data <- datum) {
	    var node = current.find(data)
	    if (node == null) {
	      node = new Node(data)
	      current.addChild(node)
	    }
	    current = node
	  }
	  current = root
	}

	/*
	 * Method: Iterate throught trie
	 */
	def key = current.key
	def next = current = current.next
	def atEnd = current.next == null
	def seek(seekKey: String) = current = current.parent.findUpper(seekKey)
	def open: Unit = current = current.children(0)
	def up: Unit = current = current.parent
      }

      class Leapfrog(var iter: List[Relation]) {
	var p = 0
	var atEnd = false
	
	def init: String = {
	  if (iter.exists(x => x.atEnd)){
	    atEnd = true
	    null
	  } else {
	    atEnd = false
	    iter = sort(iter)
	    p = 0
	    search
	  }
	}

	def sort(iter: List[Relation]): List[Relation] = {
	  val listbuf = new ListBuffer[Relation] ++ iter
	  listbuf.sortWith((x, y) => x.key < y.key).toList
	}

	def next: String = {
	  iter(p).next
	  if (iter(p).atEnd) {
	    atEnd = true
	    null
	  } else {
	    p = (p + 1) % iter.length
	    search
	  }
	}

	def seek(seekKey: String): String = {
	  iter(p).seek(seekKey)
	  if (iter(p).atEnd) {
	    atEnd = true
	    null
	  }
	  else {
	    p = (p + 1) % iter.length
	    search
	  }
	}

	def search: String = {
	  var max = iter((p + iter.length - 1) % iter.length).key
	  var min = iter(p).key
	  var key: String = null
	  while(max != min && !iter(p).atEnd){
	    min = iter(p).key
	    iter(p).seek(max)
	    if (!iter(p).atEnd) {max = iter(p).key; p = (p + 1) % iter.length} else atEnd = true
	  }
	  if (max == min) key = max
	  key
	}

	def open = iter foreach {i => i.open}
	def up = iter foreach {i => i.up}
	def key = iter(0).key
      }

      class TrieJoin(RData: ArrayBuffer[ArrayBuffer[Record]], RVar: ArrayBuffer[Schema]) {
	val leapfrog = ArrayBuffer[Leapfrog]()
	val iter = ArrayBuffer[Relation]()
	val schema = ArrayBuffer[String]()
	var depth = -1
	init()
	
	def init = {
	  val hm = new HashMap[String, Int]
	  val relAtDepth = new ArrayBuffer[ListBuffer[Int]]
	  var rel = 0
	  
	  RVar foreach {r =>
            r foreach {vname =>
	      hm.get(vname) match {
		case None =>
                  depth += 1
		  hm += (vname -> depth)
		  schema += vname

		case Some(n) =>
	      }
	      val d = hm.getOrElse(vname, -1)
	      if (relAtDepth.length == depth) relAtDepth += ListBuffer[Int](rel) else relAtDepth(d) += rel
	    }
	    rel += 1
	  }
	  RVar foreach {r => RVar.update(RVar indexOf r, r.sortWith((a, b) => hm.getOrElse(a, 0) < hm.getOrElse(b, 0)))}
	  var i = 0
	  for (i <- 0 until RVar.length) {val r = new Relation(RVar(i)); iter += r; r.build(RData(i))}
	  for (i <- 0 to depth) {val rs = ListBuffer[Relation](); relAtDepth(i) foreach {rs += iter(_)}; val lf = new Leapfrog(rs.toList); leapfrog += lf}
	}

	def run (yld: Record => Unit) = {
	  var lfj: Leapfrog  = null
	  val res = Stack[String]()
	  var needToInit = true
	  depth = 0
	  leapfrog(depth).open
	  
	  while (depth != -1) {
	    lfj = leapfrog(depth)
	    if (needToInit) {
	      lfj.init
	      needToInit = false
	    }
	    if (lfj.atEnd) {
	      lfj.up
	      depth -= 1
	      if (depth != -1) {leapfrog(depth).next; res.pop}
	    }
	    else if (depth != leapfrog.length - 1) {
	      res.push(lfj.key)
	      depth += 1
	      leapfrog(depth).open
	      needToInit = true
	    }
	    else {
	      res.push(lfj.key)
	      yld(Record(res.toVector.reverse, schema.toVector))
	      res.pop
	      lfj.next
	    }
	  }
	}
      }
    }
}}
