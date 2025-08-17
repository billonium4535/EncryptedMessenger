import socket
import threading
import sys

from Config.config_reader import config_parser
from Encryption.encryption_utils import derive_room_key, encrypt, decrypt


SERVER_IP = config_parser("./Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("./Config/client_config.ini", "DEFAULT", "PORT")

MESSAGE_PREFIX = b"MSG >"



def receive_message(sock, name):
    while True:
        try:
            message = sock.recv(1024).decode()
            if not message:
                break
            sys.stdout.write(f"\r{message}\n{name}> ")
            sys.stdout.flush()
            # print(f"\n{message}\n> ", end="")
        except:
            print("\n[!] Disconnected from server")
            sock.close()
            break


def main():
    name = input("Enter your name >")
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((SERVER_IP, SERVER_PORT))

    threading.Thread(target=receive_message, args=(client, name,), daemon=True).start()

    while True:
        message = input(f"{name}> ")
        if message.lower() == "/quit":
            client.close()
            break
        full_message = f"{name}> {message}"
        client.send(full_message.encode())


main()
