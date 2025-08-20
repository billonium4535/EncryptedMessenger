import socket
import threading
import time

from Config.config_reader import config_parser
from encryption_utils import derive_room_key, encrypt, decrypt


SERVER_IP = config_parser("./Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("./Config/client_config.ini", "DEFAULT", "PORT")
MESSAGE_PREFIX = config_parser("./Config/client_config.ini", "DEFAULT", "MESSAGE_PREFIX").encode("utf-8")


class Client:
    def __init__(self, server_ip: str, server_port: int, message_callback, status_callback=None):
        self.server_ip = server_ip
        self.server_port = server_port
        self.socket = None
        self.key = None
        self.name = None
        self.room = None
        self.passphrase = None
        self.buffer = bytearray()
        self.message_callback = message_callback
        self.status_callback = status_callback
        self._stop_reconnect = False

    def send_line(self, prefix: bytes, payload_b64: bytes):
        try:
            self.socket.sendall(prefix + payload_b64 + b"\n")
        except Exception:
            self.message_callback("[!] Failed to send message")

    def receiver(self):
        while not self._stop_reconnect:
            try:
                chunk = self.socket.recv(4096)
                if not chunk:
                    raise ConnectionError("Disconnected")
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
                            pass
            except Exception:
                if self.status_callback:
                    self.status_callback("reconnecting")
                self._start_reconnect_loop()
                break

    def connect(self, name, room, passphrase):
        self.name = name
        self.room = room
        self.passphrase = passphrase
        self.key = derive_room_key(room, passphrase)
        self._stop_reconnect = False
        self._start_reconnect_loop()

    def _start_reconnect_loop(self):
        def loop():
            while not self._stop_reconnect:
                try:
                    self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    self.socket.connect((self.server_ip, self.server_port))
                    # self.key = derive_room_key(self.room, self.passphrase)
                    threading.Thread(target=self.receiver, daemon=True).start()
                    if self.status_callback:
                        self.status_callback("connected")
                    break
                except Exception:
                    if self.status_callback:
                        self.status_callback("reconnecting")
                    self.status_callback("disconnected")
                    time.sleep(5)

        threading.Thread(target=loop, daemon=True).start()

    def send_message(self, message: str):
        plaintext = f"{self.name}: {message}".encode("utf-8")
        b64_payload = encrypt(self.key, plaintext)
        self.send_line(MESSAGE_PREFIX, b64_payload)

    def disconnect(self):
        self._stop_reconnect = True
        try:
            if self.socket:
                self.socket.shutdown(socket.SHUT_RDWR)
                self.socket.close()
        except Exception:
            pass
