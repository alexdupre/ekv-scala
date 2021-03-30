package com.alexdupre.ekv

import java.nio.charset.StandardCharsets

object Marshalers {

  implicit def StringMarshaller = new Marshaler[String] with Unmarshaler[String] {
    override def marshal(o: String): Array[Byte]   = o.getBytes(StandardCharsets.UTF_8)
    override def unmarshal(b: Array[Byte]): String = new String(b, StandardCharsets.UTF_8)
  }

}
