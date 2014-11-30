package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Dante on 2014-11-29.
 */
class P2PServer extends Thread {

    private final int portNumber;
    private ServerSocket serverSocket;

    P2PServer(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void run() {

        try {
            serverSocket = new ServerSocket(portNumber);
            while (true) {
                System.out.println("Waiting for client");
                final Socket clientSocket = serverSocket.accept();
                new FileSendThread(clientSocket).start();
            }
        } catch (SocketException e) {
            System.out.println("Socket Closed.");
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void Finish() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class FileSendThread extends Thread {

    private final Socket clientSocket;

    FileSendThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    private static String ConstructResponse(int code, String phrase) {
        return "HTTP/1.1 " + code + " " + phrase + "\n" +
                "Connection: close\n" +
                "Date: " + new Date().toString() + "\n" +
                "\n";
    }

    public void run() {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            ArrayList<String> headers = Utils.getHttpHeaders(in);

            String GetRequest = headers.get(0);
            String[] SplitRequest = GetRequest.split(" ");
            if (SplitRequest.length != 3 || !SplitRequest[0].equals("GET")) {
                out.print(ConstructResponse(400, "Bad_Request"));
            } else if (!SplitRequest[2].equals(Utils.HTTP_1_1)) {
                out.print(ConstructResponse(505, "HTTP_Version_Not_Supported"));
            } else {
                File requestedFile = new File(P2PApp.sharesDirectory, SplitRequest[1]);
                if (!requestedFile.exists()) {
                    out.print(ConstructResponse(404, "Not_Found"));
                } else {
                    String response =
                            "HTTP/1.1 " + 200 + " OK \n" +
                                    "Connection: close\n" +
                                    "Date: " + new Date().toString() + "\n" +
                                    "Last-Modified: " + new Date(requestedFile.lastModified()).toString() + "\n" +
                                    "Content-Length: " + requestedFile.length() + "\n" +
                                    "\n";
                    out.print(response);
                    FileInputStream fin = new FileInputStream(requestedFile);
                    for (int i = 0; i < requestedFile.length(); i++) {
                        out.write(fin.read());
                    }
                    fin.close();
                }
            }
            out.flush();
            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
