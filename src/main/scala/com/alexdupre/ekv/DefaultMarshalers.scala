package com.alexdupre.ekv

import java.nio.charset.StandardCharsets

object DefaultMarshalers {

  val byteArray = new Marshaler[Array[Byte]] {
    override def marshal(o: Array[Byte]): Array[Byte]   = o
    override def unmarshal(b: Array[Byte]): Array[Byte] = b
  }

  val string = new Marshaler[String] {
    override def marshal(o: String): Array[Byte]   = o.getBytes(StandardCharsets.UTF_8)
    override def unmarshal(b: Array[Byte]): String = new String(b, StandardCharsets.UTF_8)
  }

  object Implicits {
    implicit val ByteArrayMarshaler = byteArray
    implicit val StringMarshaler    = string
  }

}
