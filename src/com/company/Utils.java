package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class Utils
{
    static final String HTTP_1_1 = "HTTP/1.1";
    static final int ClientToServerPort = 40140;
    static final int ServerToClientPort = 40140;
    static final int DHTToDHTPort = 40141;
    static final int ClientToDHTPort = 40140;
    static final int DHTToClientPort = 40140;


    public static int Hash(String contentName)
    {
        int total = 0;
        for(char c : contentName.toCharArray())
        {
            if (c == '.') break;
            total += (int)c;
        }
        return (total % 4) + 1;
    }

    static boolean YesOrNo(Scanner sc) {
        while (true) {
            String response = sc.nextLine();
            if (response.isEmpty() ||response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("Yes"))
                return true;
            else if (response.equalsIgnoreCase("N") || response.equalsIgnoreCase("No"))
                return false;
            else
                System.out.println("Incorrect input, try again");
        }
    }

    static ArrayList<String> getHttpHeaders(BufferedReader in) throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            String line = in.readLine();
            if (line.equals("")) break;
            headers.add(line);
        }
        return headers;
    }
}
