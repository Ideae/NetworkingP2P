package com.company;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Dante on 2014-11-29.
 */
public class P2PApp {

    public static File sharesDirectory; //Concurrency?
    private static P2PClient p2pClient;
    private static P2PServer p2pServer;
    private static int serverPort;

    public static void main(String[] args) throws IOException {
        int DHTPort;
        if (args.length == 1) {
            DHTPort = Integer.parseInt(args[0]);
        } else {
            DHTPort = Utils.ClientToDHTPort;
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
            try {
                p2pClient = new P2PClient(DHTPort, new ServerRecord(1, IP, port));
            } catch (IOException e) {
                System.out.println("Connection Error: " + e.getMessage() + ". Please try again.");
                continue;
            }
            break;
        }
        while (true) {
            System.out.println("Please input your sharing folder path");
            String name = sc.nextLine();
            if (name.isEmpty()) name = "shares";
            sharesDirectory = new File(name);
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
            System.out.println("Use default Listen Port (" + Utils.ClientToServerPort + ") ?(y/n)");

            if (Utils.YesOrNo(sc)) {
                serverPort = Utils.ClientToServerPort;
            } else {
                System.out.println("Enter Listen Port");
                serverPort = Integer.parseInt(sc.nextLine());
            }
            PopulateFiles();
            p2pServer = new P2PServer(serverPort);
        } else {
            System.out.println("Running in receive only mode.");
        }

        while (true) {
            String line = sc.nextLine();
            Scanner scLine = new Scanner(line);
            String command = scLine.next();
            if (command.equals("update")) {
                String filename = scLine.next();
                p2pClient.Update(filename, serverPort);
            } else if (command.equals("query")) {
                String filename = scLine.next();
                p2pClient.Query(filename);
            } else if (command.equals("exit")) {
                p2pClient.Exit();
                p2pServer.Finish();
                System.exit(1);
                return;
            }
        }
    }

    static void PopulateFiles() throws IOException {

        File[] shares = sharesDirectory.listFiles();
        assert shares != null;
        for (File s : shares) {
            p2pClient.Update(s.getName(), serverPort);
        }
    }
}
