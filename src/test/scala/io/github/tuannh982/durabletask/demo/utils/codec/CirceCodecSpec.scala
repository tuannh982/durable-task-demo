package io.github.tuannh982.durabletask.demo.utils.codec

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec

class CirceCodecSpec extends AnyFlatSpec with MockFactory {
  behavior of "CirceCodec"

  private val defaultCodec = new CirceCodec()

  it should "correctly encode/decode primitive types" in {
    // char
    {
      val x: Char = 'a'
      val encoded = defaultCodec.encode(x)
      val decoded = defaultCodec.decode(encoded, classOf[Char])
      assert(x == decoded)
    }
    // byte
    {
      val x: Byte = 127
      val encoded = defaultCodec.encode(x)
      val decoded = defaultCodec.decode(encoded, classOf[Byte])
      assert(x == decoded)
    }
    // int
    {
      val x: Int  = 100
      val encoded = defaultCodec.encode(x)
      val decoded = defaultCodec.decode(encoded, classOf[Int])
      assert(x == decoded)
    }
    // long
    {
      val x: Long = 100
      val encoded = defaultCodec.encode(x)
      val decoded = defaultCodec.decode(encoded, classOf[Long])
      assert(x == decoded)
    }
    // float
    {
      val x: Float = 100.1f
      val encoded  = defaultCodec.encode(x)
      val decoded  = defaultCodec.decode(encoded, classOf[Float])
      assert(x == decoded)
    }
    // double
    {
      val x: Double = 100.1
      val encoded   = defaultCodec.encode(x)
      val decoded   = defaultCodec.decode(encoded, classOf[Double])
      assert(x == decoded)
    }
  }

  it should "correctly encode/decode unit type" in {
    val x: Unit       = (): Unit
    val encoded       = defaultCodec.encode(x)
    val decoded: Unit = defaultCodec.decode(encoded, classOf[Unit])
    assert(x == decoded)
  }

  it should "correctly encode/decode exception type" in {
    case class MyException(msg: String) extends Exception
    val codec = new CirceCodec(
      Map(classOf[MyException].getName -> deriveEncoder[MyException]),
      Map(classOf[MyException].getName -> deriveDecoder[MyException])
    )
    val x: MyException = MyException("some-error-occurred")
    val encoded        = codec.encode(x)
    val decoded        = codec.decode(encoded, classOf[MyException])
    assert(x == decoded)
  }
}
