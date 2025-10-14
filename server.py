import socket
import threading
import json
import time


# Server config
SERVER_IP = "0.0.0.0"
SERVER_PORT = 23194

# Global list of connected clients (sockets)
clients = []

# Maps sockets
client_info = {}

HEARTBEAT_PREFIX = b"__HEARTBEAT__"


def handle_client(connection, address):
    """
    Handle communication with a connected client.

    Args:
        connection (socket.socket): The client's socket connection.
        address (tuple): The (IP, port) tuple of the client.
    """
    print("Client {} connected".format(address))
    while True:
        try:
            # Receive data from client
            data = connection.recv(1024)
            # Empty data means the client disconnected
            if not data:
                break

            # Heartbeat message
            if data.startswith(HEARTBEAT_PREFIX):
                try:
                    payload = json.loads(data[len(HEARTBEAT_PREFIX):].decode())
                    room = payload.get("room")
                    password = payload.get("password")
                    client_info[connection] = {"room": room, "password": password, "last_seen": time.time()}

                    # Count how many clients share same room+password
                    count = sum(
                        1 for info in client_info.values()
                        if info["room"] == room and info["password"] == password
                    )

                    # Send back the count
                    connection.send(f"__COUNT__{count}\n".encode("utf-8"))
                except Exception as e:
                    print(e)
                continue

            # Forward data to all other clients
            broadcast(data, connection)
        except Exception as e:
            # Connection error or abrupt disconnect
            print(e)
            break

    print(f"Client {address} disconnected")
    clients.remove(connection)
    client_info.pop(connection, None)
    connection.close()


def broadcast(data, connection):
    """
    Send data to all connected clients except the sender.

    Args:
        data (bytes): The data/message to broadcast.
        connection (socket.socket): The socket of the sender (excluded).
    """
    # Copy list to avoid modification issues
    for client in clients.copy():
        try:
            client.send(data)
        except:
            # If sending fails, close and remove client
            client.close()
            clients.remove(client)
            client_info.pop(client, None)


def cleanup_inactive():
    while True:
        now = time.time()
        inactive = [c for c, info in client_info.items() if now - info["last_seen"] > 15]
        for c in inactive:
            print("Removing inactive client", c)
            client_info.pop(c, None)
            if c in clients:
                clients.remove(c)
                c.close()
        time.sleep(5)


def main():
    """
    Start the TCP server and listen for incoming client connections.
    Spawns a new thread to handle each client.
    """
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((SERVER_IP, SERVER_PORT))
    server.listen(2)  # Change the number of unaccepted connections that the system will allow before refusing new connections (https://docs.python.org/3.13/library/socket.html#socket.socket.listen)
    print(f"Server listening on {SERVER_IP}:{SERVER_PORT}")

    while True:
        # Wait for client connection
        connection, address = server.accept()
        clients.append(connection)

        # Create a new thread for each client
        thread = threading.Thread(target=handle_client, args=(connection, address))
        thread.start()


main()
