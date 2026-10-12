# Encryption & Cryptography Basics

A general-purpose primer on the cryptography concepts that underpin secure
systems: encryption/decryption, symmetric vs asymmetric keys, how asymmetric
cryptography works, and digital signatures. These fundamentals apply broadly
(TLS, password storage, signing) — see [`JWT-OVERVIEW.md`](./JWT-OVERVIEW.md)
for how they're used specifically in JSON Web Tokens.

---

## Three goals (vocabulary used throughout)

Everything below serves one or more of these three security goals. Keep them
straight — they are distinct, and different mechanisms provide different ones:

- **Confidentiality** — the message can't be *read* by anyone but the intended
  recipient. (Provided by **encryption**.)
- **Integrity** — the message *wasn't altered* since it was created. (Provided
  by **hashing / signatures**.)
- **Authenticity** — proof of *who* produced the message. (Provided by
  **signatures / MACs**.)

Mnemonic: confidentiality = *hidden*, integrity = *unchanged*, authenticity =
*who*. Note that encryption gives confidentiality but **not** authenticity, and a
signature gives integrity + authenticity but **not** confidentiality — which is
why the two are often combined (see below).

---

## Encryption / decryption

**Encryption** transforms readable data (*plaintext*) into unreadable data
(*ciphertext*) using an algorithm and a key. **Decryption** reverses it, turning
ciphertext back into plaintext with the appropriate key. The goal is
**confidentiality**: someone who intercepts the ciphertext can't read it without
the key.

```
plaintext --[ encrypt with key ]--> ciphertext --[ decrypt with key ]--> plaintext
```

A key is just a secret value the algorithm uses. The security comes from keeping
the right key secret, not from hiding the algorithm (the algorithms are public
and well studied).

> Confidentiality vs integrity: encryption is about **confidentiality** (hiding
> content). This is different from **signing**, which is about **integrity and
> authenticity** (proving content wasn't changed and who produced it), not
> hiding it. See digital signatures below.

---

## Symmetric vs asymmetric

These are the two fundamental models for how keys are used.

**Symmetric** — one shared secret key does everything. The same key both
encrypts and decrypts (or both signs and verifies).

```
          shared secret
         /             \
   encrypt             decrypt
```

- Fast and simple.
- The challenge is **key distribution**: everyone who needs to participate must
  hold the same secret, and anyone with it has full power. Sharing a secret
  across parties or networks safely is hard.
- Examples: AES (encryption), HMAC-SHA256 (signing).

**Asymmetric** — a mathematically linked **key pair**: a **public key** and a
**private key**. The private key is kept secret; the public key can be shared
with anyone. The two keys play complementary roles, used in two directions for
two different purposes:

- **Encryption (for confidentiality):** encrypt with the **public key**, decrypt
  with the **private key**. A key cannot reverse its *own* work — the public key
  encrypts but cannot decrypt what it encrypted; only the matching private key
  can.
- **Signing (for authenticity):** the private key **signs**, the public key
  **verifies**. This is a *different operation*, not "encrypting with the private
  key." (RSA's raw math happens to allow a reverse-direction transform, which is
  where the confusing "encrypt with the private key" phrasing comes from — but
  for ECDSA/EdDSA there is no encryption involved at all. Think sign/verify, not
  encrypt/decrypt.)

So encryption only ever goes **public → private**. The reverse direction isn't
encryption; it's signing.

```
   public key  <--- freely shared
   private key <--- kept secret by the owner
```

- Solves key distribution: you can hand out the public key openly without
  weakening security, because it can't be used to derive the private key.
- Slower and more computationally expensive than symmetric.
- Examples: RSA, ECDSA, EdDSA.

| | Symmetric | Asymmetric |
| --- | --- | --- |
| Keys | one shared secret | public + private pair |
| Key distribution | hard (must share the secret) | easy (publish the public key) |
| Speed | fast | slower |
| Who can do the "secret" operation | anyone with the key | only the private-key holder |

---

## How asymmetric works

