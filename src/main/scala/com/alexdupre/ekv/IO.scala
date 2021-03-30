package com.alexdupre.ekv

import os.Path
import scodec.bits.BitVector

import java.io.IOException
import java.nio.file.NoSuchFileException
import java.util.Random
import scala.concurrent.blocking
import scala.util.{Failure, Success, Try}

object IO extends IO

trait IO {

  // getPaths returns "path.1" and "path.2"
  def getPaths(baseDir: Path, file: String): (Path, Path) =
    (baseDir / s"$file.1", baseDir / s"$file.2")

  def getPathsArray(baseDir: Path, file: String): Array[Path] =
    Array(baseDir / s"$file.1", baseDir / s"$file.2")

  def getFileOrder(path1: Path, path2: Path): List[Path] = {
    val t1 = Try(os.read.bytes(path1, 0, 1))
    val t2 = Try(os.read.bytes(path2, 0, 1))
    (t1, t2) match {
      case (Failure(e1), Failure(e2)) =>
        if (e1.isInstanceOf[NoSuchFileException] && e2.isInstanceOf[NoSuchFileException])
          Nil
        else
          throw new IOException(s"Invalid read finding newest file: ${e1.getMessage}, ${e2.getMessage}", e1)
      case (Success(_), Failure(_)) => List(path1)
      case (Failure(_), Success(_)) => List(path2)
      case (Success(s1), Success(s2)) =>
        compareModMonCntr(s1(0), s2(0)) match {
          case 1 => List(path1, path2)
          case 2 => List(path2, path1)
          case _ => throw new IOException(s"ModMonCntr Invalid Values: $s1, $s2")
        }
    }
  }

  // compareModMonCntr returns 1 if t1 is newer, 2 if t2 is newer, and 0 if
  // there is an error. newer is defined as the second of 3 cases:
  // (0 < 1), (1 < 2), (2 < 0). Anything else is an error
  def compareModMonCntr(t1: Byte, t2: Byte): Byte = {
    // NOTE: Yes, the following could be cleverer -- don't "improve" it.
    // t1 cases, 1 > 0, 2 > 1 and 0 > 2
    if ((t1 == 1 && t2 == 0) || (t1 == 2 && t2 == 1) || (t1 == 0 && t2 == 2)) 1
    // t2 cases, 0 < 1, 1 < 2, and 2 < 0
    else if ((t1 == 0 && t2 == 1) || (t1 == 1 && t2 == 2) || (t1 == 2 && t2 == 0)) 2
    // everything else is an error
    else 0
  }

  // readContents of a file, checking the checksum and returning the data.
  def readContents(f: Path): FileContent = {
    val contents = os.read.bytes(f)
    FileContent.codec.decodeValue(BitVector.view(contents)).require
  }

  // read returns the contents of the newest file for which it
  // can read all elements and validate the internal checksum
  def read(baseDir: Path, file: String): Array[Byte] = {
    val (path1, path2) = getPaths(baseDir, file)
    val filesToRead = getFileOrder(path1, path2)
    filesToRead
      .foldLeft(Try[FileContent](throw new NoSuchFileException(file))) { case (acc, path) =>
        acc match {
          case Failure(_) => Try(readContents(path))
          case success    => success
        }
      }
      .map(_.body)
      .get
  }

  def write(baseDir: Path, file: String, data: Array[Byte]): Unit = blocking {
    val (path1, path2) = getPaths(baseDir, file)
    val filesToRead    = getFileOrder(path1, path2)
    val (fileToWrite, modMonCntr) = filesToRead
      .foldLeft(
        Try[(Path, FileContent)](throw new NoSuchFileException(file))
      ) { case (acc, path) =>
        acc match {
          case Failure(_) => Try(readContents(path)).map(fc => path -> fc)
          case success    => success
        }
      }
      .map { case (path, fc) =>
        val modMonCntr = ((fc.header + 1) % 3).toByte
        if (path == path1) (path2, modMonCntr) else (path1, modMonCntr)
      }
      .getOrElse(path1 -> 0.toByte)
    val buf = FileContent.codec.encode(FileContent(modMonCntr, data)).require.toByteArray
    os.write.over(fileToWrite, buf)
    readContents(fileToWrite) // verify it's readable
  }

  // deleteFile overwrites a files contents with random data and then deletes the file
  def deleteFile(path: Path, rng: Random): Unit = blocking {
    try {
      val info = os.stat(path)
      val buf  = new Array[Byte](info.size.toInt)
      rng.nextBytes(buf)
      os.write.over(path, buf)
      os.remove(path)
    } catch {
      case _: NoSuchFileException => // ignore
    }
  }

  def deleteFiles(baseDir: Path, file: String, rng: Random): Unit = {
    getPathsArray(baseDir, file).foreach(file => deleteFile(file, rng))
  }

}
