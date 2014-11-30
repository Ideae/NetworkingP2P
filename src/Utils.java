import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
class Utils {
    static final String HTTP_1_1 = "HTTP/1.1";
    static final int FileTransferListenPort = 40140;//done tcp
    //static final int ServerToClientPort = 40141;
    static final int DHTToDHTPort = 40141;//done tcp
    static final int DHTServerListenPortFromClientUDP = 40140;//done udp
    static final int ClientListensFromDHTServerUDP = 40141;//done udp

    static String defaultIPAddress = "127.0.0.1";
    public static boolean debug = false;

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
        ArrayList<String> headers = new ArrayList<String>();
        while (true) {
            String line = in.readLine();
            if (line.equals("")) break;
            headers.add(line);
        }
        return headers;
    }
}
