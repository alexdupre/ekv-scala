package com.alexdupre.ekv

import org.bouncycastle.crypto.CryptoException

import java.io.IOException

trait KeyValue {

  @throws[IOException]
  @throws[MarshalerException]
  def update[T: Marshaler](key: String, value: T): Unit

  @throws[IOException]
  @throws[CryptoException]
  @throws[UnmarshalerException]
  @throws[NoSuchElementException]
  def apply[T: Unmarshaler](key: String): T = get(key) match {
    case Some(v) => v
    case None    => throw new NoSuchElementException
  }

  @throws[IOException]
  @throws[CryptoException]
  @throws[UnmarshalerException]
  def get[T: Unmarshaler](key: String): Option[T]

  @throws[IOException]
  def delete(key: String): Unit
}
