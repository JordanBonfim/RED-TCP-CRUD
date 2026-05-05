import socket

def create_client(host='localhost', port=5000):
    """Create and connect a TCP client"""
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((host, port))
    return client

def send_message(client, message):
    """Send a message to the server"""
    client.sendall(message.encode())

def receive_message(client, buffer_size=1024):
    """Receive a message from the server"""
    data = client.recv(buffer_size)
    return data.decode()

def close_client(client):
    """Close the client connection"""
    client.close()

if __name__ == "__main__":
    try:
        client = create_client()
        print("Connected to server")
        
        while True:
            message = input("Enter message (or 'quit' to exit): ")
            if message.lower() == 'quit':
                break
            
            send_message(client, message)
            response = receive_message(client)
            print(f"Response: {response}")
        
        close_client(client)
        print("Disconnected from server")
    except ConnectionRefusedError:
        print("Could not connect to server")
    except Exception as e:
        print(f"Error: {e}")
