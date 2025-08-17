import os
import base64
import hashlib
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

from Config.config_reader import config_parser

# PROTOCOL = config_parser("../Config/client_config.ini", "DEFAULT", "ENCRYPTION_PROTOCOL").encode("utf-8")
PROTOCOL = b"chat-group-e2e-v1"


def derive_room_key(room: str, passphrase: str) -> bytes:
    salt = hashlib.sha256(room.encode("utf-8")).digest()[:16]
    kdf = Scrypt(salt=salt, length=32, n=2 ** 14, r=8, p=1)
    return kdf.derive(passphrase.encode("utf-8"))


def encrypt(key: bytes, plaintext: bytes) -> bytes:
    aead = ChaCha20Poly1305(key)
    nonce = os.urandom(12)
    ct = aead.encrypt(nonce, plaintext, PROTOCOL)
    return base64.b64encode(nonce + ct)


def decrypt(key: bytes, payload_b64: bytes) -> bytes:
    raw = base64.b64decode(payload_b64)
    nonce, ct = raw[:12], raw[12:]
    aead = ChaCha20Poly1305(key)
    return aead.decrypt(nonce, ct, PROTOCOL)
