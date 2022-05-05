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
    public FileManager fileManager;
    public WatchFolder watchfolder;

    public static void main(String[] args) throws InterruptedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        Node cl = new Node();
        //cl.fileManager.namingRequest("testfile name.txt");;
        Thread.sleep(120000);
        cl.shutdown();

    }

    public Node() {
        // get own infromation
        new Thread(this::UDPListener).start();
        new Thread(this::UDPListener2).start();
        networkManager = new NetworkManager();
        fileManager = new FileManager(networkManager);
        watchfolder = new WatchFolder(fileManager);

    }

    public void shutdown() {
        networkManager.shutdown();
        fileManager.shutdown();
        System.exit(0);
    }

    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode() + 2147483648.0) * (32768 / (2147483648.0 + Math.abs(-2147483648.0))));
    }

    public void UDPListener() {
        try {
            MulticastSocket msocket = new MulticastSocket(9999);
            String multicastAddress = "230.0.0.1";
            InetAddress multicastGroup = InetAddress.getByName(multicastAddress);
            msocket.joinGroup(multicastGroup);
            DatagramPacket packet = new DatagramPacket(new byte[256], 256);
            while (!msocket.isClosed()) {
                msocket.receive(packet);
                Thread thread = new Thread(() -> {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = (JSONObject) parser.parse(new String(packet.getData(), 0, packet.getLength()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    assert jsonObject != null;
                    String type = jsonObject.get("type").toString();
                    switch (type) {
                        case "discovery":
                            networkManager.listenForNewNodes((String) jsonObject.get("hostname"));
                            break;
                        default:
                            System.out.println("error, foute JSON");
                    }

                });
                thread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void UDPListener2() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(7777);
            while (!datagramSocket.isClosed()) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                Thread thread = new Thread(() -> {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    assert jsonObject != null;
                    String type = jsonObject.get("type").toString();
                    switch (type) {
                        case "checkNeighbors":
                            System.out.println("checkneighbors");
                            System.out.println(datagramPacket.toString() + datagramSocket.toString());
                            System.out.println(networkManager);
                            networkManager.failureCheckListener(datagramPacket, datagramSocket);
                            break;
                        case "shutdown":
                            networkManager.shutdownListener(jsonObject);
                            break;
                        default:
                            System.out.println("error, foute JSON");
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
