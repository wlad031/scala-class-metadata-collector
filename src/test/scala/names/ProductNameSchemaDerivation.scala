package dev.vgerasimov.shapelse
package names

import org.scalacheck.{ Arbitrary, ScalacheckShapeless }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

//noinspection TypeAnnotation
class ProductNameSchemaDerivation
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with ScalacheckShapeless {

  import names.implicits.all._

  test("Name schema for case class containing nothing should be derivable") {
    case class C()
    implicit val arbitrary = implicitly[Arbitrary[C]]
    val encoder = namesSchemaEncoder[C]
    val expected = ProductSchema("", List())
    encoder.encode shouldBe expected
    forAll { (a: C) => encoder.encode(a) shouldBe expected }
  }

  test("Name schema for case class containing only primitives should be derivable") {
    case class C(i: Int, s: String, b: Boolean, i2: Int)
    implicit val arbitrary = implicitly[Arbitrary[C]]
    val encoder = namesSchemaEncoder[C]
    val expected = ProductSchema(
      "",
      List(
        IntSchema("i"),
        StringSchema("s"),
        BooleanSchema("b"),
        IntSchema("i2")
      )
    )
    encoder.encode shouldBe expected
    forAll { (a: C) => encoder.encode(a) shouldBe expected }
  }

  test("Name schema for case class containing nested case class should be derivable") {
    case class C1(k: Int, b: Boolean)
    case class C(s: String, n: C1, i: Int)
    implicit val arbitrary = implicitly[Arbitrary[C]]
    val encoder = namesSchemaEncoder[C]
    val expected =
      ProductSchema(
        "",
        List(
          StringSchema("s"),
          ProductSchema(
            "n",
            List(
              IntSchema("k"),
              BooleanSchema("b")
            )
          ),
          IntSchema("i")
        )
      )
    encoder.encode shouldBe expected
    forAll { (a: C) => encoder.encode(a) shouldBe expected }
  }
}
