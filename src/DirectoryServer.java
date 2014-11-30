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
    static final ConcurrentHashMap<Integer, String> serverIPs = new ConcurrentHashMap<Integer, String>();
    static final ConcurrentHashMap<String, ArrayList<ContentRecord>> contentRecords = new ConcurrentHashMap<String, ArrayList<ContentRecord>>();
    //static ServerRecord thisServerRecord;
    //static ServerRecord nextServerRecord;
    static String thisServerIP, nextServerIP;
    static int serverid;
    //static int serverID;

    //ports: 40140 - 40149

    public static void main(String[] args) throws IOException {

        //if (args.length != 1) {
        //    System.err.println("Specify <port number>");
        //    System.exit(1);
        //}
        //int portNumber = Integer.parseInt(args[0]);
        //int portNumber = Utils.DHTToDHTPort;
        //serverID = portNumber % 10;
        Scanner sc = new Scanner(System.in);

        System.out.println("\n------DHT SERVER------\n");

        System.out.println("Enter ServerID of this DHT Node:");
        String idString;
        idString = sc.nextLine();
        while (!idString.matches("^[1-4]$"))
        {
            System.out.println("Error, Please enter a valid Server number from 1 to 4:");
            idString = sc.nextLine();
        }
        serverid = Integer.parseInt(idString);



        //System.out.println("Enter IP of this DHT Node:");
        //String IP = sc.nextLine();
        //if (IP.isEmpty()) IP = Utils.defaultIPAddress;

        InetAddress current = InetAddress.getLocalHost();
        String Address = current.getHostAddress();
        System.out.println("IP of this DHT node is: " + Address);
        //thisServerRecord = new ServerRecord(serverID, IP, portNumber);
        //thisServerRecord = new ServerRecord(IP, portNumber);
        thisServerIP = Address;

        System.out.println("Enter IP of the successor DHT Node:");
        String NextIP = sc.nextLine();
        if (NextIP.isEmpty()) NextIP = Utils.defaultIPAddress;
        //int nextID = (serverID % 4) + 1;
        //nextServerRecord = new ServerRecord(nextID, NextIP, 4440 + nextID);
        //nextServerRecord = new ServerRecord(NextIP, 4440 + nextID);
        nextServerIP = NextIP;

        //UpdateThread updThread = new UpdateThread(portNumber);
        UpdateThread updThread = new UpdateThread(Utils.DHTToClientPort);
        updThread.start();

        try {
            //ServerSocket serverSocket = new ServerSocket(portNumber);
            ServerSocket serverSocket = new ServerSocket(Utils.DHTToDHTPort);
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                DirectoryTCPThread tcpThread = new DirectoryTCPThread(clientSocket);
                tcpThread.start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + Utils.DHTToDHTPort);
            System.out.println(e.getMessage());
        }
    }
}

class UpdateThread extends Thread {
    private final int portNumber;

    UpdateThread(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(portNumber);
            while (true) {
                try {
                    byte[] buf = new byte[256];
                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(received);
                    String response;
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
        try  {
            Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
            String message = "exit\n" + senderIP + " " + DirectoryServer.thisServerIP;
            out.println(message);

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + DirectoryServer.nextServerIP);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    DirectoryServer.nextServerIP);
        }
        return "no_response";
    }

