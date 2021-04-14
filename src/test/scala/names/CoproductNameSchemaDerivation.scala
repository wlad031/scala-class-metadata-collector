package dev.vgerasimov.shapelse
package names

import org.scalacheck.{ Arbitrary, ScalacheckShapeless }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

//noinspection TypeAnnotation
class CoproductNameSchemaDerivation
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with ScalacheckShapeless {

  import names.implicits.all._

  test("Name schema for trait containing two simple case classes should be derivable") {
    sealed trait T
    case class A(i: Int) extends T
    case class B(s: String) extends T

    implicit val arbitrary = implicitly[Arbitrary[T]]
    val encoder = namesSchemaEncoder[T]
    val expected = CoproductSchema(
      "",
      List(
        ProductSchema("A", List(IntSchema("i"))),
        ProductSchema("B", List(StringSchema("s")))
      )
    )
    encoder.encode shouldBe expected
    forAll { (a: T) => encoder.encode(a) shouldBe expected }
  }
}
