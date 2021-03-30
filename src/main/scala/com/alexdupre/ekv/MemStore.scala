package com.alexdupre.ekv

import java.util.concurrent.ConcurrentHashMap

class MemStore extends KeyValue {

  private val store = new ConcurrentHashMap[String, Array[Byte]]

  override def update[T: Marshaler](key: String, value: T): Unit = {
    val m = implicitly[Marshaler[T]]
    store.put(key, m.marshal(value))
  }

  override def get[T: Unmarshaler](key: String): Option[T] = {
    val m = implicitly[Unmarshaler[T]]
    Option(store.get(key)).map(m.unmarshal)
  }

  override def delete(key: String): Unit = store.remove(key)
}
