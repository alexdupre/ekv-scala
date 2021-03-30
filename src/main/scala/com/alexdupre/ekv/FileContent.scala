package com.alexdupre.ekv

import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b256
import scodec.bits.BitVector
import scodec.codecs._
import scodec._

case class FileContent(header: Byte, body: Array[Byte]) {
  require(header >= 0 && header < 3, "Invalid header")
}

object FileContent {

  val codec = new Codec[FileContent] {

    override def sizeBound: SizeBound = SizeBound.bounded((1 + 4 + 32) * 8L, (1 + 4 + Int.MaxValue + 32) * 8L)

    override def encode(f: FileContent): Attempt[BitVector] = {
      for {
        header <- byte.encode(f.header)
        len    <- int32L.encode(f.body.length)
      } yield {
        val body     = BitVector.view(f.body)
        val checksum = body.digest(new Blake2b256)
        header ++ len ++ body ++ checksum
      }
    }

    override def decode(buf: BitVector): Attempt[DecodeResult[FileContent]] = {
      for {
        header         <- byte.decode(buf)
        body           <- variableSizeBytes(int32L, bytes).decode(header.remainder)
        actualChecksum <- bytes(32).complete.decode(body.remainder)
        calculatedChecksum = body.value.digest(new Blake2b256)
        result <-
          if (calculatedChecksum == actualChecksum.value)
            Attempt.successful(DecodeResult(FileContent(header.value, body.value.toArray), actualChecksum.remainder))
          else
            Attempt.failure(Err("Invalid checksum"))
      } yield result
    }
  }

}
