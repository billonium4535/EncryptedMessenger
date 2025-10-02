import socket
import threading


SERVER_IP = "0.0.0.0"
SERVER_PORT = 23194

clients = []


def handle_client(connection, address):
    print("Client {} connected".format(address))
    while True:
        try:
            data = connection.recv(1024)
            if not data:
                break
            broadcast(data, connection)
            # print(f"Client {address} sent {data}")
        except:
            break
    print("Client {} disconnected".format(address))
    clients.remove(connection)
    connection.close()


def broadcast(data, connection):
    for client in clients:
        try:
            client.send(data)
        except:
            client.close()
            clients.remove(client)


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((SERVER_IP, SERVER_PORT))
    server.listen(2)  # Change the number of unaccepted connections that the system will allow before refusing new connections (https://docs.python.org/3.13/library/socket.html#socket.socket.listen)
    print("Server listening on {}:{}".format(SERVER_IP, SERVER_PORT))

    while True:
        connection, address = server.accept()
        clients.append(connection)
        thread = threading.Thread(target=handle_client, args=(connection, address))
        thread.start()


main()
