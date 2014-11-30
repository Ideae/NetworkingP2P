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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class DirectoryServer
{
    static final Map<Integer, String> serverIPs = new ConcurrentHashMap<Integer, String>();
    static final Map<String, ArrayList<ContentRecord>> contentRecords = new ConcurrentHashMap<String, ArrayList<ContentRecord>>();
    static String thisServerIP, nextServerIP;
    static int serverid;

    private static final HashMap<Integer, String> testMap;
    static
    {
        testMap = new HashMap<Integer, String>();
        testMap.put(1, "14.117.57.40");
        testMap.put(2, "141.117.57.42");
        testMap.put(3, "141.117.57.41");
        testMap.put(4, "141.117.57.46");
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        System.out.println("\n------DHT SERVER------\n");
        InetAddress current = InetAddress.getLocalHost();
        String Address = current.getHostAddress();
        System.out.println("DHTServer: IP of this DHT node is: " + Address);
        thisServerIP = Address;
        boolean tempDebug = false;
        if (!tempDebug){
            System.out.println("DHTServer: Enter ServerID of this DHT Node:");
            String idString;
            idString = sc.nextLine();
            while (!idString.matches("^[1-4]$"))
            {
                System.out.println("DHTServer: Error, Please enter a valid Server number from 1 to 4:");
                idString = sc.nextLine();
            }
            serverid = Integer.parseInt(idString);

            System.out.println("DHTServer: Enter IP of the successor DHT Node:");
            String NextIP = sc.nextLine();
            if (NextIP.isEmpty()) NextIP = Utils.defaultIPAddress;
            nextServerIP = NextIP;
        } else{
            if (thisServerIP.equals(testMap.get(1))){
                nextServerIP = testMap.get(2);
                serverid = 1;
            }
            else if (thisServerIP.equals(testMap.get(2))){
                nextServerIP = testMap.get(3);
                serverid = 2;
            }
            else if (thisServerIP.equals(testMap.get(3))){
                nextServerIP = testMap.get(4);
                serverid = 3;
            }
            else if (thisServerIP.equals(testMap.get(4))){
                nextServerIP = testMap.get(1);
                serverid = 4;
            }
        }


        System.out.println("DHTServer: Server is now listening.");
        UpdateThread updThread = new UpdateThread();
        updThread.start();

        try {
            ServerSocket serverSocket = new ServerSocket(Utils.DHTToDHTPort);
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                DirectoryTCPThread tcpThread = new DirectoryTCPThread(clientSocket);
                tcpThread.start();
            }
        } catch (IOException e) {
            System.out.println("DHTServer: Exception caught when trying to listen on port "
                    + Utils.DHTToDHTPort);
            System.out.println(e.getMessage());
        }
    }
}

class UpdateThread extends Thread {

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(Utils.DHTServerListenPortFromClientUDP);
            while (true) {
                try {
                    byte[] buf = new byte[256];
                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("DHTServer: Got message: ("+ received +")\n");
                    String response;
                    if (received.equals("init")) {
                        response = handleInit(packet.getAddress().getHostAddress());
                    } else if (received.startsWith("update")) {
                        response = handleUpdate(received, packet.getAddress().getHostAddress());
                    } else if (received.startsWith("query")) {
                        response = handleQuery(received);
                    } else if (received.startsWith("exit")) {
                        response = handleExit(packet.getAddress().getHostAddress());
                    } else {
                        response = "invalid request: {" + received + "}";
                    }

                    if (!response.equals("no_response")) {
                        buf = response.getBytes();
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

    String handleExit(String senderIP) {
        try  {
            Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
            String message = "exit\n" + senderIP + " " + DirectoryServer.thisServerIP;
            out.println(message);

        } catch (UnknownHostException e) {
            System.err.println("DHTServer: Don't know about host " + DirectoryServer.nextServerIP);
        } catch (IOException e) {
            System.err.println("DHTServer: Couldn't get I/O for the connection to " +
                    DirectoryServer.nextServerIP);
        }
        return "no_response";
    }

    String handleInit(String senderIP) {
        if(Utils.debug)System.out.println("DHTServer: Handle init has been called.");
        if (DirectoryServer.serverIPs.size() < 4) {
            //use tcp to get all records
                try{
                    Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    in.readLine();

                    String message = "init\n" + senderIP + "\n" + DirectoryServer.thisServerIP + " " + DirectoryServer.serverid + "\nendtaco";
                    if(Utils.debug)System.out.println("DHTServer: About to write message: ("+message+")");
                    out.println(message);
                    out.flush();
                    if(Utils.debug)System.out.println("DHTServer: flushed");
                } catch (UnknownHostException e) {
                    System.err.println("DHTServer: Don't know about host " + DirectoryServer.nextServerIP);
                } catch (IOException e) {
                    System.err.println("DHTServer: Couldn't get I/O for the connection to " +
                            DirectoryServer.nextServerIP);
                }
                return "no_response";
        } else {
            //return server records
            String message = "";
            for (int i = 1; i <= 4; i++) {
                message += DirectoryServer.serverIPs.get(i) + " " + i + "\n";
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
            if(Utils.debug)System.out.println("DHTServer: inputLine was received: " + inputLine);
            if (inputLine.equals("init")) {
                if(Utils.debug)System.out.println("DHTServer: Got an init");
                String fullmessage = in.readLine() + "\n";
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("endtaco")) break;
                    if (!inputLine.isEmpty())
                        fullmessage += inputLine + "\n";
                }
                if(Utils.debug)System.out.print("DHTServer: fullmessage:\n" + fullmessage);
                SendInitMessage(fullmessage);

            } else if (inputLine.equals("exit")) {
                SendExitMessage(in.readLine());

            } else {
                do {
                    if (Utils.debug)System.out.println(inputLine);
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
        String firstServerIP = sc.next();
        RemoveContentRecords(clientIP);
        if (!firstServerIP.equals(DirectoryServer.thisServerIP)) {
            //continue traversing dht ring
            try {
                Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                in.readLine();
                out.println("exit\n" + clientInfo);
            } catch (UnknownHostException e) {
                System.err.println("DHTServer: Don't know about host " + DirectoryServer.nextServerIP);
            } catch (IOException e) {
                System.err.println("DHTServer: Couldn't get I/O for the connection to " +
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
                System.out.println("DHTServer: Removed content record: " + rec.toString());
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
        if (firstserver.equals(DirectoryServer.thisServerIP)) {
            //send back upd
            if(Utils.debug)System.out.println("DHTServer: SEND BACK USING UDP TO " + p2pclient);
            String returnMessage = firstserver + " " + servNum + "\n";
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
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Utils.ClientListensFromDHTServerUDP);
            socket.send(packet);
        } else {
            //keep going tcp
            try {
                Socket socket = new Socket(DirectoryServer.nextServerIP, Utils.DHTToDHTPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                in.readLine();
                if(Utils.debug) System.out.println(newMessage);
                out.println("init\n" + newMessage + "\nendtaco");
            } catch (UnknownHostException e) {
                System.err.println("DHTServer: Don't know about host " + DirectoryServer.nextServerIP);
            } catch (IOException e) {
                System.err.println("DHTServer: Couldn't get I/O for the connection to " +
                        DirectoryServer.nextServerIP);
            }
        }
    }

    void StoreServerRecord(String ip, int id) {
        if (!DirectoryServer.serverIPs.containsKey(id))
        {
            DirectoryServer.serverIPs.put(id, ip);
        }
    }
}