The key idea is a **one-way mathematical relationship**: the key pair is
generated together so that whatever one key encrypts, **only the other key can
decrypt** — the same key cannot both encrypt and then decrypt. And you cannot
feasibly compute the private key from the public key, so the public key is safe
to share.

This enables two distinct uses, depending on which key does which job:

**1. Encryption for confidentiality (encrypt with public, decrypt with private)**

```
Anyone --[ encrypt with Bob's PUBLIC key ]--> ciphertext --[ decrypt with Bob's PRIVATE key ]--> Bob reads it
```

Anyone can encrypt a message to Bob using his public key, but only Bob (holding
the private key) can decrypt it. This provides confidentiality.

**2. Signing for authenticity (sign with private, verify with public)**

```
Owner --[ sign with PRIVATE key ]--> signature --[ verify with PUBLIC key ]--> anyone can confirm it
```

The owner signs with their private key; anyone can verify with the public key.
This proves the message came from the private-key holder and wasn't altered.

The crucial asymmetry: because only the private-key holder can produce a valid
signature, but *everyone* can verify it, you get **"one signer, many verifiers"**
without ever sharing the signing secret.

---

## What is a digital signature

A **digital signature** is a value attached to a message that proves two things:

- **Integrity** — the message hasn't been modified since it was signed.
- **Authenticity** — it was produced by the holder of a specific (private) key.

How it's produced and checked (asymmetric case):

```
Signing (private key holder):
  1. hash the message                → digest (a fixed-size fingerprint)
  2. run the SIGN operation over the digest with the PRIVATE key → signature
  3. send: message + signature

Verifying (anyone with the public key):
  1. hash the received message       → digest
  2. run the VERIFY operation with the PUBLIC key, the digest, and the signature
  3. VERIFY returns valid / invalid
```

If even one byte of the message changed, the recomputed hash won't match what
the signature covers, and verification fails. Someone who lacks the private key
cannot produce a signature that verifies against the public key.

> **"Signing = encrypting with the private key" is a simplification.** It's a
> handy mental model for *textbook RSA*, where the math happens to look like a
> reversed encryption. But it is not true in general: **sign** and **verify**
> are their own operations. ECDSA and EdDSA (JWT's `ES256` / `EdDSA`) involve no
> encryption at all — there is nothing to "decrypt" in the signature. Think of
> sign/verify as distinct primitives, not as encryption with the keys swapped.

> A digital signature does **not** hide the message — the message travels in the
> clear alongside the signature. It only proves integrity and authenticity.
> Symmetric signing works similarly but uses one shared key for both producing
> and checking the signature (e.g. HMAC). The tradeoff is the same: anyone who
> can verify can also sign.

### How verification actually works (and what's in a signature)

If the public key can't decrypt, how does "verify" work, and what is a signature
actually made of? The honest answer is **algorithm-specific**, but there's one
unifying idea:

> A signature is a value that is **hard to produce without the private key** but
> **easy to check with the public key.** "Checking" is a mathematical
> verification that binds *(message hash + signature + public key)* together and
> returns true/false. It is **not** necessarily decryption.

```
verify(message, signature, public_key) → valid / invalid
```

**What a signature contains** — not "the encrypted hash" in general; it's a
number (or pair) only the private-key holder could have computed for this
specific hash:

- **RSA:** a single large integer — the private-key math applied to a padded
  encoding of the message hash.
- **ECDSA:** a **pair** of numbers `(r, s)` derived from the hash, the private
  key, and a per-signature random value. The hash is **not** stored inside and
  cannot be extracted.

**How each verifies:**

- **RSA** — the math *is* roughly reversible, so verification applies the
  **public-key** operation to the signature to recover the hash encoding, then
  compares it to a fresh hash of the message. This is the one case where "the
  public key recovers the hash" is fair — and why RSA gets taught as "decrypt
  with the public key." (Even so, it's recovering a hash anyone could compute,
  not revealing a secret.)
- **ECDSA / EdDSA** — there is **no reversal and nothing to decrypt**.
  Verification plugs the message hash, the signature `(r, s)`, and the public key
  into the verification equation (elliptic-curve point math). If the equation
  balances, the signature is valid. You cannot pull the hash out of the
  signature — it was never in there.

