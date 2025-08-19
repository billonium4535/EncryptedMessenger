import os
import base64
import hashlib
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

from Config.config_reader import config_parser

ADDITIONAL_AUTHENTICATED_DATA = config_parser("../Config/client_config.ini", "DEFAULT", "ADDITIONAL_AUTHENTICATED_DATA").encode("utf-8")


def derive_room_key(room: str, passkey: str) -> bytes:
    """
    Get a 32-byte encryption key for a room and passkey.

    Args:
        room (str): The room name (used for unique salt).
        passkey (bytes): The passkey secret.

    Returns:
        bytes: A 32-byte encryption key.
    """

    # Generate a salt by hashing the room name and taking the first 16 bytes
    salt = hashlib.sha256(room.encode("utf-8")).digest()[:16]

    # Initialise a script key function with the salt and parameters:
    # Salt, key length, CPU/Memory cost parameter (n), block size (r), parallelisation (p)
    kdf = Scrypt(salt=salt, length=32, n=2 ** 14, r=8, p=1)

    # Derive a 32-byte key from the passkey
    return kdf.derive(passkey.encode("utf-8"))


def encrypt(key: bytes, plaintext: bytes) -> bytes:
    """
    Encrypt a message using ChaCha20-Poly1305 AEAD.

    Args:
        key (bytes): A 32-byte symmetric key.
        plaintext (bytes): The data to encrypt.

    Returns:
        bytes: Base64-encoded data.
    """

    # Initialise ChaCha20-Poly1305 AEAD cipher with the key
    # Authenticated encryption with associated data
    aead = ChaCha20Poly1305(key)

    # Generate a random 12-byte nonce
    nonce = os.urandom(12)

    # Encrypt the plaintext
    ct = aead.encrypt(nonce, plaintext, ADDITIONAL_AUTHENTICATED_DATA)

    # Merge nonce and cyphertext, then encode and return
    return base64.b64encode(nonce + ct)


def decrypt(key: bytes, payload_b64: bytes) -> bytes:
    """
    Decrypt a message using ChaCha20-Poly1305 AEAD.

    Args:
        key (bytes): A 32-byte symmetric key.
        payload_b64 (bytes): The data to decrypt containing nonce + cyphertext.

    Returns:
        bytes: Decrypted data.
    """

    # Decode
    raw = base64.b64decode(payload_b64)

    # Split the first 12 bytes into nonce and the rest into data
    nonce, ct = raw[:12], raw[12:]

    # Initialise ChaCha20-Poly1305 AEAD cipher with the key
    # Authenticated encryption with associated data
    aead = ChaCha20Poly1305(key)

    # Decrypt and return the data
    return aead.decrypt(nonce, ct, ADDITIONAL_AUTHENTICATED_DATA)
