package com.alexdupre.ekv

import com.alexdupre.ekv.crypto.{HChaCha20, XChaCha20Poly1305}
import org.bouncycastle.crypto.params.{AEADParameters, KeyParameter}
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import utest._

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

object CryptoTest extends TestSuite {
  val tests = Tests {
    val rng = new SecureRandom()
    test("Encrypt and Decrypt") {
      val plaintext  = "Hello, World!".getBytes(StandardCharsets.UTF_8)
      val password   = "test_password"
      val cipherText = Crypto.encrypt(plaintext, password, rng)
      Crypto.decrypt(cipherText, password) ==> plaintext
    }
    test("HChaCha20") {
      val key      = Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
      val nonce    = Hex.decode("000000090000004a0000000031415927")
      val expected = Hex.decode("82413b4227b27bfed30e42508a877d73a0f9e4d58a74a853c12ec41326d3ecdc")
      val result   = HChaCha20.hChaCha20(key, nonce)
      Arrays.areEqual(expected, result) ==> true
    }
    test("XChaCha20Poly1305") {
      val plaintext  = Hex.decode("0f5ca45a54875d1d19e952e53caeaa19389342f776dab11723535503338d6f77202a37")
      val key        = Hex.decode("1031bc920d4fcb4434553b1bf2d25ab375200643bf523ff037bf8914297e8dca")
      val nonce      = Hex.decode("4cc77e2ef5445e07b5f44de2dc5bf62d35b8c6f69502d2bf")
      val ciphertext = Hex.decode("7aa8669e1bfe8b0688899cdddbb8cee31265928c66a69a5090478da7397573b1cc0f64121e7d8bff8db0ddd3c17460d7f29a12")
      val chaCipher  = new XChaCha20Poly1305
      val params     = new AEADParameters(new KeyParameter(key), 128, nonce)
      chaCipher.init(false, params)
      chaCipher.processAADBytes(Array.emptyByteArray, 0, 0)
      val out    = new Array[Byte](chaCipher.getOutputSize(ciphertext.length))
      val outOff = chaCipher.processBytes(ciphertext, 0, ciphertext.length, out, 0)
      chaCipher.doFinal(out, outOff)
      Arrays.areEqual(out, plaintext) ==> true
    }
  }
}
