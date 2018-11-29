import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    static final String DELIMITER = "$";

    private static String usersFile = "users.txt";
    private static String spotsFile = "spots.txt";

    private static BinarySemaphore usersFileMutex = new BinarySemaphore(true);
    private static BinarySemaphore spotsFileMutex = new BinarySemaphore(true);
    private static Map<String, BinarySemaphore> messageFilesMutexMap = new HashMap<>();
    private static Map<String, BinarySemaphore> conversationFilesMutexMap = new HashMap<>();

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

    static List<String> getMessagesOfUser(String username) throws IOException {
        if (!conversationFilesMutexMap.containsKey(username + ".txt")) {
            conversationFilesMutexMap.put(username + ".txt", new BinarySemaphore(true));
        }

        List<String> messageFiles = new ArrayList<>();
        conversationFilesMutexMap.get(username + ".txt").P();
        Scanner s = new Scanner(new File(username + ".txt"));
        if (s.hasNextLine()) {
            messageFiles = Arrays.asList(s.nextLine().split(DELIMITER));
        }
        s.close();
        conversationFilesMutexMap.get(username + ".txt").V();

        List<String> messages = new ArrayList<>();
        for (String file : messageFiles) {
            if (!messageFilesMutexMap.containsKey(file)) {
                messageFilesMutexMap.put(file, new BinarySemaphore(true));
            }

            messageFilesMutexMap.get(file).P();
            s = new Scanner(new File(file));
            String temp = file + DELIMITER;
            while (s.hasNextLine()) {
                temp += s.nextLine() + "#";
            }
            messages.add(temp);
            s.close();
            messageFilesMutexMap.get(file).V();
        }

        return messages;
    }

    static String getUserCredentials(String username) throws FileNotFoundException {
        usersFileMutex.P();
        Scanner s = new Scanner(new File(usersFile));

        String credentials = null;
        while (s.hasNextLine()) {
            credentials = s.nextLine();
            if (credentials.substring(0, credentials.indexOf(DELIMITER)).equals(username)) {
                break;
            } else {
                credentials = null;
            }
        }

        s.close();
        usersFileMutex.V();
        return credentials;
    }

    static void createNewUser(String username, String password) throws IOException {
        writeToFile(usersFile, username + Server.DELIMITER + password + "\n");

        if (!conversationFilesMutexMap.containsKey(username + ".txt")) {
            conversationFilesMutexMap.put(username + ".txt", new BinarySemaphore(true));
        }
        writeToFile(username + ".txt", "");
    }

    static List<String> getSpots() throws IOException {
        spotsFileMutex.P();
        Scanner s = new Scanner(new File(Server.spotsFile));
        List<String> spots = new ArrayList<>();
        while (s.hasNextLine()) {
            spots.add(s.nextLine());
        }

        s.close();
        spotsFileMutex.V();

        return spots;
    }

    static void createNewSpot(String name, String lat, String lng) throws IOException {
        writeToFile(spotsFile, name + DELIMITER + lat + DELIMITER + lng + "\n");
        threadsMutex.P();
        for (int i = 0; i < threads.size(); i++) {
            ServerThread thread = threads.get(i);

            if (thread.isAlive()) {
                thread.getSpotUpdate();
            } else {
                threads.remove(thread);
                i--;
            }
        }
        threadsMutex.V();
    }

    static void notifyUserOfNewMessage(String toUsername, String fromUsername, String message) throws IOException {
        threadsMutex.P();
        List<Thread> aliveTargetThreads = threads.stream()
                .filter(t -> t.isAlive() && t.clientUsername != null && t.clientUsername.equals(toUsername))
                .collect(Collectors.toList());
        System.out.println("Alive:");
        System.out.println(aliveTargetThreads);
        for (int i = 0; i < aliveTargetThreads.size(); i++) {
            ServerThread thread = threads.get(i);
            System.out.println(thread.clientUsername + " " + toUsername);
            thread.receiveMessage(fromUsername, message);
        }
        threadsMutex.V();

        String username1;
        String username2;
        if (toUsername.compareTo(fromUsername) < 0) {
            username1 = toUsername;
            username2 = fromUsername;
        } else {
            username1 = fromUsername;
            username2 = toUsername;
        }

        String messageFile = username1 + "_" + username2 + ".txt";
        if (!messageFilesMutexMap.containsKey(messageFile)) {
            messageFilesMutexMap.put(messageFile, new BinarySemaphore(true));
        }

        if (!conversationFilesMutexMap.containsKey(toUsername + ".txt")) {
            conversationFilesMutexMap.put(toUsername + ".txt", new BinarySemaphore(true));
        }

        if (!conversationFilesMutexMap.containsKey(fromUsername + ".txt")) {
            conversationFilesMutexMap.put(fromUsername + ".txt", new BinarySemaphore(true));
        }

        File file = new File(messageFile);
        if (!file.exists()) {
            writeToFile(toUsername + ".txt", messageFile + DELIMITER);
            writeToFile(fromUsername + ".txt", messageFile + DELIMITER);
        }

        writeToFile(messageFile, toUsername + Server.DELIMITER + message + Server.DELIMITER + (new SimpleDateFormat().format(new Date())) + "\n");
    }

    private static void writeToFile(String file, String s) throws IOException {
        if (file.equals(usersFile)) {
            usersFileMutex.P();
        } else if (file.equals(spotsFile)) {
            spotsFileMutex.P();
        } else if (file.contains("_")){
            messageFilesMutexMap.get(file).P();
        } else {
            conversationFilesMutexMap.get(file).P();
        }

        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.write(s);
        fileWriter.close();

        if (file.equals(usersFile)) {
            usersFileMutex.V();
        } else if (file.equals(spotsFile)) {
            spotsFileMutex.V();
        } else if (file.contains("_")){
            messageFilesMutexMap.get(file).V();
        } else {
            conversationFilesMutexMap.get(file).V();
        }
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
