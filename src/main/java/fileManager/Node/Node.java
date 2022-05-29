package fileManager.Node;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;


public class Node {

    public NetworkManager networkManager;
    public static FileManager fileManager;
    public WatchFolder watchfolder;
    public SyncAgent syncAgent;

    public static void main(String[] args) throws InterruptedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        Node cl = new Node();
        //cl.fileManager.namingRequest("testfile name.txt");;
        //Thread.sleep(120000);
        //cl.shutdown();

    }

    public Node() {
        // get own infromation
        networkManager = new NetworkManager();
        fileManager = new FileManager(networkManager);
        watchfolder = new WatchFolder(fileManager);
        syncAgent = new SyncAgent(networkManager);
    }

    public void shutdown() {
        networkManager.shutdown();
        fileManager.shutdown();
        System.exit(0);
    }

    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode() + 2147483648.0) * (32768 / (2147483648.0 + Math.abs(-2147483648.0))));
    }

    public static void sendReplicatedfiles(){
        fileManager.startUp();
    }
}
