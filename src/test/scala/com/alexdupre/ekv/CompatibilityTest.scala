package com.alexdupre.ekv

import utest._

object CompatibilityTest extends TestSuite {
  val tests = Tests {
    import DefaultMarshalers.Implicits._
    test("Go Implementation") {
      val path = os.pwd / "target" / ".ekv_testdir_compat"
      os.remove.all(path)
      os.copy(os.pwd / "src" / "test" / "resources" / "store", path)
      val s = new FileStore(path.toString, "MyStrongPassword")
      s[String]("One") ==> "FirstValue"
      s[String]("Two") ==> "SecondValue"
      s[String]("Three") ==> "ThirdValue"
      s[String]("Four") ==> "FourthValue"
    }
  }
}
