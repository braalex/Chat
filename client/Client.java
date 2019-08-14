package client;

import main.Connection;
import main.ConsoleHelper;
import main.Message;
import main.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean isClientConnected = false;

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Enter server address:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Enter server port:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Enter user name:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("An error occurred");
            isClientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Socket thread is interrupted");
                isClientConnected = false;
            }
        }
        if (isClientConnected) {
            ConsoleHelper.writeMessage("Connection established. Print 'exit' for exit");
            while (isClientConnected) {
                String text = ConsoleHelper.readString();
                if (text.equals("exit")) break;
                else if (shouldSendTextFromConsole()) sendTextMessage(text);
            }
        } else ConsoleHelper.writeMessage("An error occurred while client works");
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void infoAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("User %s joined chat", userName));
        }

        protected void infoAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("User %s left chat", userName));
        }

        protected void notifyConnectionStatusChanged(boolean isClientConnected) {
            Client.this.isClientConnected = isClientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST)
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    break;
                }
                else throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT)
                    processIncomingMessage(message.getData());
                else if (message.getType() == MessageType.USER_ADDED)
                    infoAboutAddingNewUser(message.getData());
                else if (message.getType() == MessageType.USER_REMOVED)
                    infoAboutDeletingNewUser(message.getData());
                else throw new IOException("Unexpected MessageType");
            }
        }

        @Override
        public void run() {
            try {
                Socket client = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(client);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }
    }
}
