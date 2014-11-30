import java.io.*;
import java.net.*;
import java.util.*;
/**
 * Created by zacktibia on 2014-11-26.
 */
class P2PClient {
    private final int DHTPort;
    //static int FirstDirectoryServerPort = 4441;
    private final HashMap<Integer, String> serverIPs = new HashMap<Integer, String>();
    private final HashMap<String, Integer> contentToDHTServer = new HashMap<String, Integer>();

    public P2PClient(int DHTPort, String serverIP) throws IOException {
        this.DHTPort = DHTPort;
        serverIPs.put(1, serverIP);
        String response = CreateRequest("init", 1);
        System.out.println("Response: " + response);
        Scanner sc = new Scanner(response);
        while (sc.hasNext())
        {
            //int num = Integer.parseInt(sc.next());
            String ip = sc.next();
            int id = Integer.parseInt(sc.next());
            //int serverPort = Integer.parseInt(sc.next());
            if (!serverIPs.containsKey(id))
                serverIPs.put(id, ip);
        }
    }
    void Exit() throws IOException
    {
        CreateRequest("exit", 1);
    }
    void Query(String contentName) throws IOException
    {
        String request = "query " + contentName;
        int serverNum = Utils.Hash(contentName);
        String response = CreateRequest(request, serverNum);
        if (response.startsWith("404")) {
            System.out.println(response);
            return;
        }
        ContentRecord peerProvider = ContentRecord.parseRecord(response);
        System.out.printf("The file %s can be found at the peer IP: %s\n", contentName, peerProvider.toString());
        RequestFile(peerProvider);
    }
    void Update(String contentName) throws IOException
    {
        String request = "update " + contentName;
        int serverNum = Utils.Hash(contentName);
        String response = CreateRequest(request, serverNum);
        System.out.println(response);
        System.out.printf("Stored %s in server %d \n", contentName, serverNum);
        contentToDHTServer.put(contentName, serverNum);

    }
    String CreateRequest(String request, int serverNum) throws IOException
    {
        String ip = serverIPs.get(serverNum);
        DatagramSocket socket;
        socket = new DatagramSocket(DHTPort);
        byte[] buf = request.getBytes();
        InetAddress address = InetAddress.getByName(ip);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Utils.DHTToClientPort);
        socket.send(packet);

        // get response
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        socket.close();
        return received;
    }
    void RequestFile(ContentRecord record) {

        String hostName = record.ContentOwnerIP;
        System.out.println("Attempting to contact server");
        try {
            Socket socket = new Socket(hostName, Utils.ClientToServerPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            out.println("GET " + record.ContentName + " " + Utils.HTTP_1_1);
            out.println();

            List<String> headers = Utils.getHttpHeaders(in);
            String response = headers.get(0);
            String[] SplitResponse = response.split(" ");
            if (!SplitResponse[1].equals("200")) {
                System.out.println("Error " + SplitResponse[1] + ": " + SplitResponse[2]);
                return;
            }
            int length = -1;
            for (String header : headers) {
                if (header.startsWith("Content-Length")) {
                    length = Integer.parseInt(header.split(" ")[1]);
                }
            }
            if (length <= 0) {
                System.out.println("File Length Error");
                return;
            }
            File f = new File(P2PApp.sharesDirectory, record.ContentName);
            FileOutputStream fos = new FileOutputStream(f);
            for (int i = 0; i < length; i++) {
                fos.write(in.read());
            }

            out.close();
            in.close();
            fos.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("P2P Server did not respond or P2P server did not contain record");
            e.printStackTrace();
        }
    }
}
