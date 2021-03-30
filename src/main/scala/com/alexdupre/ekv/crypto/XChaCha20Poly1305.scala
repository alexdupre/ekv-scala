package com.alexdupre.ekv.crypto

import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.{AEADParameters, KeyParameter}

class XChaCha20Poly1305 extends ChaCha20Poly1305 {

  val nonceSize = 24

  @throws[IllegalArgumentException]
  override def init(forEncryption: Boolean, params: CipherParameters): Unit = {
    params match {
      case aeadParams: AEADParameters =>
        require(aeadParams.getNonce.length == nonceSize, "'nonce' must be 24-bytes")
        val hKey   = aeadParams.getKey.getKey
        val hNonce = new Array[Byte](16)
        System.arraycopy(aeadParams.getNonce, 0, hNonce, 0, hNonce.length)
        val key   = HChaCha20.hChaCha20(hKey, hNonce)
        val nonce = new Array[Byte](12)
        System.arraycopy(aeadParams.getNonce, 16, nonce, 4, nonce.length - 4)
        val newParams = new AEADParameters(new KeyParameter(key), aeadParams.getMacSize, nonce, aeadParams.getAssociatedText)
        super.init(forEncryption, newParams)
      case _ => throw new IllegalArgumentException("invalid parameters passed to XChaCha20Poly1305")
    }
  }
}
