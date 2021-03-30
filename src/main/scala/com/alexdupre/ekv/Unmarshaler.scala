package com.alexdupre.ekv

trait Unmarshaler[T] {
  @throws[UnmarshalerException]
  def unmarshal(b: Array[Byte]): T
}
