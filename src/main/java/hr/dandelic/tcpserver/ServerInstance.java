/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hr.dandelic.tcpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 *
 * @author dominikandelic
 */
public class ServerInstance implements Runnable {

    private final Socket client;
    private PrintWriter out;
    private BufferedReader in;

    public ServerInstance(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            // Runs when ctrl-c signal is sent
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Gracefully closing streams.");
                    in.close();
                    client.close();
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error while closing streams.");
                }
            }));
            String clientId = Integer.toString(++Server.idCounter);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true);
            if (!in.readLine().contentEquals("handshake")) {
                throw new SocketException();
            }

            // Notify client of ID, wait for the stream pipeline to have some input
            out.println(clientId);
            String clientUsername;
            while (true) {
                if (in.ready()) {
                    clientUsername = in.readLine();
                    for (Socket socket : Server.activeSockets) {
                        if (socket.equals(client)) {
                            continue;
                        }
                        (new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)).printf("%s connected.\n", clientUsername);
                    }
                    Server.activeUsers.add(clientUsername);
                    out.println(Server.getActiveUsers());
                    System.out.println(clientUsername + " connected.");
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            String line;
            // Wait for input from stream pipeline
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (in.ready()) {
                    line = in.readLine();
                    // Loop terminates when Client closes the Frame
                    if (line.equalsIgnoreCase("QUIT_PROCESS")) {
                        out.println("QUIT_CLIENT");
                        line = clientUsername + " disconnected.";
                        Server.activeUsers.remove(clientUsername);
                        Server.activeSockets.remove(client);
                        for (Socket socket : Server.activeSockets) {
                            /*if (socket.equals(client)) {
                                continue;
                            }*/
                            (new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)).printf("%s\n", line);
                        }
                        break;
                    }
                    line = checkForCensoredWords(line);
                    // Broadcast to all active sockets
                    for (Socket socket : Server.activeSockets) {
                        /*if (socket.equals(client)) {
                            continue;
                        }*/
                        (new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)).printf("%s %s (%s): %s\n", getCurrentDateTime(), clientUsername, clientId, line);
                    }
                    System.out.printf("%s %s (%s): %s\n", getCurrentDateTime(), clientUsername, clientId, line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getCurrentDateTime() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("(HH:mm)");
        return date.format(format);
    }

    public String checkForCensoredWords(String line) {
        String[] splitLineArray = line.split(" ");

        for (int i = 0; i < splitLineArray.length; i++) {
            for (String censoredWord : Server.curseWords) {
                if (splitLineArray[i].equalsIgnoreCase(censoredWord)) {
                    char[] censoredCharArray = new char[splitLineArray[i].length()];
                    Arrays.fill(censoredCharArray, '*');
                    splitLineArray[i] = new String(censoredCharArray);
                }
            }
        }
        return String.join(" ", splitLineArray);
    }

}
