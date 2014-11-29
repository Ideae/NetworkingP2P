package com.company;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class P2PClient
{
    //static int FirstDirectoryServerPort = 4441;
    static TreeMap<Integer, ServerRecord> serverRecords = new TreeMap<>();
    static TreeMap<String, Integer> contentToDHTServer = new TreeMap<>();
    static File sharesDirectory = new File("shares/");
    static int portNumber = -1;

    public static void main(String[] args) throws IOException
    {
        if (args.length == 1)
        {
            portNumber = Integer.parseInt(args[0]);
        }
        serverRecords.put(1, new ServerRecord(1, "127.0.0.1", 4441));
        Init();

        Scanner sc = new Scanner(System.in);
        while(true)
        {
            String line = sc.nextLine();
            Scanner scLine = new Scanner(line);
            String command = scLine.next();
            if (command.equals("update"))
            {
                String filename = scLine.next();
                Update(filename);
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
        ContentRecord peerProvider = ContentRecord.parseRecord(response);
        System.out.printf("The file %s can be found at the peer IP: %s\n", contentName, peerProvider.ContentOwnerIP);
    }
    static void Update(String contentName) throws IOException
    {
        String request = "update " + contentName;
        int serverNum = Utils.Hash(contentName);
        String response = CreateRequest(request, serverNum);
        contentToDHTServer.put(contentName, serverNum);
        System.out.printf("Stored %s in server %d \n", contentName, serverNum);
    }
    static void Init() throws IOException
    {
        String response = CreateRequest("init", 1);
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
        System.out.println("Req");
        try (
                Socket socket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("hello");
            System.out.println("Said hello");
            System.out.println("response was : "+ in.readLine());

            //Contacts P2PServer in the ContentRecord, requests the file and waits for response
            //Upon receiving response:
            //If response is successful:
            //begins file transfer from the P2PServer
            //after it is transferred, saves it to the shared folder by sending GET request.
        } catch (IOException e) {
            System.out.println("P2P Server did not respond or P2P server did not contain record");
            e.printStackTrace();
        }
    }

    //P2PServer Implementation.

    public static void PopulateFiles() throws IOException {

        File[] shares = sharesDirectory.listFiles();
        assert shares != null;
        for (File s : shares){
            Update(s.getName());
    }
    }
    public static void Listen(int portNumber){

        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            final Socket clientSocket = serverSocket.accept();
            Runnable r = new Runnable(){
                public void run()
                {
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
                        ArrayList<String> headers = new ArrayList<>();
                        while(!in.readLine().equals("")){
                            headers.add(in.readLine());
                        }
                        String GetRequest = headers.get(0);
                        String[] SplitRequest = GetRequest.split(" ");
                        if(SplitRequest.length != 3 || !SplitRequest[0].equals("GET")){
                            out.print(ConstructResponse(400, "Bad Request"));
                            return;
                        }
                        if(!SplitRequest[2].equals("HTTP/1.1")){
                            out.print(ConstructResponse(505, "HTTP Version Not Supported"));
                            return;
                        }
                        File requestedFile = new File(sharesDirectory, SplitRequest[1]);
                        if(!requestedFile.exists()){
                            out.print(ConstructResponse(404, "Not Found"));
                            return;
                        }

                        String response =
                                "HTTP/1.1 " + 200 + " OK \n" +
                                        "Connection: close\n"+
                                        "Date: " + new Date().toString() + "\n" +
                                        "Last-Modified: " + new Date(requestedFile.lastModified()).toString() +
                                        "Content-Length:" + requestedFile.length() +
                                        "\n";

                        FileInputStream fin = new FileInputStream(requestedFile);
                        for (int i = 0; i < requestedFile.length(); i++) {
                            out.write(fin.read());
                        }
                        out.flush();

                        fin.close();
                        in.close();
                        out.close();
                        clientSocket.close();
                    }
                    catch(IOException e){e.printStackTrace();}
                }
            };
            new Thread(r).start();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
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
