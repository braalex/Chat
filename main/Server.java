package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        for (String name : connectionMap.keySet()) {
            try {
                connectionMap.get(name).send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Message not sent to user " + name);
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Enter server port:");
        int port = ConsoleHelper.readInt();
        try (ServerSocket server = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Server started!");
            while (true) {
                Socket client = server.accept();
                new Handler(client).start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Error!");
//            server.close();
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message reply = connection.receive();
                if (reply.getType() == MessageType.USER_NAME) {
                    String userName = reply.getData();
                    if (userName != null && !userName.isEmpty() && !connectionMap.containsKey(userName)) {
                        connectionMap.put(userName, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        return userName;
                    }
                }
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String user : connectionMap.keySet()) {
                if (!user.equals(userName)) connection.send(new Message(MessageType.USER_ADDED, user));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message != null && message.getType() == MessageType.TEXT) {
                    String text = String.format("%s: %s", userName, message.getData());
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                }
                else ConsoleHelper.writeMessage("Error! Message is not a text");
            }
        }

        @Override
        public void run() {
            String userName = null;
            ConsoleHelper.writeMessage("Established connection to " + socket.getRemoteSocketAddress());
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("An error occurred while exchanging data with the remote address");
            }
            if (userName != null && !userName.isEmpty()) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Connection to the remote address is closed");
        }
    }
}