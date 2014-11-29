package com.company;

import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class ServerRecord
{
    public int serverID;
    public String IPAddress;
    public int portNumber;
    public ServerRecord(int serverID, String IPAddress, int portNumber)
    {
        this.serverID = serverID;
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }
    @Override
    public String toString()
    {
        return String.format("%d %s %d\n", serverID, IPAddress, portNumber);
    }
    public static ServerRecord parseRecord(String formattedString)
    {
        Scanner sc = new Scanner(formattedString);
        int serverNum = Integer.parseInt(sc.next());
        String ip = sc.next();
        int port = Integer.parseInt(sc.next());
        return new ServerRecord(serverNum, ip, port);
    }

}
