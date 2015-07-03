import org.specs2.mutable.Specification
import play.api.libs.iteratee._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.extras.iteratees._

object JsonSpec extends Specification {
  "json iteratee" should {
    "parse an empty object" in test(Json.obj())
    "parse a string" in test(Json.obj("string" -> "value"))
    "parse escaped values in a string" in test(JsString("start\"\n\r\\end"))
    "parse a number" in test(Json.obj("number" -> 10))
    "parse true" in test(Json.obj("boolean" -> true))
    "parse false" in test(Json.obj("boolean" -> false))
    "parse null" in test(Json.obj("obj" -> JsNull))
    "parse an empty array" in test(Json.obj("array" -> Json.arr()))
    "parse an array with stuff in it" in test(Json.obj("array" -> Json.arr("foo", "bar")))
    "parse a complex object" in test(Json.obj(
      "string" -> "value",
      "number" -> 10,
      "boolean" -> true,
      "null" -> JsNull,
      "array" -> Json.arr(Json.obj("foo" -> "bar"), 20),
      "obj" -> Json.obj("one" -> 1, "two" -> 2, "nested" -> Json.obj("spam" -> "eggs"))
    ))
    "parse a object out of a character enumerator with newlines using Enumeratee.grouped and Concurrent.broadcast" in testJsObjectWithWhitespaces()
    "parse an array out of a character enumerator with newlines using Enumeratee.grouped and Concurrent.broadcast" in testJsArrayWithWhitespaces()
  }

  def test(json: JsValue) = {
    Await.result(Enumerator(CharString.fromString(Json.stringify(json))) |>>> JsonIteratees.jsValue, Duration.Inf) must_== json
    def testGrouped(n: Int) = {
      Await.result(Enumerator.enumerate(Json.stringify(json).grouped(n).map(CharString.fromString)) |>>> JsonIteratees.jsValue, Duration.Inf) must_== json
    }
    testGrouped(1)
    testGrouped(2)
    testGrouped(7)
  }


  def testJsObjectWithWhitespaces() = {
    val data = """
    {"key1": "value1"} {"key2": "value2"}{"key3": 
    "value3"} 

    """
    val (en, chann) = play.api.libs.iteratee.Concurrent.broadcast[CharString]
    import scala.concurrent.ExecutionContext.Implicits.global

    val res = en &> Enumeratee.grouped(JsonIteratees.jsSimpleObject) |>>> Iteratee.getChunks
    chann.push(CharString.fromString(data))
    chann.eofAndEnd()
    Await.result(res, Duration.Inf).mkString must_== data.filterNot(_.isWhitespace).mkString
  }

  def testJsArrayWithWhitespaces() = {
    val data = """
    [1, 3, 4,5,
    6,7
    ,10,11, [0, 0, 0]

    ]

    """
    val (en, chann) = play.api.libs.iteratee.Concurrent.broadcast[CharString]
    import scala.concurrent.ExecutionContext.Implicits.global

    val res = en &> Enumeratee.grouped(JsonIteratees.jsSimpleArray) |>>> Iteratee.getChunks
    chann.push(CharString.fromString(data))
    chann.eofAndEnd()
    Await.result(res, Duration.Inf).mkString must_== data.filterNot(_.isWhitespace).mkString
  }
}
