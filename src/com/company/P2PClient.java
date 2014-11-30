package com.company;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class P2PClient
{
    public static final String HTTP_1_1 = "HTTP/1.1";
    //static int FirstDirectoryServerPort = 4441;
    static TreeMap<Integer, ServerRecord> serverRecords = new TreeMap<>();
    static TreeMap<String, Integer> contentToDHTServer = new TreeMap<>();
    static File sharesDirectory;
    static int portNumber = -1;
    static int serverPort;

    public static void main(String[] args) throws IOException
    {
        if (args.length == 1)
        {
            portNumber = Integer.parseInt(args[0]);
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Enter IP of DHT Node:");
            String IP = sc.nextLine();
            if (IP.isEmpty()) IP = "127.0.0.1";
            System.out.println("Enter Port of DHT Node:");
            String p = sc.nextLine();
            if (p.isEmpty()) p = "4441";
            int port = Integer.parseInt(p);
            serverRecords.put(1, new ServerRecord(1, IP, port));
            if (Init()) break;
        }
        while (true) {
            System.out.println("Please input your sharing folder name");
            String name = sc.nextLine(); if (name.isEmpty()) name = "shares";
            sharesDirectory = new File( name + "/");
            if (!sharesDirectory.exists()) {
                System.out.println("Directory does not exist, create?(y/n)");
                if (Utils.YesOrNo(sc)) {
                    if (!sharesDirectory.mkdirs()) {
                        System.out.println("Error making directory, Program will now exit");
                        return;
                    }
                } else {
                    System.out.println("Try Again.");
                    continue;
                }
            }
            break;
        }
        System.out.println("Connected to DHT Ring, broadcast Files? (y/n)");

        if (Utils.YesOrNo(sc)) {
            System.out.println("Current sharing folder is " + sharesDirectory.getAbsolutePath());
            System.out.println("Enter Listen Port");
            String p = sc.nextLine();
            if (p.isEmpty()) p = "6666";
            serverPort = Integer.parseInt(p);
            PopulateFiles(serverPort);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Listen(serverPort);
                }
            }).start();
        } else {
            System.out.println("Running in receive only mode.");
        }

        while (true) {
            String line = sc.nextLine();
            Scanner scLine = new Scanner(line);
            String command = scLine.next();
            if (command.equals("update"))
            {
                String filename = scLine.next();
                Update(filename, serverPort);
            }
            else if (command.equals("query"))
            {
                String filename = scLine.next();
                Query(filename);
            }
            else if (command.equals("exit"))
            {
                Exit();
                System.exit(1);
                return;
            }
        }
    }
    static void Exit() throws IOException
    {
        CreateRequest("exit", 1);
    }
    static void Query(String contentName) throws IOException
    {
        String request = "query " + contentName;
        int serverNum = Utils.Hash(contentName);
        String response = CreateRequest(request, serverNum);
        if (response.startsWith("404")) {
            System.out.println(response);
            return;
        }
        ContentRecord peerProvider = ContentRecord.parseRecord(response);
        System.out.printf("The file %s can be found at the peer IP: %s\n", contentName, peerProvider.toString());
        RequestFile(peerProvider);

    }
    static void Update(String contentName, int portNumber) throws IOException
    {
        String request = "update " + portNumber + " " + contentName;
        int serverNum = Utils.Hash(contentName);
        String response = CreateRequest(request, serverNum);
        contentToDHTServer.put(contentName, serverNum);
        System.out.printf("Stored %s in server %d \n", contentName, serverNum);
    }

    static boolean Init() throws IOException
    {
        String response = CreateRequest("init", 1);
        //TODO:Handle incorrect response and return false.
        System.out.println("Response: " + response);
        Scanner sc = new Scanner(response);
        while (sc.hasNext())
        {
            int num = Integer.parseInt(sc.next());
            String serverIP = sc.next();
            int serverPort = Integer.parseInt(sc.next());
            if (!serverRecords.containsKey(num))
                serverRecords.put(num, new ServerRecord(num, serverIP, serverPort));
        }
        return true;
    }
    static String CreateRequest(String request, int serverNum) throws IOException
    {
        ServerRecord record = serverRecords.get(serverNum);
        DatagramSocket socket;
        if (portNumber > 0)
            socket = new DatagramSocket(portNumber);
        else
            socket = new DatagramSocket();

        byte[] buf = request.getBytes();
        InetAddress address = InetAddress.getByName(record.IPAddress);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, record.portNumber);
        socket.send(packet);

        // get response
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        socket.close();
        return received;
    }

    public static void RequestFile (ContentRecord record){

        String hostName = record.ContentOwnerIP;
        int portNumber = record.ContentOwnerPort;
        System.out.println("Attempting to contact server");
        try (
                Socket socket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("GET " + record.ContentName + " " + HTTP_1_1);
            out.println();

            List<String> headers = getHttpHeaders(in);
            String response = headers.get(0);
            String[] SplitResponse = response.split(" ");
            if (!SplitResponse[1].equals("200")) {
                System.out.println("Error " + SplitResponse[1] + ": " + SplitResponse[2]);
                return;
            }
            int length = -1;
            for (String header : headers) {
                if (header.startsWith("Content-Length")) {
                    length = Integer.parseInt(header.split(" ")[1]);
                }
            }
            if (length <= 0) {
                System.out.println("File Length Error");
                return;
            }
            File f = new File(sharesDirectory,record.ContentName);
            FileOutputStream fos = new FileOutputStream(f);
            for (int i = 0; i < length; i++) {
                fos.write(in.read());
            }

            out.close();
            in.close();
            fos.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("P2P Server did not respond or P2P server did not contain record");
            e.printStackTrace();
        }
    }

    //P2PServer Implementation.

    public static void PopulateFiles(int portNumber) throws IOException {

        File[] shares = sharesDirectory.listFiles();
        assert shares != null;
        for (File s : shares){
            Update(s.getName(), portNumber);
    }
    }
    public static void Listen(int portNumber){
        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            while(true)
            {
                System.out.println("Waiting for client");
                final Socket clientSocket = serverSocket.accept();
                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                            ArrayList<String> headers = getHttpHeaders(in);

                            String GetRequest = headers.get(0);
                            String[] SplitRequest = GetRequest.split(" ");
                            if (SplitRequest.length != 3 || !SplitRequest[0].equals("GET"))
                            {
                                out.print(ConstructResponse(400, "Bad Request"));
                            } else if (!SplitRequest[2].equals(HTTP_1_1))
                            {
                                out.print(ConstructResponse(505, "HTTP Version Not Supported"));
                            } else
                            {
                                File requestedFile = new File(sharesDirectory, SplitRequest[1]);
                                if (!requestedFile.exists())
                                {
                                    out.print(ConstructResponse(404, "Not Found"));
                                } else
                                {
                                    String response =
                                            "HTTP/1.1 " + 200 + " OK \n" +
                                                    "Connection: close\n" +
                                                    "Date: " + new Date().toString() + "\n" +
                                                    "Last-Modified: " + new Date(requestedFile.lastModified()).toString() + "\n" +
                                                    "Content-Length: " + requestedFile.length() + "\n"+
                                                    "\n";
                                    out.print(response);
                                    FileInputStream fin = new FileInputStream(requestedFile);
                                    for (int i = 0; i < requestedFile.length(); i++)
                                    {
                                        out.write(fin.read());
                                    }
                                    fin.close();
                                }
                            }
                            out.flush();
                            in.close();
                            out.close();
                            clientSocket.close();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }

                    }
                };
                new Thread(r).start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

    private static ArrayList<String> getHttpHeaders(BufferedReader in) throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            String line = in.readLine();
            if(line.equals("")) break;
            headers.add(line);
        }
        return headers;
    }

    static String ConstructResponse(int code, String phrase){
        String response =
                "HTTP/1.1 " + code + " " + phrase + "\n" +
                "Connection: close\n"+
                "Date: " + new Date().toString() + "\n" +
                "\n";
        return response;
    }

}
