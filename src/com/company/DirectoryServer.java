package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class DirectoryServer
{
    static ConcurrentHashMap<Integer, ServerRecord> serverRecords = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ArrayList<ContentRecord>> contentRecords = new ConcurrentHashMap<>();
    static ServerRecord thisServerRecord;
    static ServerRecord nextServerRecord;
    static int serverID;
    private static String defaultIPAddress = "127.0.0.1";
    //ports: 40140 - 40149

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Specify <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        serverID = portNumber % 10;
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter IP of this DHT Node:");
        String IP = sc.nextLine();
        if (IP.isEmpty()) IP = defaultIPAddress;
        thisServerRecord = new ServerRecord(serverID, IP, portNumber);

        System.out.println("Enter IP of the predecessor DHT Node:");
        String NextIP = sc.nextLine();
        if (NextIP.isEmpty()) NextIP = defaultIPAddress;
        int nextID = (serverID % 4) + 1;
        nextServerRecord = new ServerRecord(nextID, NextIP, 4440 + nextID);


        UpdateThread updThread = new UpdateThread(portNumber);
        updThread.start();

        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                DirectoryTCPThread tcpThread = new DirectoryTCPThread(clientSocket);
                tcpThread.start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}

class UpdateThread extends Thread {
    protected static DatagramSocket socket = null;
    int portNumber;

    UpdateThread(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(portNumber);
            while (true) {
                try {
                    byte[] buf = new byte[256];
                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(received);
                    String response = "";
                    if (received.equals("init")) {
                        response = handleInit(packet.getAddress().getHostAddress(), packet.getPort());
                    } else if (received.startsWith("update")) {
                        response = handleUpdate(received, packet.getAddress().getHostAddress());
                    } else if (received.startsWith("query")) {
                        response = handleQuery(received);
                    } else if (received.startsWith("exit")) {
                        response = handleExit(packet.getAddress().getHostAddress(), packet.getPort());
                    } else {
                        response = "invalid request: {" + received + "}";
                    }

                    if (!response.equals("no_response")) {
                        buf = response.getBytes();
                        // send the response to the client at "address" and "port"
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);
                        }
                } catch (IOException e) {
                    e.printStackTrace();
                    }
                }
        } catch (IOException e) {
            e.printStackTrace();
            }
    }

    String handleExit(String senderIP, int senderPort) {
        //System.out.println(senderIP);
        try (
                Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            in.readLine();
            String message = "exit\n" + senderIP + " " + senderPort + " " + DirectoryServer.serverID;
            out.println(message);

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    DirectoryServer.nextServerRecord.IPAddress);
        }
        return "no_response";
    }

    String handleInit(String senderIP, int senderPort) {
        if (DirectoryServer.serverRecords.size() < 4) {
            //use tcp to get all records
                try (
                        Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ) {
                    in.readLine();
                    String message = "init\n" + senderIP + " " + senderPort + "\n" + DirectoryServer.thisServerRecord.toString();
                    out.println(message);

                } catch (UnknownHostException e) {
                    System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to " +
                            DirectoryServer.nextServerRecord.IPAddress);
                }
                return "no_response";
        } else {
            //return server records
            String message = "";
            for (int i = 1; i <= 4; i++) {
                message += DirectoryServer.serverRecords.get(i).toString();
            }
            return message;
            }
    }

    String handleUpdate(String received, String ownerIP) {
        Scanner sc = new Scanner(received);
        sc.next();
        int portNum = sc.nextInt();
        String contentName = sc.next();
        if (!DirectoryServer.contentRecords.containsKey(contentName)) {
            DirectoryServer.contentRecords.put(contentName, new ArrayList<ContentRecord>());
        }
        DirectoryServer.contentRecords.get(contentName).add(new ContentRecord(contentName, ownerIP, portNum));
        return "success: the content record was stored on the dht server";
    }

    String handleQuery(String received) {
        Scanner sc = new Scanner(received);
        sc.next();
        String contentName = sc.next();
        String response = "";
        if (!DirectoryServer.contentRecords.containsKey(contentName)) {
            response = "404 content not found";
        } else {
            ContentRecord rec = DirectoryServer.contentRecords.get(contentName).get(0); //gets the first client in the list of providers
            response = rec.toString();
        }
        return response;
    }
}

