package io.github.tuannh982.durabletask.demo.utils.codec

import io.circe.{Decoder, Encoder}

import scala.runtime.BoxedUnit

case class WrappedJson(cls: String, json: String)

private object WrappedJson {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

  val encoder: Encoder.AsObject[WrappedJson] = deriveEncoder[WrappedJson]
  val decoder: Decoder[WrappedJson]          = deriveDecoder[WrappedJson]
}

class CirceCodec(
  encoderMap: Map[String, Encoder[_]] = Map(),
  decoderMap: Map[String, Decoder[_]] = Map()
) extends Codec {
  import io.circe._
  import io.circe.syntax._

  private val primitiveTypes = Set(
    classOf[Char].getName,
    classOf[Byte].getName,
    classOf[Int].getName,
    classOf[Long].getName,
    classOf[Float].getName,
    classOf[Double].getName
  )

  private val typeMapConversion: Map[String, String] = Map(
    classOf[java.lang.Character].getName -> classOf[Char].getName,
    classOf[java.lang.Byte].getName      -> classOf[Byte].getName,
    classOf[java.lang.Integer].getName   -> classOf[Int].getName,
    classOf[java.lang.Long].getName      -> classOf[Long].getName,
    classOf[java.lang.Float].getName     -> classOf[Float].getName,
    classOf[java.lang.Double].getName    -> classOf[Double].getName,
    classOf[BoxedUnit].getName           -> classOf[Unit].getName
  )

  private def resolveClassName(className: String): String = {
    if (primitiveTypes.contains(className)) {
      className
    } else if (typeMapConversion.contains(className)) {
      typeMapConversion(className)
    } else {
      className
    }
  }

  private val resolvedEncoderMap: Map[String, Encoder[_]] = Map[String, Encoder[_]](
    classOf[Char].getName   -> Encoder[Char],
    classOf[Byte].getName   -> Encoder[Byte],
    classOf[Int].getName    -> Encoder[Int],
    classOf[Long].getName   -> Encoder[Long],
    classOf[Float].getName  -> Encoder[Float],
    classOf[Double].getName -> Encoder[Double],
    classOf[String].getName -> Encoder[String],
    classOf[Unit].getName   -> Encoder[Unit]
  ) ++ encoderMap.map {
    case (k, v) =>
      resolveClassName(k) -> v
  }

  private val resolvedDecoderMap: Map[String, Decoder[_]] = Map[String, Decoder[_]](
    classOf[Char].getName   -> Decoder[Char],
    classOf[Byte].getName   -> Decoder[Byte],
    classOf[Int].getName    -> Decoder[Int],
    classOf[Long].getName   -> Decoder[Long],
    classOf[Float].getName  -> Decoder[Float],
    classOf[Double].getName -> Decoder[Double],
    classOf[String].getName -> Decoder[String],
    classOf[Unit].getName   -> Decoder[Unit]
  ) ++ decoderMap.map {
    case (k, v) =>
      resolveClassName(k) -> v
  }

  override def encode(v: Any): String = {
    val className = resolveClassName(v.getClass.getName)
    val wrapped = WrappedJson(
      className,
      v.asJson(resolvedEncoderMap(className).asInstanceOf[Encoder[Any]]).noSpaces
    )
    wrapped.asJson(WrappedJson.encoder).noSpaces
  }

  override def decode[T](v: String, clazz: Class[T]): T = {
    val result = for {
      json    <- parser.parse(v)
      wrapped <- json.as[WrappedJson](WrappedJson.decoder)
      _ <- Either.cond(
        clazz.getName == wrapped.cls,
        Unit,
        new RuntimeException(
          s"wrong class to decode, expected ${clazz.getName}, actual ${wrapped.cls}, payload ${wrapped.json}"
        )
      )
      wrappedJson <- parser.parse(wrapped.json)
      result      <- wrappedJson.as[T](resolvedDecoderMap(resolveClassName(clazz.getName)).asInstanceOf[Decoder[T]])
    } yield result
    result match {
      case Left(err)    => throw err
      case Right(value) => value
    }
  }

  override def tryDecode(v: String): Any = {
    val result = for {
      json        <- parser.parse(v)
      wrapped     <- json.as[WrappedJson](WrappedJson.decoder)
      wrappedJson <- parser.parse(wrapped.json)
      result      <- wrappedJson.as[Any](resolvedDecoderMap(resolveClassName(wrapped.cls)).asInstanceOf[Decoder[Any]])
    } yield result
    result match {
      case Left(err)    => throw err
      case Right(value) => value
    }
  }
}
