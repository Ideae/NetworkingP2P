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
    static boolean debug = false;

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
        //for(int i = 1; i <= 4; i++)
        //{
        //    serverRecords.put(i, new ServerRecord(i, ipAddress, 4440 + i));
        //}

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
                                response = handleInit(packet.getAddress().getHostAddress(), packet.getPort());
                            }
                            else if (received.startsWith("update"))
                            {
                                response = handleUpdate(received, packet.getAddress().getHostAddress(), packet.getPort());
                            }
                            else if (received.startsWith("query"))
                            {
                                response = handleQuery(received);
                            }
                            else if (received.startsWith("exit"))
                            {
                                response = handleExit(packet.getAddress().getHostAddress(), packet.getPort());
                            }
                            else
                            {
                                response = "invalid request: {" + received + "}";
                            }

                            if (!response.equals("no_response"))
                            {
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
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            String handleExit(String senderIP, int senderPort)
            {
                //System.out.println(senderIP);
                try (
                        Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                ) {
                    in.readLine();
                    String message = "exit\n" + senderIP + " " + senderPort + " " + serverID;
                    out.println(message);

                } catch (UnknownHostException e) {
                    System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to " +
                            DirectoryServer.nextServerRecord.IPAddress);
                }
                return "no_response";
            }
            String handleInit(String senderIP, int senderPort)
            {
                if (serverRecords.size() < 4)
                {
                    //use tcp to get all records
                    try (
                            Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
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
            String handleQuery(String received)
            {
                Scanner sc = new Scanner(received);
                sc.next();
                String contentName = sc.next();
                String response = "";
                if (!contentRecords.containsKey(contentName))
                {
                    response = "404 content not found";
                }
                else
                {
                    ContentRecord rec = contentRecords.get(contentName).get(0); //gets the first client in the list of providers
                    response = rec.toString();
                }
                return response;
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
                            PrintWriter out =
                                    new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));


                            String inputLine, outputLine;
                            outputLine = "request received from server " + serverID;
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
                                if (debug) System.out.print("fullmessage:\n" + fullmessage);
                                SendInitMessage(fullmessage);
                            }
                            else if (inputLine.equals("exit"))
                            {
                                SendExitMessage(in.readLine());
                            }
                            else
                            {
                                do
                                {
                                    if (debug) System.out.println(inputLine);
                                    outputLine = "I'm a server: " + counter++;
                                    out.println(outputLine);
                                } while ((inputLine = in.readLine()) != null);
                            }
                        }
                        catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    void SendExitMessage(String clientInfo)
                    {
                        Scanner sc = new Scanner(clientInfo);
                        String clientIP = sc.next();
                        int clientPort = Integer.parseInt(sc.next());
                        int serverNumber = Integer.parseInt(sc.next());
                        RemoveContentRecords(clientIP, clientPort);

                        if (serverNumber != serverID)
                        {
                            //continue traversing dht ring
                            try (
                                    Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                    BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                            ) {
                                in.readLine();
                                out.println("exit\n"+clientInfo);
                            } catch (UnknownHostException e) {
                                System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
                            } catch (IOException e) {
                                System.err.println("Couldn't get I/O for the connection to " +
                                        DirectoryServer.nextServerRecord.IPAddress);
                            }
                        }


                    }
                    void RemoveContentRecords(String clientIP, int clientPort)
                    {
                        for(String contentName : contentRecords.keySet())
                        {
                            ArrayList<ContentRecord> removeList = new ArrayList<>();
                            for(ContentRecord rec : contentRecords.get(contentName))
                            {
                                if (rec.ContentOwnerIP.equals(clientIP) && rec.ContentOwnerPort == clientPort)
                                {
                                    removeList.add(rec);
                                }
                            }
                            for(ContentRecord rec : removeList)
                            {
                                contentRecords.get(contentName).remove(rec);
                                System.out.println("Removed content record: " + rec.toString());
                            }
                            if (contentRecords.get(contentName).size() == 0) contentRecords.remove(contentName);
                        }
                    }
                    void SendInitMessage(String fullmessage) throws IOException
                    {
                        Scanner sc = new Scanner(fullmessage);
                        String p2pclient = sc.nextLine();
                        String firstserver = sc.nextLine();

                        String newMessage = fullmessage += DirectoryServer.thisServerRecord.toString();
                        if (new Scanner(firstserver).nextInt() == serverID)
                        {
                            //send back upd
                            if (debug) System.out.println("SEND BACK USING UDP TO " + p2pclient);
                            String returnMessage = firstserver + "\n";
                            StoreServerRecord(firstserver);
                            while(sc.hasNextLine())
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
                        }
                        else
                        {
                            //keep going tcp
                            try (
                                    Socket socket = new Socket(DirectoryServer.nextServerRecord.IPAddress, DirectoryServer.nextServerRecord.portNumber);
                                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                    BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                            ) {
                                in.readLine();
                                out.println("init\n"+newMessage);
                            } catch (UnknownHostException e) {
                                System.err.println("Don't know about host " + DirectoryServer.nextServerRecord.IPAddress);
                            } catch (IOException e) {
                                System.err.println("Couldn't get I/O for the connection to " +
                                        DirectoryServer.nextServerRecord.IPAddress);
                            }
                        }


                    }
                    void StoreServerRecord(String formatString)
                    {
                        ServerRecord rec = ServerRecord.parseRecord(formatString);
                        if (!serverRecords.containsKey(rec.serverID))
                        {
                            serverRecords.put(rec.serverID, rec);
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