class DirectoryTCPThread extends Thread {

    private static int counter = 0;
    private Socket socket = null;

    public DirectoryTCPThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            PrintWriter out =
                    new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));


            String inputLine, outputLine;
            outputLine = "request received from server " + DirectoryServer.serverID;
            out.println(outputLine);
            inputLine = in.readLine();
            if (inputLine.equals("init"))
            {
                String fullmessage = in.readLine() + "\n";
                while ((inputLine = in.readLine()) != null)
                {
                    if (!inputLine.isEmpty())
                        fullmessage += inputLine + "\n";
                }
                //System.out.print("fullmessage:\n" + fullmessage);
                SendInitMessage(fullmessage);
            } else if (inputLine.equals("exit"))
            {
                SendExitMessage(in.readLine());
            } else {
                do
                {
                    //System.out.println(inputLine);
                    outputLine = "I'm a server: " + counter++;
                    out.println(outputLine);
                } while ((inputLine = in.readLine()) != null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void SendExitMessage(String clientInfo) {
        Scanner sc = new Scanner(clientInfo);
        String clientIP = sc.next();
        int clientPort = Integer.parseInt(sc.next());
        int serverNumber = Integer.parseInt(sc.next());
        RemoveContentRecords(clientIP, clientPort);

        if (serverNumber != DirectoryServer.serverID) {
            //continue traversing dht ring
            try (
                    Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                in.readLine();
                out.println("exit\n" + clientInfo);
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " +
                        DirectoryServer.nextServerRecord.IPAddress);
            }
        }


    }

    void RemoveContentRecords(String clientIP, int clientPort) {
        for (String contentName : DirectoryServer.contentRecords.keySet()) {
            ArrayList<ContentRecord> removeList = new ArrayList<>();
            for (ContentRecord rec : DirectoryServer.contentRecords.get(contentName))
            {
                if (rec.ContentOwnerIP.equals(clientIP) && rec.ContentOwnerPort == clientPort)
                {
                    removeList.add(rec);
                }
            }
            for (ContentRecord rec : removeList) {
                DirectoryServer.contentRecords.get(contentName).remove(rec);
                System.out.println("Removed content record: " + rec.toString());
            }
            if (DirectoryServer.contentRecords.get(contentName).size() == 0)
                DirectoryServer.contentRecords.remove(contentName);
        }
    }

    void SendInitMessage(String fullmessage) throws IOException {
        Scanner sc = new Scanner(fullmessage);
        String p2pclient = sc.nextLine();
        String firstserver = sc.nextLine();

        String newMessage = fullmessage += DirectoryServer.thisServerRecord.toString();
        if (new Scanner(firstserver).nextInt() == DirectoryServer.serverID) {
            //send back upd
            // System.out.println("SEND BACK USING UDP TO " + p2pclient);
            String returnMessage = firstserver + "\n";
            StoreServerRecord(firstserver);
            while (sc.hasNextLine())
            {
                String line = sc.nextLine();
                if (!line.isEmpty())
                {
                    StoreServerRecord(line);
                    returnMessage += line + "\n";
                }
            }
            Scanner sc2 = new Scanner(p2pclient);
            String clientIP = sc2.next();
            int clientPort = Integer.parseInt(sc2.next());

            DatagramSocket socket = new DatagramSocket();
            byte[] buf = returnMessage.getBytes();
            InetAddress address = InetAddress.getByName(clientIP);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, clientPort);
            socket.send(packet);
        } else {
            //keep going tcp
            try (
                    Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                in.readLine();
                out.println("init\n" + newMessage);
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " +
                        DirectoryServer.nextServerRecord.IPAddress);
            }
        }


    }

    void StoreServerRecord(String formatString) {
        ServerRecord rec = ServerRecord.parseRecord(formatString);
        if (!DirectoryServer.serverRecords.containsKey(rec.serverID)) {
            DirectoryServer.serverRecords.put(rec.serverID, rec);
        }
    }
}
