package fileManager.NamingServer;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.*;


public class Discovery {
    private static final int PORT = 9999;

    /**
     * Constructor
     */
    public Discovery() {
    }

    /**
     * Start starts the discovery on the NamingServer
     * It listens to all the multicasts that new nodes send and it responds with the numberofnodes, previousnode and nextnode
     */
    public static void start() {
        System.out.println("Starting Discovery");
        try {
            //listen to multicastaddress
            MulticastSocket mSocket = new MulticastSocket(PORT);
            String multicastAddress = "230.0.0.1";
            InetAddress mGroup = InetAddress.getByName(multicastAddress);
            mSocket.joinGroup(mGroup);

            //keep listening
            while (!mSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                mSocket.receive(packet);
                //for every packet a new thread is created
                Thread thread = new Thread(() -> {
                    // Receiving new node
                    String hostname = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("pakcet received : " + hostname);
                    // Adding new node to the list
                    Naming.addNode(hostname, (Inet4Address) packet.getAddress());

                    // Creating response
                    Integer hash = Naming.hashCode(hostname);
                    Integer previousNode = Naming.getNodesList().lowerKey(hash);
                    Integer nextNode = Naming.getNodesList().higherKey(hash);

                    // Structuring response
                    JSONObject jsonObject = new JSONObject();
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
                });
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}