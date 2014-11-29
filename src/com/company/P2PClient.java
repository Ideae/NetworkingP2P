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

    public static void main(String[] args) throws IOException
    {
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
        }
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
        DatagramSocket socket = new DatagramSocket();
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
}
