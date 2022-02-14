/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hr.dandelic.tcpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author dominikandelic
 */
public class Server {

    private static ServerSocket serverSocket;
    static ArrayList<String> curseWords;
    static ArrayList<Socket> activeSockets;
    static ArrayList<String> activeUsers;
    static int idCounter = 0;

    public static void main(String[] args) {
        Server server = new Server();
        try {
            activeSockets = new ArrayList<>();
            activeUsers = new ArrayList<>();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));

            server.start("localhost", 25555);

            curseWords = new ArrayList<>();
            server.addCurseWords();

            while (true) {
                Socket socket = serverSocket.accept();
                activeSockets.add(socket);
                new Thread(new ServerInstance(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getActiveUsers() {
        String[] userArray = new String[activeUsers.size()];
        String singleUserLine;
        for (int i = 0; i < activeUsers.size(); i++) {
            userArray[i] = activeUsers.get(i);
        }
        singleUserLine = String.join(",", userArray);
        return singleUserLine;
    }

    public void start(String host, int port) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            System.out.println("Listening on port " + serverSocket.getLocalPort());

        } catch (IOException e) {
            System.out.println("Failed while starting the server.");
            System.exit(-1);

        }
    }

    public void addCurseWords() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/curseWords.txt"); BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in)))) {
            String curseWord;
            while ((curseWord = reader.readLine()) != null) {
                curseWords.add(curseWord);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
