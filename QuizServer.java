import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.KeyStore;

// -------- CLIENT HANDLER --------
class ClientHandler extends Thread {
    SSLSocket socket;
    BufferedReader in;
    PrintWriter out;
    String name;
    int score = 0;
    String lastAnswer = "";
    long answerTime = Long.MAX_VALUE;

    public ClientHandler(SSLSocket socket) throws Exception {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        String msg = in.readLine();
        name = msg.split("\\|")[1];
        System.out.println(name + " connected");
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void run() {
        try {
            while (true) {
                String input = in.readLine();
                if (input == null) break;

                if (input.startsWith("ANSWER")) {
                    lastAnswer = input.split("\\|")[1];
                    answerTime = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {}
    }
}

// -------- QUESTION CLASS --------
class Question {
    String question;
    String[] options;
    String correct;

    public Question(String q, String[] opt, String c) {
        question = q;
        options = opt;
        correct = c;
    }
}

// -------- MAIN SERVER --------
public class QuizServer {

    static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        int port = 5000;
        String keystoreFile = "serverkeystore.jks";
        String keystorePassword = "123456"; // 👈 YOUR PASSWORD

        // Load keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());

        // Key manager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keystorePassword.toCharArray());

        // SSL context
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(kmf.getKeyManagers(), null, null);

        // SSL server socket
        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket) ssf.createServerSocket(port);

        System.out.println("🔐 Secure Server started on port " + port);

        // Accept 2 clients
        while (clients.size() < 2) {
            SSLSocket socket = (SSLSocket) server.accept();
            ClientHandler ch = new ClientHandler(socket);
            clients.add(ch);
            ch.start();
        }

        runQuiz();
    }

    // -------- QUIZ LOGIC --------
    static void runQuiz() throws Exception {

        List<Question> quiz = new ArrayList<>();

        quiz.add(new Question("What is 2+2?",
                new String[]{"A) 3", "B) 4", "C) 5", "D) 6"}, "B"));

        quiz.add(new Question("Capital of India?",
                new String[]{"A) Mumbai", "B) Delhi", "C) Chennai", "D) Kolkata"}, "B"));

        quiz.add(new Question("Which is a programming language?",
                new String[]{"A) HTTP", "B) HTML", "C) Python", "D) URL"}, "C"));

        quiz.add(new Question("5 * 6 = ?",
                new String[]{"A) 30", "B) 25", "C) 20", "D) 35"}, "A"));

        quiz.add(new Question("Binary of 2?",
                new String[]{"A) 10", "B) 01", "C) 11", "D) 00"}, "A"));

        int timeLimit = 10;

        for (Question q : quiz) {

            for (ClientHandler c : clients) {
                StringBuilder msg = new StringBuilder();
                msg.append("QUESTION|").append(q.question).append("|");

                for (String opt : q.options) {
                    msg.append(opt).append(";");
                }

                msg.append("|").append(timeLimit);

                c.send(msg.toString());
                c.lastAnswer = "";
                c.answerTime = Long.MAX_VALUE;
            }

            long start = System.currentTimeMillis();

            // Wait until all answered OR timeout
            while (System.currentTimeMillis() - start < timeLimit * 1000) {
                boolean allAnswered = true;

                for (ClientHandler c : clients) {
                    if (c.lastAnswer.equals("")) {
                        allAnswered = false;
                        break;
                    }
                }

                if (allAnswered) break;
                Thread.sleep(100);
            }

            for (ClientHandler c : clients) {
                if (c.lastAnswer.equalsIgnoreCase(q.correct)) {
                    int points = (int)(1000 - (c.answerTime - start));
                    c.score += Math.max(points, 200);
                    c.send("RESULT|Correct|Score:" + c.score);
                } else {
                    c.send("RESULT|Wrong|Score:" + c.score);
                }
            }

            sendLeaderboard();
        }

        clients.sort((a, b) -> b.score - a.score);
        String winner = clients.get(0).name;

        for (ClientHandler c : clients) {
            c.send("WINNER|" + winner);
            c.send("END");
        }
    }

    // -------- LEADERBOARD --------
    static void sendLeaderboard() {
        clients.sort((a, b) -> b.score - a.score);

        StringBuilder lb = new StringBuilder("LEADERBOARD|");

        for (ClientHandler c : clients) {
            lb.append(c.name).append(":").append(c.score).append(",");
        }

        for (ClientHandler c : clients) {
            c.send(lb.toString());
        }
    }
}