/**
  my unit test file for lftj
*/

package scala.lms.tutorial

import scala.virtualization.lms.common._
import org.scalatest.FunSuite

/**

Unit Tests
----------

*/
class LFTjoinQueryTest extends TutorialFunSuite {
  val under = "lftjquery_"

  trait TestDriver extends SQLParser with QueryProcessor with ExpectedASTs {
    def runtest: Unit
    override def filePath(table: String) = dataFilePath(table)

    def name: String
    def query: String
    def parsedQuery: Operator = if (query.isEmpty) expectedAstForTest(name) else parseSql(query)
  }

  trait PlainTestDriver extends TestDriver with PlainQueryProcessor {
    override def dynamicFilePath(table: String): Table = if (table == "?") defaultEvalTable else filePath(table)
    def eval(fn: Table): Unit = {
      execQuery(PrintCSV(parsedQuery))
    }
  }

  trait StagedTestDriver extends TestDriver with StagedQueryProcessor {
    var dynamicFileName: Table = _
    override def dynamicFilePath(table: String): Table = if (table == "?") dynamicFileName else unit(filePath(table))
    def snippet(fn: Table): Rep[Unit] = {
      dynamicFileName = fn
      execQuery(PrintCSV(parsedQuery))
    }
  }

  abstract class ScalaPlainQueryDriver(val name: String, val query: String) extends PlainTestDriver with QueryProcessor { q =>
    override def runtest: Unit = {
      test(version+" "+name) {
        for (expectedParsedQuery <- expectedAstForTest.get(name)) {
          assert(expectedParsedQuery==parsedQuery)
        }
        checkOut(name, "csv", eval(defaultEvalTable))
      }
    }
  }

  abstract class ScalaStagedQueryDriver(val name: String, val query: String) extends DslDriver[String,Unit] with StagedTestDriver with StagedQueryProcessor with ScannerExp { q =>
    override val codegen = new DslGen with ScalaGenScanner {
      val IR: q.type = q
    }
    import java.io._
    import scala.tools.nsc._
    import scala.tools.nsc.util._
    import scala.tools.nsc.reporters._
    import scala.tools.nsc.io._
    import scala.tools.nsc.interpreter.AbstractFileClassLoader
    override def compile[String,Unit](f: Exp[String] => Exp[Unit])(implicit mA: Manifest[String], mB: Manifest[Unit]): String=>Unit = {
      if (this.compiler eq null)
        setupCompiler()
      /*
      val className = "staged$" + compileCount
      compileCount += 1
      */
      val className = "Snippet"
      val source = new StringWriter()
      val writer = new PrintWriter(source)
      val staticData = codegen.emitSource(f, className, writer)

      codegen.emitDataStructures(writer)

      val compiler = this.compiler
      val run = new compiler.Run

      val fileSystem = new VirtualDirectory("<vfs>", None)
      compiler.settings.outputDirs.setSingleOutput(fileSystem)
    //      compiler.genJVM.outputDir = fileSystem

      //run.compileSources(List(new util.BatchSourceFile("<stdin>", source.toString)))
      run.compileSources(List(new util.BatchSourceFile("<stdin>", readFile("src/out/lftjquery_lftj_1gram1.check.scala"))))
      reporter.printSummary()

      reporter.reset
      //output.reset

      val parent = this.getClass.getClassLoader
      val loader = new AbstractFileClassLoader(fileSystem, this.getClass.getClassLoader)
      val cls: Class[_] = loader.loadClass(className)
      val cons = cls.getConstructor(staticData.map(_._1.tp.erasure):_*)
      
      val obj: String=>Unit = cons.newInstance(staticData.map(_._2.asInstanceOf[AnyRef]):_*).asInstanceOf[String=>Unit]
      obj
    }
    override def runtest: Unit = {
      if (version == "query_staged0" && List("Group","HashJoin").exists(parsedQuery.toString contains _)) return ()
      test(version+" "+name) {
        for (expectedParsedQuery <- expectedAstForTest.get(name)) {
          assert(expectedParsedQuery==parsedQuery)
        }
//comment for develop. uncomment after it. 
        //check(name, code)
        precompile
        checkOut(name, "csv", eval(defaultEvalTable))
      }
    }
  }

  abstract class CStagedQueryDriver(val name: String, val query: String) extends DslDriverC[String,Unit] with StagedTestDriver with StagedQueryProcessor with ScannerLowerExp { q =>
    override val codegen = new DslGenC with CGenScannerLower {
      val IR: q.type = q
    }
    override def runtest: Unit = {
      test(version+" "+name) {
        for (expectedParsedQuery <- expectedAstForTest.get(name)) {
          assert(expectedParsedQuery==parsedQuery)
        }
        check(name, code, "c")
        //precompile
        checkOut(name, "csv", eval(defaultEvalTable))
      }
    }
  }

  def testquery(name: String, query: String = "") {
    val drivers: List[TestDriver] =
      List(
//        new ScalaPlainQueryDriver(name, query) with query_unstaged.QueryInterpreter,
//        new ScalaStagedQueryDriver(name, query) with query_staged0.QueryCompiler,
        new ScalaStagedQueryDriver(name, query) with query_staged.QueryCompiler
/*
        new CStagedQueryDriver(name, query) with query_optc.QueryCompiler {
          // FIXME: hack so i don't need to replace Value -> #Value in all the files right now
          override def isNumericCol(s: String) = s == "Value" || super.isNumericCol(s)
        }
*/
      )
    drivers.foreach(_.runtest)
  }

  // NOTE: we can use "select * from ?" to use dynamic file names (not used here right now)

  trait ExpectedASTs extends QueryAST {
    val scan_t = Scan("t.csv")
    val scan_1gram = Scan("1gram.csv.1",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))
    val scan_t1gram = Scan("t1gram.csv",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))

    val expectedAstForTest = Map(
      "1gram1" -> scan_1gram,
      "t1gram1" -> scan_t1gram,
      "lftj_t1gram1" -> LFTJoin(List(scan_t1gram, scan_t1gram)),
      "lftj_1gram1" -> LFTJoin(List(scan_1gram, scan_1gram)),
      "lftj_1gram2" -> LFTJoin(List(scan_1gram, Filter(Eq(Field("Phrase"), Value("Auswanderung")), scan_1gram)))
    )
  }

  val defaultEvalTable = dataFilePath("1gram.csv")
  val t1gram = "? schema Phrase, Year, MatchCount, VolumeCount delim \\t"

//  testquery("t1gram1", s"select * from $t1gram")
  //print("Total execution time: ")
  testquery("lftj_1gram1", "")
  //println()
}



/**
Suggestions for Exercises
-------------------------

The query engine we presented is decidedly simple, so as to present an
end-to-end system that can be understood in total. Below are a few
suggestions for interesting extensions.

- Implement a scanner that reads on demand from a URL.

  (Cool with: a new operator that only prints the first N results.)

- (easy) Implement a typed schema in the Scala version, so that the
  types of columns are statically known, while the values are not.

  (Hint: the C version already does this, but is also more involved
  because of the custom type representations.)

- Implement more predicates (e.g. `LessThan`) and predicate
  combinators (e.g. `And`, `Or`) in order to run more interesting
  queries.

- (medium) Implement a real column-oriented database, where each column has its
  own file so that it can be read independently.

- Implement an optimizer on the relational algebra before generating code.
  (Hint: smart constructors might help.)

*/