    String handleInit(String senderIP, int senderPort) {
        if (DirectoryServer.serverIPs.size() < 4) {
            //use tcp to get all records
                try{
                    Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    in.readLine();
                    String message = "init\n" + senderIP + "\n" + DirectoryServer.thisServerIP + " " + DirectoryServer.serverid;
                    out.println(message);

                } catch (UnknownHostException e) {
                    System.err.println("Don't know about host " + DirectoryServer.nextServerIP);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to " +
                            DirectoryServer.nextServerIP);
                }
                return "no_response";
        } else {
            //return server records
            String message = "";
            for (int i = 1; i <= 4; i++) {
                message += DirectoryServer.serverIPs.get(i);
            }
            return message;
            }
    }

    String handleUpdate(String received, String ownerIP) {
        Scanner sc = new Scanner(received);
        sc.next();
        String contentName = sc.next();
        if (!DirectoryServer.contentRecords.containsKey(contentName)) {
            DirectoryServer.contentRecords.put(contentName, new ArrayList<ContentRecord>());
        }
        DirectoryServer.contentRecords.get(contentName).add(new ContentRecord(contentName, ownerIP));
        return "success: the content record was stored on the dht server";
    }

    String handleQuery(String received) {
        Scanner sc = new Scanner(received);
        sc.next();
        String contentName = sc.next();
        String response;
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
    private boolean debug = true;

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
            outputLine = "request received from server " + DirectoryServer.thisServerIP;
            out.println(outputLine);
            inputLine = in.readLine();
            if (inputLine.equals("init")) {
                String fullmessage = in.readLine() + "\n";
                while ((inputLine = in.readLine()) != null) {
                    if (!inputLine.isEmpty())
                        fullmessage += inputLine + "\n";
                }
                if(debug)System.out.print("fullmessage:\n" + fullmessage);
                SendInitMessage(fullmessage);

            } else if (inputLine.equals("exit")) {
                SendExitMessage(in.readLine());

            } else {
                do {
                    if (debug)System.out.println(inputLine);
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
        //int clientPort = Integer.parseInt(sc.next());
        //int serverNumber = Integer.parseInt(sc.next());
        String firstServerIP = sc.next();
        RemoveContentRecords(clientIP);

        if (firstServerIP != DirectoryServer.thisServerIP) {
            //continue traversing dht ring
            try {
                Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                in.readLine();
                out.println("exit\n" + clientInfo);
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + DirectoryServer.nextServerIP);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " +
                        DirectoryServer.nextServerIP);
            }
        }


    }

    void RemoveContentRecords(String clientIP) {
        for (String contentName : DirectoryServer.contentRecords.keySet()) {
            ArrayList<ContentRecord> removeList = new ArrayList<ContentRecord>();
            for (ContentRecord rec : DirectoryServer.contentRecords.get(contentName))
            {
                if (rec.ContentOwnerIP.equals(clientIP))
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
        String p2pclient = sc.next();
        String firstserver = sc.next();
        String servNum = sc.next();

        String newMessage = fullmessage + DirectoryServer.thisServerIP + " " + DirectoryServer.serverid;
        if (firstserver == DirectoryServer.thisServerIP) {
            //send back upd
            if(debug)System.out.println("SEND BACK USING UDP TO " + p2pclient);
            String returnMessage = firstserver + "\n";
            StoreServerRecord(firstserver, Integer.parseInt(servNum));
            while(sc.hasNext())
            {
                String ip = sc.next();
                if (!ip.isEmpty())
                {
                    int id = Integer.parseInt(sc.next());
                    StoreServerRecord(ip, id);
                    returnMessage += ip + " " + id + "\n";
                }
            }

            DatagramSocket socket = new DatagramSocket();
            byte[] buf = returnMessage.getBytes();
            InetAddress address = InetAddress.getByName(p2pclient);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Utils.DHTToClientPort);
            socket.send(packet);
        } else {
            //keep going tcp
            try {
                Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                in.readLine();
                out.println("init\n" + newMessage);
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + DirectoryServer.nextServerIP);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " +
                        DirectoryServer.nextServerIP);
            }
        }


    }

    void StoreServerRecord(String ip, int id) {
        if (!DirectoryServer.serverIPs.containsKey(id))
        {
            DirectoryServer.serverIPs.put(id, ip);
        }
        //ServerRecord rec = ServerRecord.parseRecord(formatString);
        //if (!DirectoryServer.serverRecords.containsKey(rec.serverID)) {
        //    DirectoryServer.serverRecords.put(rec.serverID, rec);
        //}
    }
}
