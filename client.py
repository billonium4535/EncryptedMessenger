import socket
import threading

from Config.config_reader import config_parser
from Encryption.encryption_utils import derive_room_key, encrypt, decrypt


SERVER_IP = config_parser("./Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("./Config/client_config.ini", "DEFAULT", "PORT")
MESSAGE_PREFIX = config_parser("./Config/client_config.ini", "DEFAULT", "MESSAGE_PREFIX").encode("utf-8")


class Client:
    def __init__(self, server_ip: str, server_port: int, message_callback):
        self.server_ip = server_ip
        self.server_port = server_port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.key = None
        self.name = None
        self.room = None
        self.buffer = bytearray()
        self.message_callback = message_callback

    def send_line(self, prefix: bytes, payload_b64: bytes):
        self.socket.sendall(prefix + payload_b64 + b"\n")

    def receiver(self):
        while True:
            try:
                chunk = self.socket.recv(4096)
                if not chunk:
                    break
                self.buffer.extend(chunk)

                while True:
                    idx = self.buffer.find(b"\n")
                    if idx < 0:
                        break
                    line = bytes(self.buffer[:idx])
                    del self.buffer[:idx + 1]

                    if line.startswith(MESSAGE_PREFIX):
                        payload_b64 = line[len(MESSAGE_PREFIX):]
                        if not self.key:
                            continue
                        try:
                            plaintext = decrypt(self.key, payload_b64).decode("utf-8", errors="replace")
                            self.message_callback(plaintext)
                        except Exception:
                            self.message_callback("[!] failed to decrypt a message")
            except Exception:
                self.message_callback("[!] Disconnected from server")
                try:
                    self.socket.close()
                finally:
                    break

    def connect(self, name, room, passphrase):
        self.name = name
        self.room = room
        self.key = derive_room_key(room, passphrase)
        self.socket.connect((self.server_ip, self.server_port))
        threading.Thread(target=self.receiver, daemon=True).start()

    def send_message(self, message: str):
        plaintext = f"{self.name}: {message}".encode("utf-8")
        b64_payload = encrypt(self.key, plaintext)
        self.send_line(MESSAGE_PREFIX, b64_payload)

    def disconnect(self):
        try:
            self.socket.close()
        except Exception:
            pass
