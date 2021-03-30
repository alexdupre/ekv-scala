package com.alexdupre.ekv

import utest._

object MemStoreTest extends TestSuite {
  val tests = Tests {
    import Marshalers._
    val s = new MemStore
    test("Smoke") {
      s("TestMe123") = "Hi"
      s[String]("TestMe123") ==> "Hi"
    }
    test("Multiset") {
      for (i <- 1 to 20) {
        val v = s"Hi, $i!"
        s("TestMe123") = v
        s[String]("TestMe123") ==> v
      }
    }
    test("Delete") {
      s("TestMe123") = "Hi"
      s.delete("TestMe123")
      s.get[String]("TestMe123") ==> None
      intercept[NoSuchElementException] {
        s[String]("TestMe123")
      }
    }
  }
}
