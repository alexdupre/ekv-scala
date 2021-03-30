package com.alexdupre.ekv.crypto

import org.bouncycastle.util.{Pack, Strings}

object HChaCha20 {

  private val SIGMA: Array[Int] = Pack.littleEndianToInt(Strings.toByteArray("expand 32-byte k"), 0, 4)

  def hChaCha20(key: Array[Byte], nonce: Array[Byte]): Array[Byte] = {
    val out = new Array[Byte](32)
    hChaCha20(out, key, nonce)
    out
  }

  private def hChaCha20(out: Array[Byte], key: Array[Byte], nonce: Array[Byte]): Unit = {
    require(out.length == 32, "'out' must be 32-bytes")
    require(key.length == 32, "'key' must be 32-bytes")
    require(nonce.length == 16, "'nonce' must be 16-bytes")

    var x00 = SIGMA(0)
    var x01 = SIGMA(1)
    var x02 = SIGMA(2)
    var x03 = SIGMA(3)
    var x04 = Pack.littleEndianToInt(key, 0)
    var x05 = Pack.littleEndianToInt(key, 4)
    var x06 = Pack.littleEndianToInt(key, 8)
    var x07 = Pack.littleEndianToInt(key, 12)
    var x08 = Pack.littleEndianToInt(key, 16)
    var x09 = Pack.littleEndianToInt(key, 20)
    var x10 = Pack.littleEndianToInt(key, 24)
    var x11 = Pack.littleEndianToInt(key, 28)
    var x12 = Pack.littleEndianToInt(nonce, 0)
    var x13 = Pack.littleEndianToInt(nonce, 4)
    var x14 = Pack.littleEndianToInt(nonce, 8)
    var x15 = Pack.littleEndianToInt(nonce, 12)

    for (_ <- 1 to 20 by 2) {
      // Diagonal round.
      x00 += x04
      x12 = Integer.rotateLeft(x12 ^ x00, 16)
      x08 += x12
      x04 = Integer.rotateLeft(x04 ^ x08, 12)
      x00 += x04
      x12 = Integer.rotateLeft(x12 ^ x00, 8)
      x08 += x12
      x04 = Integer.rotateLeft(x04 ^ x08, 7)

      x01 += x05
      x13 = Integer.rotateLeft(x13 ^ x01, 16)
      x09 += x13
      x05 = Integer.rotateLeft(x05 ^ x09, 12)
      x01 += x05
      x13 = Integer.rotateLeft(x13 ^ x01, 8)
      x09 += x13
      x05 = Integer.rotateLeft(x05 ^ x09, 7)

      x02 += x06
      x14 = Integer.rotateLeft(x14 ^ x02, 16)
      x10 += x14
      x06 = Integer.rotateLeft(x06 ^ x10, 12)
      x02 += x06
      x14 = Integer.rotateLeft(x14 ^ x02, 8)
      x10 += x14
      x06 = Integer.rotateLeft(x06 ^ x10, 7)

      x03 += x07
      x15 = Integer.rotateLeft(x15 ^ x03, 16)
      x11 += x15
      x07 = Integer.rotateLeft(x07 ^ x11, 12)
      x03 += x07
      x15 = Integer.rotateLeft(x15 ^ x03, 8)
      x11 += x15
      x07 = Integer.rotateLeft(x07 ^ x11, 7)

      // Column round.
      x00 += x05
      x15 = Integer.rotateLeft(x15 ^ x00, 16)
      x10 += x15
      x05 = Integer.rotateLeft(x05 ^ x10, 12)
      x00 += x05
      x15 = Integer.rotateLeft(x15 ^ x00, 8)
      x10 += x15
      x05 = Integer.rotateLeft(x05 ^ x10, 7)

      x01 += x06
      x12 = Integer.rotateLeft(x12 ^ x01, 16)
      x11 += x12
      x06 = Integer.rotateLeft(x06 ^ x11, 12)
      x01 += x06
      x12 = Integer.rotateLeft(x12 ^ x01, 8)
      x11 += x12
      x06 = Integer.rotateLeft(x06 ^ x11, 7)

      x02 += x07
      x13 = Integer.rotateLeft(x13 ^ x02, 16)
      x08 += x13
      x07 = Integer.rotateLeft(x07 ^ x08, 12)
      x02 += x07
      x13 = Integer.rotateLeft(x13 ^ x02, 8)
      x08 += x13
      x07 = Integer.rotateLeft(x07 ^ x08, 7)

      x03 += x04
      x14 = Integer.rotateLeft(x14 ^ x03, 16)
      x09 += x14
      x04 = Integer.rotateLeft(x04 ^ x09, 12)
      x03 += x04
      x14 = Integer.rotateLeft(x14 ^ x03, 8)
      x09 += x14
      x04 = Integer.rotateLeft(x04 ^ x09, 7)
    }

    Pack.intToLittleEndian(x00, out, 0)
    Pack.intToLittleEndian(x01, out, 4)
    Pack.intToLittleEndian(x02, out, 8)
    Pack.intToLittleEndian(x03, out, 12)
    Pack.intToLittleEndian(x12, out, 16)
    Pack.intToLittleEndian(x13, out, 20)
    Pack.intToLittleEndian(x14, out, 24)
    Pack.intToLittleEndian(x15, out, 28)
  }

}
