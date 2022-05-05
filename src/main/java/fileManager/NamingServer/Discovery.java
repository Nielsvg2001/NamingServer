package fileManager.NamingServer;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.*;


public class Discovery {
    private static final int PORT = 9999;

    public Discovery() {
    }

    public void start(String hostname, DatagramPacket packet) {
        System.out.println("Starting Discovery");
                    // Receiving new node

                    System.out.println("pakcet received : " + hostname);
                    // Adding new node to the list
                    Naming.addNode(hostname, (Inet4Address) packet.getAddress());

                    // Creating response
                    Integer hash = Naming.hashCode(hostname);
                    Integer previousNode = Naming.getNodesList().lowerKey(hash);
                    Integer nextNode = Naming.getNodesList().higherKey(hash);

                    // Structuring response
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type","responseDiscovery");
                    jsonObject.put("numberOfNodes", Naming.numberOfNodes());
                    jsonObject.put("previousNode", (previousNode == null) ? Naming.getNodesList().lastKey() : previousNode);
                    jsonObject.put("nextNode", (nextNode == null) ? Naming.getNodesList().firstKey() : nextNode);

                    System.out.println(jsonObject.toJSONString());

                    // Send numberOfNodes, previousNode and nextNode to the new node
                    byte[] data = jsonObject.toJSONString().getBytes();
                    DatagramPacket reply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    try {
                        DatagramSocket datagramSocket = new DatagramSocket(packet.getPort());
                        datagramSocket.send(reply);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

    }
}