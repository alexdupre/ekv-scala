package com.alexdupre.ekv

import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import os.Path

import java.nio.charset.StandardCharsets
import java.nio.file.NoSuchFileException
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable

class FileStore(basedir: String, password: String, rng: Random = new SecureRandom()) extends KeyValue {

  import Crypto._
  import IO._

  private val rwLock = new ReentrantReadWriteLock

  private val keyLocks = mutable.Map.empty[String, ReentrantReadWriteLock]

  val basePath = Path(basedir, os.pwd)

  init()

  private def init(): Unit = {
    os.makeDir.all(
      basePath,
      if (System.getProperty("os.name").startsWith("Windows")) null
      else "rw-------"
    )
    if (!os.isDir(basePath)) sys.error("Unable to create base directory")

    // Get the path to the ".ekv" file
    val ekvPath = ".ekv"

    val expectedContent = "version:1".getBytes(StandardCharsets.UTF_8)
    // Try to read the .ekv.1/2 file, if it exists then we check its content
    try {
      val ekvCiphertext = read(basePath, ekvPath)
      val ekvContents   = decrypt(ekvCiphertext, password)
      if (!Arrays.areEqual(ekvContents, expectedContent)) sys.error("Invalid ekv version file")
    } catch {
      case _: NoSuchFileException => // ignore
    }
    write(basePath, ekvPath, encrypt(expectedContent, password, rng))
  }

  private def getLock(encryptedKey: String): ReentrantReadWriteLock = {
    rwLock.readLock().lock()
    try {
      if (keyLocks.contains(encryptedKey)) return keyLocks(encryptedKey)
    } finally rwLock.readLock().unlock()
    rwLock.writeLock().lock()
    try {
      if (keyLocks.contains(encryptedKey)) return keyLocks(encryptedKey)
      val lock = new ReentrantReadWriteLock()
      keyLocks(encryptedKey) = lock
      lock
    } finally rwLock.writeLock().unlock()
  }

  private def getKey(key: String): String = {
    val encryptedKey = hashStringWithPassword(key, password)
    Hex.toHexString(encryptedKey)
  }

  override def update[T: Marshaler](key: String, value: T): Unit = {
    val encryptedKey      = getKey(key)
    val m                 = implicitly[Marshaler[T]]
    val data              = m.marshal(value)
    val encryptedContents = encrypt(data, password, rng)
    val lck               = getLock(encryptedKey)
    lck.writeLock().lock()
    try {
      write(basePath, encryptedKey, encryptedContents)
    } finally lck.writeLock().unlock()
  }

  override def get[T: Marshaler](key: String): Option[T] = {
    val encryptedKey = getKey(key)
    val lck          = getLock(encryptedKey)
    lck.readLock().lock()
    try {
      val encryptedContents =
        try {
          read(basePath, encryptedKey)
        } finally lck.readLock().unlock()
      val decryptedContents = decrypt(encryptedContents, password)
      val m                 = implicitly[Marshaler[T]]
      Some(m.unmarshal(decryptedContents))
    } catch {
      case _: NoSuchFileException => None
    }
  }

  override def delete(key: String): Unit = {
    val encryptedKey = getKey(key)
    val lck          = getLock(encryptedKey)
    lck.writeLock().lock()
    try {
      deleteFiles(basePath, encryptedKey, rng)
    } finally lck.writeLock().unlock()
  }
}
