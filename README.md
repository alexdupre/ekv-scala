# Encrypted KV Store

EKV is a directory and file-based encrypted key value storage library
with metadata protection written in scala. This project is a direct port
of the original [Elixxir's ekv](https://gitlab.com/elixxir/ekv)
implementation in golang. It is intended for use in
mobile and desktop applications where one may want to transfer
protected files to a new device while protecting the nature of the
information of what is stored in addition to the contents.

Features:
1. Both the key and the contents behind the key are protected on disk.
2. A best-effort approach is used to store and flush changes to disk.
3. Thread-safety within the same EKV instance.

EKV is not a secured memory enclave. Data is protected when stored to
disk, not RAM.

EKV requires a cryptographically secure random number generator.
By default it uses Java's SecureRandom implementation.

EKV is released under the simplified BSD License.

## Known Limitations and Roadmap

EKV has several known limitations at this time:

1. The code is currently in beta and has not been audited.
2. The password to open and close the store is a string that can be
   dumped from memory.
3. EKV protects keys and contents, it doesn't protect the size of
   those files or the number of unique keys being stored in the
   database. We would like to include controls for EKV users to hide that
   information by setting a block size for files and adding a number of
   fake files to the directory.
4. Users are currently limited to the number of files the operating
   system can support in a single directory.
5. The underlying file system must support hex encoded 256 bit file
   names.

## General Usage

EKV implements the following
[KeyValue](https://github.com/alexdupre/ekv-scala/blob/master/src/main/scala/com/alexdupre/ekv/KeyValue.scala) trait:


```scala
trait KeyValue {
  def update[T: Marshaler](key: String, value: T): Unit
  def apply[T: Unmarshaler](key: String): T
  def get[T: Unmarshaler](key: String): Option[T]
  def delete(key: String): Unit
}
```

EKV works with any object for which a
[Marshaler](https://github.com/alexdupre/ekv-scala/blob/master/src/main/scala/com/alexdupre/ekv/Marshaler.scala) and an
[Unmarshaler](https://github.com/alexdupre/ekv-scala/blob/master/src/main/scala/com/alexdupre/ekv/Unmarshaler.scala) are defined:

```scala
trait Marshaler[T] {
  def marshal(o: T): Array[Byte]
}

trait Unmarshaler[T] {
   def unmarshal(b: Array[Byte]): T
}
```

For example, we can make a marshalable String with:

```scala
  implicit def StringMarshaler = new Marshaler[String] with Unmarshaler[String] {
    override def marshal(o: String): Array[Byte]   = o.getBytes(StandardCharsets.UTF_8)
    override def unmarshal(b: Array[Byte]): String = new String(b, StandardCharsets.UTF_8)
  }
```

To load and store to the EKV with this implicit marshaler:

```scala
  val store = new FileStore("somedirectory", "Some Password")
  store("TestMe123") = "Hi"
  val str: String = store("TestMe123")
```

### Deleting Data

To delete, use `delete`, which will also remove the file corresponding
to the key:

```scala
  store.delete("SomeKey")
```

### Detecting if a key exists:

To detect if a key exists you can use the `get` function that
returns an `Option`:

```scala
  val strOpt: Option[String] = store.get("TestMe123")
```

# Cryptographic Primitives

All cryptographic code is located in [Crypto.scala](https://github.com/alexdupre/ekv-scala/blob/master/src/main/scala/com/alexdupre/ekv/Crypto.scala).

To create keys, EKV uses the construct:

* `H(H(password)||H(keyname))`

The `keyname` is the name of the key and `password` is the password or
passphrase used to generate the key. EKV uses the 256bit blake2b hash.

Code:


```scala
  def hashStringWithPassword(data: String, password: String): Array[Byte] = {
    val dHash = blake2bSum256(data.getBytes(StandardCharsets.UTF_8))
    val pHash = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
    blake2bSum256(Array(pHash, dHash))
  }
```


To encrypt files, EKV uses XChaCha20Poly1305 with a randomly generated
nonce. This project probably contains the first scala implementations of
HChaCha20 and XChaCha20Poly1305. The cryptographically secure pseudo-random
number generator can be provided by the user:


```scala
  def encrypt(data: Array[Byte], password: String, rng: Random): Array[Byte] = {
    val pwHash    = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
    val chaCipher = new XChaCha20Poly1305
    val nonce     = new Array[Byte](chaCipher.nonceSize)
    rng.nextBytes(nonce)
    val params = new AEADParameters(new KeyParameter(pwHash), 128, nonce)
    chaCipher.init(true, params)
    val out = new Array[Byte](nonce.length + chaCipher.getOutputSize(data.length))
    System.arraycopy(nonce, 0, out, 0, nonce.length)
    val outOff = chaCipher.processBytes(data, 0, data.length, out, nonce.length)
    chaCipher.doFinal(out, nonce.length + outOff)
    out
  }

def decrypt(data: Array[Byte], password: String): Array[Byte] = {
   val pwHash              = blake2bSum256(password.getBytes(StandardCharsets.UTF_8))
   val chaCipher           = new XChaCha20Poly1305
   val (nonce, ciphertext) = data.splitAt(chaCipher.nonceSize)
   val params              = new AEADParameters(new KeyParameter(pwHash), 128, nonce)
   chaCipher.init(false, params)
   val out    = new Array[Byte](chaCipher.getOutputSize(ciphertext.length))
   val outOff = chaCipher.processBytes(ciphertext, 0, ciphertext.length, out, 0)
   chaCipher.doFinal(out, outOff)
   out
}
```
