package com.alexdupre.ekv

import com.alexdupre.ekv.crypto.XChaCha20Poly1305
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.params.{AEADParameters, KeyParameter}

import java.nio.charset.StandardCharsets
import java.util.Random

object Crypto extends Crypto

trait Crypto {

  def blake2bSum256(data: Array[Array[Byte]]): Array[Byte] = {
    val md = new Blake2bDigest(256)
    data.foreach(b => md.update(b, 0, b.length))
    val out = new Array[Byte](md.getDigestSize)
    md.doFinal(out, 0)
    out
  }

  def blake2bSum256(data: Array[Byte]): Array[Byte] = blake2bSum256(Array(data))

  def hashStringWithPassword(data: String, password: String): Array[Byte] = {
    val dHash = blake2bSum256(data.getBytes(StandardCharsets.UTF_8))
    val pHash = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
    blake2bSum256(Array(pHash, dHash))
  }

  def encrypt(data: Array[Byte], password: String, rng: Random): Array[Byte] = {
    val pwHash    = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
    val chaCipher = new XChaCha20Poly1305
    val nonce     = new Array[Byte](chaCipher.nonceSize)
    rng.nextBytes(nonce)
    val params = new AEADParameters(new KeyParameter(pwHash), 128, nonce)
    chaCipher.init(true, params)
    val out = new Array[Byte](nonce.length + chaCipher.getOutputSize(data.length))
    System.arraycopy(nonce, 0, out, 0, nonce.length)
    val outOff = chaCipher.processBytes(data, 0, data.length, out, nonce.length)
    chaCipher.doFinal(out, nonce.length + outOff)
    out
  }

  def decrypt(data: Array[Byte], password: String): Array[Byte] = {
    val pwHash              = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
    val chaCipher           = new XChaCha20Poly1305
    val (nonce, ciphertext) = data.splitAt(chaCipher.nonceSize)
    val params              = new AEADParameters(new KeyParameter(pwHash), 128, nonce)
    chaCipher.init(false, params)
    val out    = new Array[Byte](chaCipher.getOutputSize(ciphertext.length))
    val outOff = chaCipher.processBytes(ciphertext, 0, ciphertext.length, out, 0)
    chaCipher.doFinal(out, outOff)
    out
  }

}
