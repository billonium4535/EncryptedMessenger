import socket
import threading


# Server config
SERVER_IP = "0.0.0.0"
SERVER_PORT = 23194

# Global list of connected clients (sockets)
clients = []


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
            # Forward data to all other clients
            broadcast(data, connection)
        except:
            # Connection error or abrupt disconnect
            break
    print("Client {} disconnected".format(address))
    clients.remove(connection)
    connection.close()


def broadcast(data, connection):
    """
    Send data to all connected clients except the sender.

    Args:
        data (bytes): The data/message to broadcast.
        connection (socket.socket): The socket of the sender (excluded).
    """
    # Copy list to avoid modification issues
    for client in clients:
        try:
            client.send(data)
        except:
            # If sending fails, close and remove client
            client.close()
            clients.remove(client)


def main():
    """
    Start the TCP server and listen for incoming client connections.
    Spawns a new thread to handle each client.
    """
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((SERVER_IP, SERVER_PORT))
    server.listen(2)  # Change the number of unaccepted connections that the system will allow before refusing new connections (https://docs.python.org/3.13/library/socket.html#socket.socket.listen)
    print("Server listening on {}:{}".format(SERVER_IP, SERVER_PORT))

    while True:
        # Wait for client connection
        connection, address = server.accept()
        clients.append(connection)

        # Create a new thread for each client
        thread = threading.Thread(target=handle_client, args=(connection, address))
        thread.start()


main()
