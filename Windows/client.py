import socket
import threading
import time

from Config.config_reader import config_parser
from encryption_utils import derive_room_key, encrypt, decrypt


# Load configuration values from client_config.ini
SERVER_IP = config_parser("./Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("./Config/client_config.ini", "DEFAULT", "PORT")
MESSAGE_PREFIX = config_parser("./Config/client_config.ini", "DEFAULT", "MESSAGE_PREFIX").encode("utf-8")
SYSTEM_TAG = config_parser("./Config/client_config.ini", "DEFAULT", "SYSTEM_TAG")


class Client:
    """
    A TCP client for sending and receiving encrypted messages in a chat room.

    Attributes:
        server_ip (str): The IP address of the server.
        server_port (int): The port of the server.
        message_callback (callable): Function to call when a message is received.
        status_callback (callable): Function to call when connection status changes.
        socket (socket.socket): Active socket connection.
        key (bytes): Encryption key derived from room + passphrase.
        name (str): Name of the client/user.
        room (str): Chat room identifier.
        passphrase (str): Passphrase for deriving encryption key.
        buffer (bytearray): Buffer for incoming data before processing.
        _stop_reconnect (bool): Flag to stop reconnection attempts.
    """
    def __init__(self, server_ip: str, server_port: int, message_callback, status_callback=None):
        """Initialise the client with server details and callbacks."""
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
        self._first_connection = True
        self._first_connection_leave = True

    def send_line(self, prefix: bytes, payload_b64: bytes):
        """
        Send a line of data to the server.

        Args:
            prefix (bytes): Message prefix.
            payload_b64 (bytes): Payload base64 encoded bytes.
        """
        try:
            self.socket.sendall(prefix + payload_b64 + b"\n")
        except Exception:
            self.message_callback("[!] Failed to send message")

    def receiver(self):
        """
        Continuously receive and process messages from the server.
        Handles reconnection if disconnected.
        """
        while not self._stop_reconnect:
            try:
                chunk = self.socket.recv(4096)
                if not chunk:
                    raise ConnectionError("Disconnected")
                self.buffer.extend(chunk)

                # Process complete lines in the buffer
                while True:
                    idx = self.buffer.find(b"\n")
                    if idx < 0:
                        break
                    line = bytes(self.buffer[:idx])
                    del self.buffer[:idx + 1]

                    # Only process messages with the correct prefix
                    if line.startswith(MESSAGE_PREFIX):
                        payload_b64 = line[len(MESSAGE_PREFIX):]
                        if not self.key:
                            continue
                        try:
                            # Attempt to decrypt and decode message
                            plaintext = decrypt(self.key, payload_b64).decode("utf-8", errors="replace")

                            # if plaintext.startswith(SYSTEM_TAG):
                            #     system_message = plaintext.strip()
                            #     self.message_callback(f"*{system_message}*")
                            # else:
                            self.message_callback(plaintext.strip())
                        except Exception:
                            # Ignore decryption failures
                            pass
            except Exception:
                if self.status_callback:
                    self.status_callback("reconnecting")
                self._start_reconnect_loop()
                break

    def connect(self, name, room, passphrase):
        """
        Connect to the server and join a room.

        Args:
            name (str): Client username.
            room (str): Room name.
            passphrase (str): Passphrase for encryption key derivation.
        """
        self.name = name
        self.room = room
        self.passphrase = passphrase
        self.key = derive_room_key(room, passphrase)
        self._stop_reconnect = False
        self._start_reconnect_loop()

    def _start_reconnect_loop(self):
        """
        Attempt to connect (or reconnect) to the server in a loop.
        Runs in a background thread until stopped.
        """
        def loop():
            while not self._stop_reconnect:
                try:
                    self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    self.socket.connect((self.server_ip, self.server_port))
                    # Start the receiver thread once connected
                    threading.Thread(target=self.receiver, daemon=True).start()
                    if self.status_callback:
                        self.status_callback("connected")

                    if self._first_connection:
                        self.send_system_message(f"{self.name} has entered the chat room")
                        self._first_connection = False
                        self._first_connection_leave = True
                    # Exit loop if successfully connected
                    break
                except Exception:
                    if self.status_callback:
                        self.status_callback("reconnecting")
                    self.status_callback("disconnected")
                    time.sleep(5)

        threading.Thread(target=loop, daemon=True).start()

    def send_message(self, message: str):
        """
        Encrypt and send a message to the server.

        Args:
            message (str): Message to send.
        """
        plaintext = f"{self.name}: {message}".encode("utf-8")
        b64_payload = encrypt(self.key, plaintext)
        self.send_line(MESSAGE_PREFIX, b64_payload)

    def send_system_message(self, content: str):
        """
        Encrypt and send a system message.
        Args:
            content (str): Content to send.
        """
        plaintext = f"{SYSTEM_TAG}{content}".encode("utf-8")
        b64_payload = encrypt(self.key, plaintext)
        self.send_line(MESSAGE_PREFIX, b64_payload)

    def disconnect(self):
        """Disconnect from the server and stop reconnection attempts."""
        self._stop_reconnect = True
        try:
            if self._first_connection_leave:
                self.send_system_message(f"{self.name} has left the chat room")
                time.sleep(0.1)
        except Exception:
            pass
        finally:
            try:
                if self.socket:
                    self.socket.shutdown(socket.SHUT_RDWR)
                    self.socket.close()
            except Exception:
                # Ignore errors during shutdown
                pass
