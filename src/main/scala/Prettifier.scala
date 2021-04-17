package dev.vgerasimov.shapelse

import dev.vgerasimov.shapelse.values._

import scala.collection.mutable.ListBuffer

trait Prettifier[A] {
  def prettify(a: A)(implicit config: Prettifier.Config): fansi.Str
}

object Prettifier {

  final case class Data(value: Value, fieldName: String, typeName: String)

  object implicits {
    implicit val defaultConfig: Config = Config()
  }

  final case class Config(
    indent: Int = 2,
    maxWidth: Int = 80,
    withProductFieldNames: Boolean = true,
    withPrimitiveTypeNames: Boolean = false,
    modifiers: Modifiers = Modifiers()
  )

  final case class Modifiers(
    typeName: String => fansi.Str = fansi.Str(_),
    fieldName: String => fansi.Str = fansi.Str(_),
    value: String => fansi.Str = fansi.Str(_),
    chars: String => fansi.Str = fansi.Str(_)
  )

  private final class Buffer() {
    private val listBuffer = ListBuffer[fansi.Str]()
    private var length_ = 0

    def append(elem: fansi.Str): Buffer = {
      listBuffer.append(elem)
      length_ += elem.length
      this
    }

    def append(elem: String): Buffer = append(fansi.Str(elem))

    def length(): Int = length_

    def clear(): Unit = {
      listBuffer.clear()
      length_ = 0
    }

    //noinspection JavaAccessorMethodOverriddenAsEmptyParen
    def toFansiStr(): fansi.Str = listBuffer.reduce(_ ++ _)

    def result(): fansi.Str = {
      val res = toFansiStr()
      clear()
      res
    }
  }

  def instance[A](implicit shapeEncoder: ShapeInstanceEncoder[Data, A]): Prettifier[A] = {
    new Prettifier[A] {
      override def prettify(a: A)(implicit cfg: Prettifier.Config): fansi.Str = {
        def indent(lvl: Int): String = " ".repeat(lvl * cfg.indent)

        val f = cfg.modifiers.chars
        val (eq, lp, rp, lb, rb, cm) = (f("="), f("("), f(")"), f("{"), f("}"), f(","))

        val sb = new Buffer()

        def iter(shape: Shape[Data], lvl: Int, prevLen: Int): fansi.Str = {
          shape match {

            case shape: PrimitiveShape[_] =>
              if (cfg.withProductFieldNames)
                sb.append(cfg.modifiers.fieldName(shape.meta.fieldName)).append(" ").append(eq).append(" ")
              if (cfg.withPrimitiveTypeNames)
                sb.append(cfg.modifiers.typeName(shape.meta.typeName)).append(lp)
              sb.append(cfg.modifiers.value(foo(shape.meta.value)))
              if (cfg.withPrimitiveTypeNames)
                sb.append(rp)
              sb.result()

            case OptionShape(meta, shape) => ???

            case ListShape(meta, childs) =>
              if (cfg.withProductFieldNames && meta.fieldName != "")
                sb.append(cfg.modifiers.fieldName(meta.fieldName)).append(" ").append(eq).append(" ")
              sb.append(cfg.modifiers.typeName(shape.meta.typeName)).append(" ").append(lb).append(" ")

              val c = childs.map(sc => iter(sc, lvl + 1, prevLen + sb.length()))
              val len = prevLen + sb.length() + c.map(x => x.length).sum + (c.length - 1) * 2 + 1

              if (len > cfg.maxWidth) {
                sb.append("\n")
                  .append(c.map(x => indent(lvl + 1) + x).mkString("\n"))
                  .append("\n")
                  .append(indent(lvl))
                  .append(")")
              } else {
                sb.append(c.mkString(", ")).append(cfg.modifiers.typeName(" }"))
              }

              sb.result()

            case ProductShape(meta, childs) =>
              if (cfg.withProductFieldNames && shape.meta.fieldName != "")
                sb.append(cfg.modifiers.fieldName(s"${shape.meta.fieldName} = "))
              sb.append(cfg.modifiers.typeName(s"${shape.meta.typeName} { "))

              val c = childs.map(sc => iter(sc, lvl + 1, prevLen + sb.length()))
              val len = prevLen + sb.length() + c.map(x => x.length).sum + (c.length - 1) * 2 + 1

              if (len > cfg.maxWidth) {
                sb.append("\n")
                  .append(c.map(x => indent(lvl + 1) + x).mkString("\n"))
                  .append("\n")
                  .append(indent(lvl))
                  .append(")")
              } else {
                sb.append(c.mkString(", ")).append(cfg.modifiers.typeName(" }"))
              }

              sb.result()

            case CoproductShape(meta, childs1) =>
              val product = childs1
                .find(s => {
                  val value: Value = s.meta.value
                  foo(value) == "product"
                })
                .map(x => x.asInstanceOf[ProductShape[Data]])
              iter(product.get, lvl, prevLen)

          }
        }

        iter(shapeEncoder.encode(a), 0, 0)
      }
    }
  }

  private def foo(value: Value): String = value match {
    case NilValue            => "nil"
    case ProductValue        => "product"
    case CoproductValue      => "coproduct"
    case ListValue           => "list"
    case BooleanValue(value) => value.toString
    case CharValue(value)    => s"${value.toString}c"
    case StringValue(value)  => s""""$value""""
    case ByteValue(value)    => s"${value.toString}b"
    case ShortValue(value)   => s"${value.toString}s"
    case IntValue(value)     => s"${value.toString}i"
    case LongValue(value)    => s"${value.toString}l"
    case FloatValue(value)   => s"${value.toString}f"
    case DoubleValue(value)  => s"${value.toString}d"
  }
}