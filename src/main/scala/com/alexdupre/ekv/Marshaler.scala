package com.alexdupre.ekv

trait Marshaler[T] {
  @throws[MarshalerException]
  def marshal(o: T): Array[Byte]

  @throws[MarshalerException]
  def unmarshal(b: Array[Byte]): T

  def map[S](f: S => T, g: T => S): Marshaler[S] = {
    val m = this
    new Marshaler[S] {
      override def marshal(o: S): Array[Byte] = m.marshal(f(o))
      override def unmarshal(b: Array[Byte]): S = g(m.unmarshal(b))
    }
  }
}