So "the public key decrypts the signature" is only a (loose) description of RSA.
The general operation is a one-way **check**, not a decryption.

---

## Combining confidentiality + authenticity

Confidentiality and authenticity are **separate goals** that use the key pairs
in opposite directions — and typically use **two different key pairs** (the
recipient's and the sender's). To send a message that is both private and
provably from you:

- **Encrypt** the message with the **recipient's PUBLIC key** → only the
  recipient (holding their private key) can read it. *(confidentiality)*
- **Sign** the message with **your OWN PRIVATE key** → the recipient verifies
  with your public key that it's really from you and unaltered. *(authenticity)*

```
Sender                                              Recipient
------                                              ---------
sign    with  SENDER's   PRIVATE key  ──► signature ──► verify  with SENDER's   PUBLIC key
encrypt with  RECIPIENT's PUBLIC key  ──► ciphertext ─► decrypt with RECIPIENT's PRIVATE key
```

| Goal | Operation | Key used | Who performs it |
| --- | --- | --- | --- |
| Confidentiality | encrypt | recipient's **public** | sender |
| | decrypt | recipient's **private** | recipient |
| Authenticity | sign | sender's **private** | sender |
| | verify | sender's **public** | recipient |

Each party ends up using **both** halves of a pair — just for different roles
(you sign with your private key; you decrypt with your private key; you encrypt
with their public key; you verify with their public key).

> **How this maps to the real world — two important caveats:**
>
> - **JWTs are signed, not encrypted, and the *server* signs.** The server signs
>   the token with its private key; clients and other services verify with the
>   public key. The payload is only Base64-encoded (readable by anyone), there is
>   no "encrypt with the recipient's public key" step, and the client does **not**
>   sign — it just carries the token. So the pattern above is *not* the JWT model.
> - **Real systems rarely encrypt messages directly with a public key.** See the
>   next section — confidentiality on the wire comes from a *hybrid* approach,
>   not per-message public-key encryption.

---

## Confidentiality in practice: hybrid cryptosystems

In theory, to send confidential data you encrypt with the **recipient's public
key** (so the server replying to a client would encrypt with the *client's*
public key). In practice this is almost never done directly, for two reasons:

- **Clients usually have no key pair.** In a typical web/API setup only the
  **server** has a certificate (key pair); a browser, `curl`, or mobile app
  generally does not. So "encrypt with the client's public key" often has no key
  to use.
- **Asymmetric encryption is slow and size-limited** — impractical for real
  payloads (RSA can only encrypt data smaller than its key).

The real solution is a **hybrid cryptosystem** (this is what TLS/HTTPS does):

```
1. Handshake (once): use ASYMMETRIC crypto to securely agree on a shared
   SYMMETRIC session key. Only the SERVER needs a key pair.
2. Data (bidirectional): use the fast SYMMETRIC key to encrypt all traffic in
   BOTH directions (client→server and server→client).
```

> **In short: if you want bidirectional confidentiality, use asymmetric to
> establish a symmetric key, then symmetric for the data.** You get asymmetric's
> safe key distribution plus symmetric's speed, and the client needs no key pair.

> Modern-TLS precision: TLS no longer "encrypts the symmetric key with the
> server's public key" (that was older RSA key exchange). It uses
> **Diffie-Hellman** key exchange, where both sides *derive* a shared secret
> without transmitting it, and the server's key pair is used to **authenticate**
> (sign) the handshake — proving you're talking to the real server. The overall
> shape is unchanged: asymmetric establishes the symmetric key, symmetric
> protects the bidirectional data.

---

## References

- [Public-key cryptography (Wikipedia)](https://en.wikipedia.org/wiki/Public-key_cryptography)
- [Digital signature (Wikipedia)](https://en.wikipedia.org/wiki/Digital_signature)
- See also [`JWT-OVERVIEW.md`](./JWT-OVERVIEW.md) for how these apply to tokens.

> Note: External links are provided for authoritative detail. Summaries here are
> paraphrased for quick reference.
