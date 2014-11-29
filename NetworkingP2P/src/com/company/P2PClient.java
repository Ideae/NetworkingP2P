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
        //if (args.length != 1) {
        //    System.out.println("Usage: java P2PClient <hostname>");
        //    return;
        //}
        serverRecords.put(1, new ServerRecord(1, "127.0.0.1", 4441));
        MakeRequest("init", "init", 1);

        Scanner sc = new Scanner(System.in);
        while(true)
        {
            String line = sc.nextLine();
            Scanner scLine = new Scanner(line);
            String command = scLine.next();
            if (command.equals("update"))
            {
                String filename = scLine.next();
                int serverNum = Utils.Hash(filename);
                MakeRequest(command, command + " " + filename, serverNum);
            }

        }
    }
    static void MakeRequest(String command, String request, int serverNum) throws IOException
    {
        //String hostname = "127.0.0.1";
        // get a datagram socket
        DatagramSocket socket = new DatagramSocket();

        ServerRecord record = serverRecords.get(serverNum);
        //String message = "init";
        // send request
        //byte[] buf = new byte[256];
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
        System.out.println("Response: " + received);

        if (command.equals("init"))
        {
            Scanner sc = new Scanner(received);
            while (sc.hasNext())
            {
                int num = Integer.parseInt(sc.next());
                String serverIP = sc.next();
                int serverPort = Integer.parseInt(sc.next());
                if (!serverRecords.containsKey(num))
                    serverRecords.put(num, new ServerRecord(num, serverIP, serverPort));
            }
        }
        else if (command.equals("update"))
        {
            Scanner sc = new Scanner(request); sc.next();
            String contentName = sc.next();
            contentToDHTServer.put(contentName, Utils.Hash(contentName));
            System.out.printf("Stored %s in records\n", contentName);
        }
        socket.close();
    }
}
