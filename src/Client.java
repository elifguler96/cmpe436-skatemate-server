import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Client {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 2909);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            out.write("nasilsin");
            out.newLine();
            out.flush();
            while(true);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
