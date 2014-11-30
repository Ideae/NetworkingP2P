import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
class ContentRecord {
    public final String ContentName;
    public final String ContentOwnerIP;
    public ContentRecord(String ContentName, String ContentOwnerIP)
    {
        this.ContentName = ContentName;
        this.ContentOwnerIP = ContentOwnerIP;
    }

    public static ContentRecord parseRecord(String formattedString) {
        Scanner sc = new Scanner(formattedString);
        String name = sc.next();
        String ip = sc.next();
        return new ContentRecord(name, ip);
    }

    public String toString()
    {
        return String.format("%s %s\n", ContentName, ContentOwnerIP);
    }
}
