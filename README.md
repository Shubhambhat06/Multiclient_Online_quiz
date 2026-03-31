# 🔐 Secure Quiz System

A real-time multiplayer quiz application using a **Java server** and **Python client**, secured with **SSL/TLS encryption**.

---

## 🚀 Features

* Multiplayer quiz (2 players)
* Timed questions & scoring
* Live leaderboard
* Secure communication using TLS

---

## 🧱 Tech Stack

* Java (SSLServerSocket)
* Python (ssl module)
* TCP sockets over TLS

---

## 🔐 Setup

### Generate Certificate

```
keytool -genkeypair -alias quizserver -keyalg RSA -keystore serverkeystore.jks -validity 365
keytool -export -alias quizserver -keystore serverkeystore.jks -file servercert.cer
```

### Run Server

```
javac QuizServer.java
java QuizServer
```

### Run Client

```
python client.py
```

---

## 🧠 Concept

Secure client-server communication using **TLS with a self-signed certificate**.

---

## 👨‍💻 Author

Shubham Bhat
