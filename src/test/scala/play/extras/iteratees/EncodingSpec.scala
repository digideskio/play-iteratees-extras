package play.extras.iteratees

import org.specs2.mutable._

import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
object EncodingSpec extends Specification {
  
  "decode" should {
    "decode one byte chunk" in {
      convertBytes("Hello".getBytes) must_== "Hello"
    }
    "decode multiple byte chunks" in {
      convertBytes("Hello".getBytes, " ".getBytes, "world".getBytes, "!".getBytes) must_== "Hello world!"
    }
    "decode a chunk with multibyte characters" in {
      convertBytes("ßöé☃".getBytes("UTF-8")) must_== "ßöé☃"
    }
    "decode byte chunks with a split characters" in {
      convertBytes(split("ö".getBytes("UTF-8"), 1): _*) must_== "ö"
    }
    "decode byte chunks with lots of split characters" in {
      convertBytes(split("ßöé☃".getBytes("UTF-8"), 1, 3, 4, 6): _*) must_== "ßöé☃"
    }
    "decode multi byte characters that have been split multiple times" in {
      // rendering note: the character below is 4 bytes. Your text editor/IDE may have issues.
      convertBytes(split("𠜎".getBytes("UTF-8"), 1, 2, 3): _*) must_== "𠜎"
    }
    "decode an empty byte chunk" in {
      convertBytes("".getBytes) must_== ""
    }
  }

  def convertBytes(byteArrays: Array[Byte]*) =
    new String(Await.result(Enumerator(byteArrays:_*) &> Encoding.decode() &> Enumeratee.map[CharString](_.mkString.toCharArray) |>>> Iteratee.consume[Array[Char]](), Duration.Inf))

  def split(bytes: Array[Byte], pos: Int*): List[Array[Byte]] = {
    pos.toList match {
      case Nil => List(bytes)
      case at :: tail => {
        val s = bytes.splitAt(at)
        s._1 :: split(s._2, tail.map(_ - at):_*)
      }
    }
  }
}
