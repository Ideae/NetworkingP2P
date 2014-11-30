package com.company;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Dante on 2014-11-29.
 */
public class HTTPTester {
    public static void main(String args[]) throws IOException {
        Scanner sc = new Scanner(System.in);
        new Thread(new Runnable() {
            @Override
            public void run() {
                P2PClient.Listen(6666);
            }
        }).start();
//
        //P2PClient.RequestFile(new ContentRecord("sup","127.0.0.1",4444));

        //P2PClient.PopulateFiles();
    }

}
