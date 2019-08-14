package client;

import main.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BotClient extends Client {
    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Hello, chatty. I am a bot. " +
                    "I understand the commands: date, day, month, year, time, hour, minutes, seconds.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (message.contains(": ")) {
                String[] parts = message.split(": ", 2);
                String user = parts[0];
                String text = parts[1].toLowerCase();
//                String dateFormat = null;
//                switch (text) {
//                    case "date":
//                        dateFormat = "d.MM.YYYY";
//                        break;
//                    case "day":
//                        dateFormat = "d";
//                        break;
//                    case "month":
//                        dateFormat = "MMMM";
//                        break;
//                    case "year":
//                        dateFormat = "YYYY";
//                        break;
//                    case "time":
//                        dateFormat = "H:mm:ss";
//                        break;
//                    case "hour":
//                        dateFormat = "H";
//                        break;
//                    case "minutes":
//                        dateFormat = "m";
//                        break;
//                    case "seconds":
//                        dateFormat = "s";
//                        break;
                Map<String, String> formats = new HashMap<>();
                formats.put("date", "d.MM.YYYY");
                formats.put("day", "d");
                formats.put("month", "MMMM");
                formats.put("year", "YYYY");
                formats.put("time", "H:mm:ss");
                formats.put("hour", "H");
                formats.put("minutes", "m");
                formats.put("seconds", "s");

                formats.keySet().stream().filter(msg -> msg.equals(text))
                        .forEach(s -> sendTextMessage(String.format("Info for %s: %s", user,
                        new SimpleDateFormat(formats.get(s)).format(Calendar.getInstance().getTime()))));
            }
//                if (dateFormat != null) {
//                    String reply = String.format("Info for %s: %s", user,
//                            new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime()));
//                    sendTextMessage(reply);
//                }
        }
    }
}
