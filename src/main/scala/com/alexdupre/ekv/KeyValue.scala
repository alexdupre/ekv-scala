package com.alexdupre.ekv

import org.bouncycastle.crypto.CryptoException

import java.io.IOException

trait KeyValue {

  @throws[IOException]
  @throws[MarshalerException]
  def update[T: Marshaler](key: String, value: T): Unit

  @throws[IOException]
  @throws[CryptoException]
  @throws[MarshalerException]
  @throws[NoSuchElementException]
  def apply[T: Marshaler](key: String): T = get(key) match {
    case Some(v) => v
    case None    => throw new NoSuchElementException
  }

  @throws[IOException]
  @throws[CryptoException]
  @throws[MarshalerException]
  def get[T: Marshaler](key: String): Option[T]

  @throws[IOException]
  def delete(key: String): Unit
}
