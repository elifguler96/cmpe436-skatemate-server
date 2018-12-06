import com.google.gson.Gson;
import db_models.Conversation;
import db_models.Message;
import db_models.Spot;

import java.io.*;
import java.net.*;
import java.util.List;

public class ServerThread extends Thread {
    private BufferedReader in;
    private BufferedWriter out;
    String clientUsername;
    RequestType type;

    ServerThread(Socket socket) {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            String data;
            while ((data = in.readLine()) != null) {
                Request request = new Gson().fromJson(data, Request.class);

                clientUsername = request.clientUsername;
                type = request.type;

                switch (request.type) {
                    case LOGIN:
                        tryLogin(request.clientUsername, request.password);
                        break;
                    case SENDMESSAGE:
                        sendMessageToUser(request.message);
                        break;
                    case GETCONVERSATIONS:
                        returnConversations();
                        break;
                    case GETSPOTS:
                        returnSpots();
                        break;
                    case CREATESPOT:
                        createSpot(request.spot);
                        break;
                }
            }

            System.out.println("Client connection closed " + type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tryLogin(String username, String password) throws IOException {
        Boolean check = Server.checkUserPassword(username, password);
        if (check == null) {
            Server.createNewUser(username, password);
            check = true;
        }

        Response response = new Response();
        if (check) {
            response.code = 1;
        } else {
            response.code = 2;
        }
        sendToClient(new Gson().toJson(response));
    }

    private void sendMessageToUser(Message message) throws IOException {
        if (message.messageText.isEmpty()) {
            return;
        }

        // to check if user exists
        Boolean check = Server.checkUserPassword(message.toUsername, "");

        Response response = new Response();
        if (check == null) {
            response.code = 8;
        } else {
            Server.addNewMessageToUser(message);
            response.code = 7;
        }
        sendToClient(new Gson().toJson(response));
    }

    private void returnConversations() throws IOException {
        List<Conversation> conversations = Server.getMessagesOfUser(clientUsername);

        Response response = new Response();
        response.code = 3;
        response.conversations = conversations;
        sendToClient(new Gson().toJson(response));
    }

    private void returnSpots() throws IOException {
        List<Spot> spots = Server.getSpots();

        Response response = new Response();
        response.code = 5;
        response.spots = spots;
        sendToClient(new Gson().toJson(response));
    }

    private void createSpot(Spot spot) throws IOException {
        Server.createNewSpot(spot);
    }

    void receiveMessage(Message message) throws IOException {
        Response response = new Response();
        response.code = 9;
        response.message = message;
        sendToClient(new Gson().toJson(response));
    }

    void getSpotUpdate(List<Spot> spots) throws IOException {
        Response response = new Response();
        response.code = 11;
        response.spots = spots;
        sendToClient(new Gson().toJson(response));
    }

    private void sendToClient(String json) throws IOException {
        out.write(json);
        out.newLine();
        out.flush();
    }
}
