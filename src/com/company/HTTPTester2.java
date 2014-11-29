package com.company;

/**
 * Created by zacktibia on 2014-11-29.
 */
public class HTTPTester2
{
    public static void main(String[] args)
    {
        P2PClient.RequestFile(new ContentRecord("success.png", "127.0.0.1", 6666));
    }
}
