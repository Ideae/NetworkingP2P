package com.company;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class DirectoryServer
{
    static TreeMap<Integer, ServerRecord> serverRecords = new TreeMap<>();
    static TreeMap<String, ArrayList<ContentRecord>> contentRecords = new TreeMap<>();
    static ServerRecord thisServerRecord, nextServerRecord;
    static int counter = 0;
    static int portNumber;
    static int serverID;
    static String ipAddress = "127.0.0.1";
    protected static DatagramSocket socket = null;

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Specify <port number>");
            System.exit(1);
        }
        portNumber = Integer.parseInt(args[0]);
        serverID = portNumber % 10;
        thisServerRecord = new ServerRecord(serverID, ipAddress, portNumber);
        int nextID = (serverID % 4) + 1;
        nextServerRecord = new ServerRecord(nextID, ipAddress, 4440 + nextID);
        //hack
        for(int i = 1; i <= 4; i++)
        {
            serverRecords.put(i, new ServerRecord(i, ipAddress, 4440 + i));
        }

        Thread updThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
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
                            if (received.equals("init"))
                            {
                                response = handleInit();
                            }
                            else if (received.startsWith("update"))
                            {
                                response = handleUpdate(received, packet.getAddress().toString(), packet.getPort());
                            }
                            else
                            {
                                response = "I just received {" + received + "}";
                            }


                            buf = response.getBytes();

                            // send the response to the client at "address" and "port"
                            InetAddress address = packet.getAddress();
                            int port = packet.getPort();
                            packet = new DatagramPacket(buf, buf.length, address, port);
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            String handleInit()
            {
                if (serverRecords.size() < 4)
                {
                    //use tcp to get all records
                    //try (
                    //        Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                    //        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    //        BufferedReader in = new BufferedReader(
                    //                new InputStreamReader(socket.getInputStream()));
                    //) {
                    //    BufferedReader stdIn =
                    //            new BufferedReader(new InputStreamReader(System.in));
                    //    //String fromServer;
                    //    //String fromUser;
//
                    //    in.readLine();
                    //    String message = "init\n" + DirectoryServer.thisServerRecord.toString();
                    //    out.println(message);
                    //
                    //} catch (UnknownHostException e) {
                    //    System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
                    //    //System.exit(1);
                    //} catch (IOException e) {
                    //    System.err.println("Couldn't get I/O for the connection to " +
                    //            DirectoryServer.nextServerRecord.IPAddress);
                    //    //System.exit(1);
                    //}
                    return "nope";
                }
                else
                {
                    //return server records
                    String message = "";
                    for(int i = 1; i <= 4; i++)
                    {
                        message += serverRecords.get(i).toString();
                    }
                    return message;
                }
            }
            String handleUpdate(String received, String ownerIP, int ownerPort)
            {
                Scanner sc = new Scanner(received);
                sc.next();
                String contentName = sc.next();
                //more complex implementation possibly needed (only store peers 'in charge' of a
                //contentName who store all peers that provide that file, and query those peers for
                //the entire list of providers)
                if (!contentRecords.containsKey(contentName))
                {
                    contentRecords.put(contentName, new ArrayList<ContentRecord>());
                }
                contentRecords.get(contentName).add(new ContentRecord(contentName, ownerIP, ownerPort));
                return "success: the content record was stored on the dht server";
            }

        });
        updThread.start();

        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            while(true)
            {
                //System.out.println("waiting for tcp request");
                Socket clientSocket = serverSocket.accept();
                //System.out.println("got tcp request");
                class DirectoryTCPThread extends Thread
                {
                    private Socket socket = null;
                    public DirectoryTCPThread(Socket socket)
                    {
                        this.socket = socket;
                    }
                    public void run()
                    {
                        try
                        {
                            //System.out.println("in thread");
                            PrintWriter out =
                                    new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));


                            String inputLine, outputLine;
                            outputLine = "I'm a server: " + counter++;
                            out.println(outputLine);

                            while ((inputLine = in.readLine()) != null)
                            {
                                outputLine = "I'm a server: " + counter++;
                                out.println(outputLine);
                                if (outputLine.equals("I'm a server: 5"))
                                    break;
                            }
                        }
                        catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                DirectoryTCPThread tcpThread = new DirectoryTCPThread(clientSocket);
                tcpThread.start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
    static void beginInit()
    {

    }

}

