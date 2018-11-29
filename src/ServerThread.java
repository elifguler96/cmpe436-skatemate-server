import java.io.*;
import java.net.*;
import java.util.List;

public class ServerThread extends Thread {
    private BufferedReader in;
    private BufferedWriter out;
    String clientUsername;

    ServerThread(Socket socket) {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.out.println("ConnectionHandleThread: Failed to create input stream. Leaving.");
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            String data;
            while ((data = in.readLine()) != null) {
                System.out.println(data);

                clientUsername = data.substring(0, data.indexOf(Server.DELIMITER));
                data = data.substring(data.indexOf(Server.DELIMITER) + 1);

                String type = data.substring(0, data.indexOf(Server.DELIMITER));
                data = data.substring(data.indexOf(Server.DELIMITER) + 1);

                String username;
                switch (type) {
                    case "LOGIN":
                        username = data.substring(0, data.indexOf(Server.DELIMITER));
                        String password = data.substring(data.indexOf(Server.DELIMITER) + 1);
                        tryLogin(username, password);
                        break;
                    case "SENDMESSAGE":
                        username = data.substring(0, data.indexOf(Server.DELIMITER));
                        String message = data.substring(data.indexOf(Server.DELIMITER) + 1);
                        sendMessageToUser(username, message);
                        break;
                    case "GETCONVERSATIONS":
                        returnConversations();
                        break;
                    case "GETSPOTS":
                        returnSpots();
                        break;
                    case "CREATESPOT":
                        String spotName = data.substring(0, data.indexOf(Server.DELIMITER));
                        String lat = data.substring(data.indexOf(Server.DELIMITER) + 1, data.lastIndexOf(Server.DELIMITER));
                        String lng = data.substring(data.lastIndexOf(Server.DELIMITER) + 1);
                        createSpot(spotName, lat, lng);
                        break;
                }
            }

            System.out.println("Client connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tryLogin(String username, String password) throws IOException {
        String credentials = Server.getUserCredentials(username);
        if (credentials != null) {
            if (credentials.substring(credentials.indexOf(Server.DELIMITER) + 1).equals(password)) {
                sendToClient("1" + Server.DELIMITER);
            } else {
                sendToClient("2" + Server.DELIMITER);
            }
        } else {
            Server.createNewUser(username, password);

            sendToClient("1" + Server.DELIMITER);
        }
    }

    private void sendMessageToUser(String username, String message) throws IOException {
        if (Server.getUserCredentials(username) == null) {
            sendToClient("8" + Server.DELIMITER);
        } else {
            Server.notifyUserOfNewMessage(username, clientUsername, message);
            sendToClient("7" + Server.DELIMITER);
        }
    }

    private void returnConversations() throws IOException {
        List<String> conversationsList = Server.getMessagesOfUser(clientUsername);
        if (conversationsList.isEmpty()) {
            sendToClient("4" + Server.DELIMITER);
            return;
        }

        String temp = "3-";
        for (String conversations : conversationsList) {
            temp += conversations + "-";
        }
        System.out.printf("Conv: %s", temp);
        sendToClient(temp);
    }

    private void returnSpots() throws IOException {
        List<String> spots = Server.getSpots();
        if (spots.isEmpty()) {
            sendToClient("6" + Server.DELIMITER);
            return;
        }

        String temp = "5-";
        for (String spot : spots) {
            temp += spot + "-";
        }
        sendToClient(temp);
    }

    private void createSpot(String name, String lat, String lng) throws IOException {
        Server.createNewSpot(name, lat, lng);
    }

    void receiveMessage(String fromUsername, String message) throws IOException {
        sendToClient("9" + Server.DELIMITER + fromUsername + Server.DELIMITER + message);
    }

    void getSpotUpdate() throws IOException {
        sendToClient("11" + Server.DELIMITER);
    }

    private void sendToClient(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }
}
