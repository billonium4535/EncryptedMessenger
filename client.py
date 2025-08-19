import socket
import threading

from Config.config_reader import config_parser
from Encryption.encryption_utils import derive_room_key, encrypt, decrypt


SERVER_IP = config_parser("./Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("./Config/client_config.ini", "DEFAULT", "PORT")
MESSAGE_PREFIX = config_parser("./Config/client_config.ini", "DEFAULT", "MESSAGE_PREFIX").encode("utf-8")


class Client:
    def __init__(self, server_ip: str, server_port: int):
        self.server_ip = server_ip
        self.server_port = server_port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.key = None
        self.name = None
        self.room = None
        self.buffer = bytearray()

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
                            sys.stdout.write("\r" + plaintext + "\n> ")
                            sys.stdout.flush()
                        except Exception:
                            sys.stdout.write("\r[!] failed to decrypt a message\n> ")
                            sys.stdout.flush()
                    else:
                        pass
            except Exception:
                sys.stdout.write("\n[!] Disconnected from server\n")
                sys.stdout.flush()
                try:
                    self.socket.close()
                finally:
                    break

    def run(self):
        self.name = input("Enter name >")
        self.room = input("Enter room >")
        passphrase = input("Enter password >")
        self.key = derive_room_key(self.room, passphrase)

        self.socket.connect((self.server_ip, self.server_port))
        threading.Thread(target=self.receiver, daemon=True).start()

        while True:
            message = input("> ")
            if message.lower() == "/quit":
                self.socket.close()
                break

            plaintext = f"{self.name}: {message}".encode("utf-8")
            b64_payload = encrypt(self.key, plaintext)
            self.send_line(MESSAGE_PREFIX, b64_payload)

            sys.stdout.flush()


def main():
    client = Client(SERVER_IP, int(SERVER_PORT))
    client.run()


main()
