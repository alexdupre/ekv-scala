package com.alexdupre.ekv

import utest._

object FileStoreTest extends TestSuite {
  private def newFileStore(name: String, password: String = "Hello, World!", clean: Boolean = true) = {
    val path = os.pwd / "target" / name
    if (clean) os.remove.all(path)
    new FileStore(path.toString, password)
  }
  val tests = Tests {
    import DefaultMarshalers.Implicits._
    test("Smoke") {
      val s = newFileStore(".ekv_testdir")
      s("TestMe123") = "Hi"
      s[String]("TestMe123") ==> "Hi"
    }
    test("Multiset") {
      val s = newFileStore(".ekv_testdir_multiset")
      for (i <- 1 to 20) {
        val v = s"Hi, $i!"
        s("TestMe123") = v
        s[String]("TestMe123") ==> v
      }
    }
    test("Reopen") {
      val s        = newFileStore(".ekv_testdir_reopen")
      var expected = "Hi"
      s("TestMe123") = expected
      for (i <- 1 to 20) {
        val s = newFileStore(".ekv_testdir_reopen", clean = false)
        s[String]("TestMe123") ==> expected
        expected = s"Hi, $i!"
        s("TestMe123") = expected
      }
    }
    test("Delete") {
      val s = newFileStore(".ekv_testdir_delete")
      s("TestMe123") = "Hi"
      s.delete("TestMe123")
      s.get[String]("TestMe123") ==> None
      intercept[NoSuchElementException] {
        s[String]("TestMe123")
      }
    }
    test("BadPass") {
      newFileStore(".ekv_testdir_badpass")
      intercept[Exception] {
        newFileStore(".ekv_testdir_badpass", "badpassword", false)
      }
    }
  }
}
