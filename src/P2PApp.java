import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by Dante on 2014-11-29.
 */
class P2PApp {

    public static File sharesDirectory; //Concurrency?
    private static P2PClient p2pClient;
    private static P2PServer p2pServer;
    private static int serverPort;

    public static void main(String[] args) throws IOException {
        System.out.println("\n------P2PClient------\n");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Enter IP of DHT Node:");
            String IP = sc.nextLine();
            //InetAddress inad = InetAddress.getByName(IP);
            IP = InetAddress.getByName(IP).getHostAddress();
            try {
                //p2pClient = new P2PClient(DHTPort, new ServerRecord(1, IP, port));
                p2pClient = new P2PClient(IP);
            } catch (IOException e) {
                System.out.println("Connection Error: " + e.getMessage() + ". Please try again.");
                if (Utils.debug) e.printStackTrace();
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

            PopulateFiles();
            p2pServer = new P2PServer();
            p2pServer.start();
        } else {
            System.out.println("Running in receive only mode.");
        }

        while (true) {
            System.out.println("Input a command(Update, Query or Exit)");
            String command = sc.nextLine();
            if (command.equalsIgnoreCase("update")) {
                System.out.println("Enter the file you'd like to share. (File must be in shares directory)");
                String filename = sc.next();
                p2pClient.Update(filename);
            } else if (command.equalsIgnoreCase("query")) {
                System.out.println("Enter the file you'd like to download.");
                String filename = sc.next();
                p2pClient.Query(filename);
            } else if (command.equalsIgnoreCase("exit")) {
                p2pClient.Exit();
                p2pServer.Finish();
                System.exit(1);
                return;
            }else{
                System.out.println("Input not Accepted.");
            }
        }
    }

    private static void PopulateFiles() throws IOException {

        File[] shares = sharesDirectory.listFiles();
        assert shares != null;
        for (File s : shares) {
            p2pClient.Update(s.getName());
        }
    }
}
