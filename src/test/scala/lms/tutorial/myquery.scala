/**
  my unit test file for lftj
*/

package scala.lms.tutorial

import scala.lms.common._
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
      //Since our output C file is under src/out directory
      //"../data/" + table

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
    override def runtest: Unit = {
      if (version == "query_staged0" && List("Group","HashJoin").exists(parsedQuery.toString contains _)) return ()
      test(version+" "+name) {
        for (expectedParsedQuery <- expectedAstForTest.get(name)) {
          assert(expectedParsedQuery==parsedQuery)
        }
        check(name, code)
        precompile
        //checkOut(name, "csv", eval(defaultEvalTable))
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
        //checkOut(name, "csv", eval(defaultEvalTable))
      }
    }
  }

  def testquery(name: String, query: String = "") {
    val drivers: List[TestDriver] =
      List(
//        new ScalaPlainQueryDriver(name, query) with query_unstaged.QueryInterpreter,
//        new ScalaStagedQueryDriver(name, query) with query_staged0.QueryCompiler,
//        new ScalaStagedQueryDriver(name, query) with query_staged.QueryCompiler,
        new CStagedQueryDriver(name, query) with query_optc.QueryCompiler {
          // FIXME: hack so i don't need to replace Value -> #Value in all the files right now
          override def isNumericCol(s: String) = s == "Value" || super.isNumericCol(s)
        }
      )
    drivers.foreach(_.runtest)
  }

  // NOTE: we can use "select * from ?" to use dynamic file names (not used here right now)

  trait ExpectedASTs extends QueryAST {
    val scan_t = Scan("t.csv")
    val scan_1gram = Scan("1gram.csv",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))
    val scan_t1gram = Scan("t1gram.csv",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))
    val scan_customer = Scan("../../../data/customer.tbl",Some(Schema("#CUSTKEY","C_NAME","C_ADDRESS","#NATIONKEY","PHONE","ACCTBAL","MKTSEGMENT","C_COMMENT")),Some('\t'))
    val scan_nation = Scan("../../../data/nation.tbl",Some(Schema("#NATIONKEY","N_NAME","#REGIONKEY","N_COMMENT")),Some('\t'))
    val scan_region = Scan("../../../data/region.tbl",Some(Schema("#REGIONKEY","R_NAME","R_COMMENT")),Some('\t'))
    val scan_lineitem = Scan("../../../data/lineitem.tbl",Some(Schema("#ORDERKEY","#PARTKEY","#SUPPKEY","#LINENUMBER","#QUANTITY","EXTENDEDPRICE","DISCOUNT","TAX","RETURNFLAG","LINESTATUS","#SHIPDATE","#COMMITDATE","#RECEIPTDATE","SHIPINSTRUCT","SHIPMODE","L_COMMENT")),Some('\t'))
    val scan_orders = Scan("../../../data/orders.tbl",Some(Schema("#ORDERKEY","#CUSTKEY","ORDERSTATUS","TOTALPRICE","#ORDERDATE","ORDERPRIORITY","CLERK","#SHIPPRIORITY","O_COMMENT")),Some('\t'))
    val scan_supplier = Scan("../../../data/supplier.tbl",Some(Schema("#SUPPKEY","S_NAME","S_ADDRESS","#NATIONKEY","S_PHONE","S_ACCTBAL","S_COMMENT")),Some('\t'))
    
    val expectedAstForTest = Map(
      "lftj_q5" -> /*Group(Schema("N_NAME"), Schema("#COUNT"),  //Here we need hack Group to support count(*)
        LFTJoin(List(
          Project(Schema("#NATIONKEY","N_NAME","#REGIONKEY"), Schema("#NATIONKEY","N_NAME","#REGIONKEY"), scan_nation),
          Project(Schema("#REGIONKEY"), Schema("#REGIONKEY"), Filter(Eq(Field("R_NAME"), Value("ASIA")), scan_region)),
          Project(
            Schema("#SUPPKEY","#NATIONKEY"),
            Schema("#SUPPKEY","#NATIONKEY"),
            scan_supplier),
          Project(
            Schema("#CUSTKEY","#NATIONKEY"),
            Schema("#CUSTKEY","#NATIONKEY"),
            scan_customer),
          Project(
            Schema("#ORDERKEY","#CUSTKEY"), 
            Schema("#ORDERKEY","#CUSTKEY"), 
            Filter(GTE(Field("#ORDERDATE"), Value(19960101)), 
              Filter(LT(Field("#ORDERDATE"), Value(19970101)), 
                scan_orders))),
          Project(
            Schema("#ORDERKEY","#PARTKEY","#SUPPKEY"),
            Schema("#ORDERKEY","#PARTKEY","#SUPPKEY"),
            scan_lineitem)
        )))*/
        Project(
            Schema("#ORDERKEY","#CUSTKEY"), 
            Schema("#ORDERKEY","#CUSTKEY"), 
            Filter(GTE(Field("#ORDERDATE"), Value(19960101)), 
              Filter(LT(Field("#ORDERDATE"), Value(19970101)), 
                scan_orders)))
    )
  }

  val defaultEvalTable = dataFilePath("1gram.csv")
  val t1gram = "? schema Phrase, Year, MatchCount, VolumeCount delim \\t"

//  testquery("t1gram1", s"select * from $t1gram")
  testquery("lftj_q5", "")
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

