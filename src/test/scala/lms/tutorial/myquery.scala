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

  trait StagedTestDriver extends TestDriver with StagedQueryProcessor {
    var dynamicFileName: Table = _
    override def dynamicFilePath(table: String): Table = if (table == "?") dynamicFileName else unit(filePath(table))
    def snippet(fn: Table): Rep[Unit] = {
      dynamicFileName = fn
      execQuery(PrintCSV(parsedQuery))
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
        new CStagedQueryDriver(name, query) with query_optc.QueryCompiler {
          // FIXME: hack so i don't need to replace Value -> #Value in all the files right now
          override def isNumericCol(s: String) = s == "Value" || super.isNumericCol(s)
        }
      )
    drivers.foreach(_.runtest)
  }

  // NOTE: we can use "select * from ?" to use dynamic file names (not used here right now)

  val query = "TRIANGLE"
  val sf = 1
  trait ExpectedASTs extends QueryAST {
    val scan_t = Scan("t.csv")
    val postfix = sf match {
      case 1 => ".1"
      case 10 => ".10"
    }
    val scan_1gram = Scan("1gram.csv",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))
    val scan_t1gram = Scan("t1gram.csv",Some(Schema("Phrase", "Year", "MatchCount", "VolumeCount")),Some('\t'))
    val scan_customer = Scan("../../../data/"+query+"/customer"+postfix,Some(Schema("#CUSTKEY","C_NAME","C_ADDRESS","#NATIONKEY","PHONE","ACCTBAL","MKTSEGMENT","C_COMMENT")),Some('\t'))
    val scan_nation = Scan("../../../data/"+query+"/nation"+postfix,Some(Schema("#NATIONKEY","N_NAME","#REGIONKEY","N_COMMENT")),Some('\t'))
    val scan_region = Scan("../../../data/"+query+"/region"+postfix,Some(Schema("#REGIONKEY","R_NAME","R_COMMENT")),Some('\t'))
    val scan_lineitem = Scan("../../../data/"+query+"/lineitem"+postfix,Some(Schema("#ORDERKEY","#PARTKEY","#SUPPKEY","#LINENUMBER","#QUANTITY","EXTENDEDPRICE","DISCOUNT","TAX","RETURNFLAG","LINESTATUS","#SHIPDATE","#COMMITDATE","#RECEIPTDATE","SHIPINSTRUCT","SHIPMODE","L_COMMENT")),Some('\t'))
    val scan_orders = Scan("../../../data/"+query+"/orders"+postfix,Some(Schema("#ORDERKEY","#CUSTKEY","ORDERSTATUS","TOTALPRICE","#ORDERDATE","ORDERPRIORITY","CLERK","#SHIPPRIORITY","O_COMMENT")),Some('\t'))
    val scan_supplier = Scan("../../../data/"+query+"/supplier"+postfix,Some(Schema("#SUPPKEY","S_NAME","S_ADDRESS","#NATIONKEY","S_PHONE","S_ACCTBAL","S_COMMENT")),Some('\t'))
    val scan_part = Scan("../../../data/"+query+"/part"+postfix,Some(Schema("#PARTKEY","P_NAME","P_MFGR","P_BRAND","P_TYPE","P_SIZE","P_CONTAINER","P_RETAILPRICE","P_COMMENT")),Some('\t'))
    val scan_partsupp = Scan("../../../data/"+query+"/partsupp"+postfix,Some(Schema("#PARTKEY","#SUPPKEY","PS_AVAILQTY","PS_SUPPLYCOST","PS_COMMENT")),Some('\t'))
    val scan_gplus_edge = Scan("../../../data/"+query+"/gplus/gplus_coded.txt",Some(Schema("#X", "#Y")),Some(' '))

    val expectedAstForTest = Map(/*
      "Q5" -> Group(Schema("N_NAME"), Schema("#COUNT"),  //Here we need hack Group to support count(*)
        LFTJoin(List(
          Project(Schema("#REGIONKEY","#NATIONKEY","N_NAME"), Schema("#REGIONKEY","#NATIONKEY","N_NAME"), scan_nation),
          Project(Schema("#REGIONKEY"), Schema("#REGIONKEY"), Filter(Eq(Field("R_NAME"), Value("ASIA")), scan_region)),
          Project(
            Schema("#NATIONKEY","#SUPPKEY"),
            Schema("#NATIONKEY","#SUPPKEY"),  
            scan_supplier),
          Project(
            Schema("#NATIONKEY","#CUSTKEY"),
            Schema("#NATIONKEY","#CUSTKEY"),
            scan_customer),
          Project(
            Schema("#CUSTKEY","#ORDERKEY"), 
            Schema("#CUSTKEY","#ORDERKEY"), 
            Filter(GTE(Field("#ORDERDATE"), Value(19960101)), 
              Filter(LT(Field("#ORDERDATE"), Value(19970101)), 
                scan_orders))),
          Project(
            Schema("#ORDERKEY","#SUPPKEY","#PARTKEY"),
            Schema("#ORDERKEY","#SUPPKEY","#PARTKEY"),
            scan_lineitem)
        ))),
      "Q9" -> Group(Schema("N_NAME"), Schema("#COUNT"),
        LFTJoin(List(
          Project(Schema("#NATIONKEY","N_NAME"), Schema("#NATIONKEY","N_NAME"), scan_nation),
          Project(
            Schema("#NATIONKEY","#SUPPKEY"),
            Schema("#NATIONKEY","#SUPPKEY"),  
            scan_supplier),
          Project(
            Schema("#ORDERKEY"), 
            Schema("#ORDERKEY"), 
            scan_orders),
          Project(
            Schema("#SUPPKEY","#PARTKEY","#ORDERKEY"),
            Schema("#SUPPKEY","#PARTKEY","#ORDERKEY"),
            scan_lineitem),
          Project(
            Schema("#PARTKEY"),
            Schema("#PARTKEY"),
            Filter(CON(Field("P_NAME"), Value("green")),
              scan_part)),
          Project(
            Schema("#SUPPKEY","#PARTKEY"),
            Schema("#SUPPKEY","#PARTKEY"),
            scan_partsupp)
        ))),*/
      "TRIANGLE" -> Count(LFTJoin(List(
          Project(Schema("#X", "#Y"), Schema("#X", "#Y"), scan_gplus_edge),
          Project(Schema("#Y", "#Z"), Schema("#X", "#Y"), scan_gplus_edge),
          Project(Schema("#X", "#Z"), Schema("#X", "#Y"), scan_gplus_edge)), List("Edge","Edge","Edge")))
    )
  }

  val defaultEvalTable = dataFilePath("1gram.csv")
  val t1gram = "? schema Phrase, Year, MatchCount, VolumeCount delim \\t"

  //testquery("t1gram1", s"select * from $t1gram")
  testquery(query, "")
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

