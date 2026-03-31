import socket
import ssl
import threading

SERVER_IP = "172.20.10.3"   # your server IP
PORT = 5000

# ---------------- SSL SETUP ----------------
context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH)

# Load server certificate (exported from keytool)
context.load_verify_locations("servercert.cer")

# For local testing (important if CN=localhost)
context.check_hostname = False


# ---------------- LISTENER ----------------
def listen(sock):
    buffer = ""

    while True:
        try:
            data = sock.recv(1024).decode()
            if not data:
                break

            buffer += data

            while "\n" in buffer:
                msg, buffer = buffer.split("\n", 1)
                msg = msg.strip()

                if not msg:
                    continue

                # -------- QUESTION --------
                if msg.startswith("QUESTION"):
                    parts = msg.split("|")

                    question = parts[1]
                    options = parts[2].split(";")

                    print("\n" + question)
                    for opt in options:
                        if opt:
                            print(opt)

                    ans = input("Enter option (A/B/C/D): ").strip().upper()
                    sock.send(f"ANSWER|{ans}\n".encode())

                # -------- RESULT --------
                elif msg.startswith("RESULT"):
                    print("\n" + msg)

                # -------- LEADERBOARD --------
                elif msg.startswith("LEADERBOARD"):
                    print("\nLeaderboard:")
                    scores = msg.split("|")[1].split(",")

                    for s in scores:
                        if s:
                            print(" -", s)

                # -------- WINNER --------
                elif msg.startswith("WINNER"):
                    print("\n🏆 Winner:", msg.split("|")[1])

                # -------- END --------
                elif msg.startswith("END"):
                    print("\nQuiz Finished!")
                    return

        except Exception as e:
            print("Connection error:", e)
            break


# ---------------- MAIN ----------------
try:
    # Create raw socket
    raw_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Wrap with SSL
    client = context.wrap_socket(raw_socket, server_hostname=SERVER_IP)

    # Connect
    client.connect((SERVER_IP, PORT))

    print("🔐 Secure connection established ✅")

except Exception as e:
    print("Connection failed ❌:", e)
    exit()


# Send name
name = input("Enter name: ")
client.send(f"NAME|{name}\n".encode())

# Start listener thread
threading.Thread(target=listen, args=(client,), daemon=True).start()

# Keep alive
while True:
    pass