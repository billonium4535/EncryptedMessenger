import os
import base64
import hashlib
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

from Config.config_reader import config_parser

ADDITIONAL_AUTHENTICATED_DATA = config_parser("../Config/client_config.ini", "DEFAULT", "ADDITIONAL_AUTHENTICATED_DATA").encode("utf-8")



def derive_room_key(room: str, passphrase: str) -> bytes:
    salt = hashlib.sha256(room.encode("utf-8")).digest()[:16]
    kdf = Scrypt(salt=salt, length=32, n=2 ** 14, r=8, p=1)
    return kdf.derive(passphrase.encode("utf-8"))


def encrypt(key: bytes, plaintext: bytes) -> bytes:
    aead = ChaCha20Poly1305(key)
    nonce = os.urandom(12)

    # Encrypt the plaintext
    ct = aead.encrypt(nonce, plaintext, ADDITIONAL_AUTHENTICATED_DATA)

    # Merge nonce and cyphertext, then encode and return
    return base64.b64encode(nonce + ct)


def decrypt(key: bytes, payload_b64: bytes) -> bytes:
    raw = base64.b64decode(payload_b64)
    nonce, ct = raw[:12], raw[12:]
    aead = ChaCha20Poly1305(key)

    # Decrypt and return the data
    return aead.decrypt(nonce, ct, ADDITIONAL_AUTHENTICATED_DATA)
