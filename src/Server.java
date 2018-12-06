import com.google.gson.Gson;
import db_models.Conversation;
import db_models.Message;
import db_models.Spot;
import db_models.User;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static String usersFile = "users.json";
    private static String spotsFile = "spots.json";

    private static BinarySemaphore usersFileMutex = new BinarySemaphore(true);
    private static BinarySemaphore spotsFileMutex = new BinarySemaphore(true);

    private static List<ServerThread> threads = new LinkedList<>();
    private static BinarySemaphore threadsMutex = new BinarySemaphore(true);

    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(2909);
            while (true) {
                Socket socket = server.accept();
                ServerThread thread = new ServerThread(socket);
                thread.start();

                threadsMutex.P();
                threads.add(thread);
                threadsMutex.V();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<User> getUsersFromFile() throws IOException {
        usersFileMutex.P();
        BufferedReader reader = new BufferedReader(new FileReader(usersFile));
        User[] users = new Gson().fromJson(reader, User[].class);
        reader.close();
        usersFileMutex.V();

        List<User> list = new ArrayList<>();
        Collections.addAll(list, users);

        return list;
    }

    private static void writeUsersToFile(List<User> users) throws IOException {
        usersFileMutex.P();
        Writer writer = new FileWriter(usersFile);
        new Gson().toJson(users, writer);
        writer.flush();
        writer.close();
        usersFileMutex.V();
    }

    private static List<Spot> getSpotsFromFile() throws IOException {
        spotsFileMutex.P();
        BufferedReader reader = new BufferedReader(new FileReader(spotsFile));
        Spot[] spots = new Gson().fromJson(reader, Spot[].class);
        reader.close();
        spotsFileMutex.V();

        List<Spot> list = new ArrayList<>();
        Collections.addAll(list, spots);

        return list;
    }

    private static void writeSpotsToFile(List<Spot> spots) throws IOException {
        spotsFileMutex.P();
        Writer writer = new FileWriter(spotsFile);
        new Gson().toJson(spots, writer);
        writer.flush();
        writer.close();
        spotsFileMutex.V();
    }

    static List<Conversation> getMessagesOfUser(String username) throws IOException {
        List<User> users = getUsersFromFile();
        for (User user : users) {
            if (user.username.equals(username)) {
                return user.conversations;
            }
        }

        return null;
    }

    static Boolean checkUserPassword(String username, String password) throws IOException {
        List<User> users = getUsersFromFile();
        for (User user : users) {
            if (user.username.equals(username)) {
                return user.password.equals(password);
            }
        }

        return null;
    }

    static void createNewUser(String username, String password) throws IOException {
        List<User> users = getUsersFromFile();
        users.add(new User(username, password));
        writeUsersToFile(users);
    }

    static List<Spot> getSpots() throws IOException {
        return getSpotsFromFile();
    }

    static void createNewSpot(Spot spot) throws IOException {
        List<Spot> spots = getSpotsFromFile();
        spots.add(spot);
        writeSpotsToFile(spots);

        threadsMutex.P();
        for (ServerThread thread : threads) {
            if (thread.isAlive()) {
                thread.getSpotUpdate(spots);
            }
        }
        threadsMutex.V();
    }

    static void addNewMessageToUser(Message message) throws IOException {
        threadsMutex.P();
        for (ServerThread thread : threads) {
            if (thread.isAlive() && thread.clientUsername != null && thread.clientUsername.equals(message.toUsername)) {
                thread.receiveMessage(message);
            }
        }
        threadsMutex.V();

        String username1;
        String username2;
        if (message.toUsername.compareTo(message.fromUsername) < 0) {
            username1 = message.toUsername;
            username2 = message.fromUsername;
        } else {
            username1 = message.fromUsername;
            username2 = message.toUsername;
        }

        List<User> users = getUsersFromFile();
        boolean conversationExists = false;
        for (User user : users) {
            if (user.username.equals(message.toUsername)) {
                for (Conversation conversation : user.conversations) {
                    if (conversation.username1.equals(username1) && conversation.username2.equals(username2)) {
                        conversationExists = true;
                        conversation.messages.add(message);
                    }
                }

                if (!conversationExists) {
                    Conversation conversation = new Conversation(username1, username2);
                    conversation.messages.add(message);
                    user.conversations.add(conversation);
                }
            }

            if (user.username.equals(message.fromUsername)) {
                for (Conversation conversation : user.conversations) {
                    if (conversation.username1.equals(username1) && conversation.username2.equals(username2)) {
                        conversationExists = true;
                        conversation.messages.add(message);
                    }
                }

                if (!conversationExists) {
                    Conversation conversation = new Conversation(username1, username2);
                    conversation.messages.add(message);
                    user.conversations.add(conversation);
                }
            }
        }

        writeUsersToFile(users);
    }
}

class BinarySemaphore { // used for mutual exclusion
    private boolean value;

    BinarySemaphore(boolean initValue) {
        value = initValue;
    }

    public synchronized void P() { // atomic operation // blocking
        while (!value) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        value = false;
    }

    public synchronized void V() { // atomic operation // non-blocking
        value = true;
        notify(); // wake up a process from the queue
    }
}
