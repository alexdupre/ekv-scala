package com.alexdupre.ekv

trait Marshaler[T] {
  @throws[MarshalerException]
  def marshal(o: T): Array[Byte]
}
