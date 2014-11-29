package com.company;

import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class ContentRecord
{
    public String ContentName;
    public String ContentOwnerIP;
    public int ContentOwnerPort;
    public ContentRecord(String ContentName, String ContentOwnerIP, int ContentOwnerPort)
    {
        this.ContentName = ContentName;
        this.ContentOwnerIP = ContentOwnerIP;
        this.ContentOwnerPort = ContentOwnerPort;
    }

    public static ContentRecord parseRecord(String formattedString) {
        Scanner sc = new Scanner(formattedString);
        String name = sc.next();
        String[] ipAndPort = sc.next().split(":");
        return new ContentRecord(name, ipAndPort[0], Integer.parseInt(ipAndPort[1]));
    }

    public String toString()
    {
        return String.format("%s %s\n", ContentName, ContentOwnerIP+":"+ContentOwnerPort);
    }
}
