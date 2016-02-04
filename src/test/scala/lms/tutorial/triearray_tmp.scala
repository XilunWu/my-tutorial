/*    

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
    def updateIndex(buf: Vector[Rep[Array[Int]]], i: Rep[Int], j: Rep[Int], v: Rep[Int]): Rep[Unit] = access[Unit](i){i =>
      if (i < buf.length)
        buf(i)(j) = v
      else 
        unit()
    } 


    case LFTJoin(parents) =>
        //build TrieArrays

        val schemaOfResult = resultSchema(LFTJoin(parents))
        schemaOfResult foreach {x => print(x+" ")}; print('\n')

        val trieArrays = parents.map { p =>
          val buf = new TrieArray(1 << 16, resultSchema(p), schemaOfResult)
          execOp(p) {rec => buf += rec.fields}
          buf
    }
    //        trieArrays foreach {arr => arr.output}
    trieArrays foreach {arr => arr.toTrieArray}


class TrieArray (dataSize: Int, schema: Schema, schemaOfResult: Schema) {
      var len = 0
      val buf = schema.map(f => NewArray[String](dataSize))
      val valueArray = schema.map(f => NewArray[String](dataSize))
      val indexArray = schema.map(f => NewArray[Int](dataSize))
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
        val curr = NewArray[Int](schema.length)
        var i = 0; var j = 0
        var diff = false
        while(i < len) {
          diff = false
          while (j < schema.length) {
            if (diff || lastRecord(j) != getValue(buf, j, i)) {
              //write to non-writable data.
              updateIndex(indexArray, j, curr(j), i)
              lastRecord(j) = getValue(buf, j, i)
              curr(j) = curr(j) + 1
              diff = true
            }
//            print(getIndex(indexArray, j, curr(j) - 1) + " ")
            j += 1
          }
//          print('\n')
          i += 1
          j = 0
        }
        //for the last row
        indexArray foreach {in =>
          in(curr(j)) = len; /*print(j + " " + curr(j) + "\n");*/ j += 1
        }
//        flagTable foreach {t => print(t + " ")}; print('\n')
            //done generating TrieArray
      }
      var cursor = 0
      var currLv = 0
      val indexTrace = NewArray[Int](schema.length)

      def key: Rep[String] = getValue(buf, currLv, cursor)
      def next: Rep[Unit] = {   //set cursor to the next row according to indexArray
        indexTrace(currLv) = indexTrace(currLv) + 1
        cursor = getIndex(indexArray, currLv, indexTrace(currLv))
      }
      def atEnd: Rep[Boolean] = {
        if (currLv == 0 && cursor == len) true
        else if (cursor == getIndex(indexArray, currLv - 1, indexTrace(currLv - 1) + 1)) true
        else false
      }     
      def seek(seekKey: Rep[String]): Rep[Unit] = {   //find the first of repetitions 
        val start = if (currLv = 0) 0 else getIndex(indexArray, indexTrace(currLv - 1), currLv - 1)
        val end = if (currLv = 0) len else getIndex(indexArray, indexTrace(currLv - 1) + 1, currLv - 1)
        bsearch(seekKey, start, end)
      }
      def open: Rep[Unit] = {

        currLv += 1
      }
      def up: Rep[Unit] = currLv -= 1
    }

*/