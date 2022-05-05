package fileManager.Node;


import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;


public class NetworkManager {

    public String hostName;
    private int currentID;
    private InetAddress ipAddress;
    private final int numNodesWhenEntered;
    private int previousNode;
    private int nextNode;
    public static final int DISCOVERYPORT = 9999;
    public static final String NAMINGPORT = "8080";
    public static int SHUTDOWNPORT = 9998;
    public static int FAILUREPORT = 9997;
    public static int CHECKPORT = 9987;
    public static String NAMINGSERVERADDRESS = "localhost";
    private final String multicastAddress = "230.0.0.1";
    private MulticastSocket msocket;
    private InetAddress multicastGroup;

    public NetworkManager() {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            ipAddress = InetAddress.getLocalHost();
            currentID = Node.hashCode(hostName);
            multicastGroup = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            System.out.println("Could not get LocalHost information: " + e.getMessage());
        }

        // start services
        numNodesWhenEntered = dicovery();
        new Thread(this::checkNeighbors).start();

        System.out.println("I'm node " + hostName + "(" + currentID + ")" + " and my ip is " + ipAddress);
        System.out.println("There are " + numNodesWhenEntered + " nodes in the network \nThe previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
    }

    public void addNode(InetAddress ipaddr) {
        System.out.println("addnode");
        System.out.println("nodeName" + ipaddr.getHostName());
        System.out.println("nodeName" + ipaddr.getHostAddress());
        HttpResponse<String> response = Unirest.post("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/addNode")
                .queryString("nodeName", ipaddr.getHostName())
                .queryString("nodeIP", ipaddr.getHostAddress())
                .asString();
    }

    public void removeNode(String nodeName) {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/removeNode")
                .queryString("nodeName", nodeName)
                .asString();
    }

    public Inet4Address getNodeInfo(int id) {
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/getNodeInfo")
                .queryString("id", id)
                .asObject(Inet4Address.class);
        return response.getBody();
    }

    public Inet4Address getPreviousIP() {
        return getNodeInfo(previousNode);
    }

    public int dicovery() {
        try {
            // Send hostname (+ ip) to naming server and other nodes.
            msocket = new MulticastSocket();
            JSONObject obj = new JSONObject();
            obj.put("type","discovery");
            obj.put("hostname", hostName);
            byte[] buf = obj.toJSONString().getBytes();
            //byte[] buf = hostName.getBytes();
            msocket.joinGroup(multicastGroup);
            DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, multicastGroup, DISCOVERYPORT);
            datagramPacket.setAddress(multicastGroup);
            msocket.send(datagramPacket);

            // Receive response from naming server
            datagramPacket = new DatagramPacket(new byte[1024], 1024);
            msocket.receive(datagramPacket);

            // Handle received data
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));
            NAMINGSERVERADDRESS = datagramPacket.getAddress().getHostAddress();
            previousNode = Integer.parseInt(jsonObject.get("previousNode").toString());
            nextNode = Integer.parseInt(jsonObject.get("nextNode").toString());
            System.out.println("In discovery: The previous node is " + previousNode + " and the next node is " + nextNode);
            return Integer.parseInt(jsonObject.get("numberOfNodes").toString());

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // listens and if it receives a packet, the node checks if it must update the previous or next node
    public void listenForNewNodes(String hostname) {
        System.out.println("Starting listenForNewNodes");

                    int hash = Node.hashCode(hostname);
                    if ((hash < nextNode && hash > currentID) || (nextNode <= currentID && hash > currentID) || (nextNode <= currentID && hash < nextNode)) {
                        nextNode = hash;
                    }
                    if ((hash > previousNode && hash < currentID) || (previousNode >= currentID && hash < currentID) || (previousNode >= currentID && hash > previousNode)) {
                        previousNode = hash;
                    }
                    System.out.println("In listenForNewNodes: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");


    }

    public void shutdown() {
        System.out.println("shutdown");
        try {
            DatagramSocket socket = new DatagramSocket();
            try {
                System.out.println(previousNode);
                System.out.println(nextNode);
                // Sending nextNode to previousNode
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type","shutdown");
                jsonObject.put("newNextNode", nextNode);
                byte[] buf = jsonObject.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), 7777);
                System.out.println("ppp" + packet);
                System.out.println("jsss" + jsonObject);
                System.out.println("socket" + socket);
                socket.send(packet);

                // Sending previousNode to nextNode
                jsonObject = new JSONObject();
                jsonObject.put("type","shutdown");
                jsonObject.put("newPreviousNode", previousNode);
                buf = jsonObject.toString().getBytes();
                packet = new DatagramPacket(buf, buf.length, getNodeInfo(nextNode), 7777);
                socket.send(packet);
                msocket.leaveGroup(multicastGroup);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // Remove this node from the list of nodes
        removeNode(hostName);
    }


    public void shutdownListener(JSONObject jsonObject) {
        System.out.println("Starting Shutdown Listener");
                            // update previousNode
                            if (jsonObject.containsKey("newPreviousNode")) {
                                previousNode = Integer.parseInt(jsonObject.get("newPreviousNode").toString());
                            }
                            // update nextNode
                            if (jsonObject.containsKey("newNextNode")) {
                                nextNode = Integer.parseInt(jsonObject.get("newNextNode").toString());
                            }
                            System.out.println("In shutdonwListener: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
                        }

    public void checkNeighbors() {
        System.out.println("Checking for failure...");
        int teller = 0;
        try {
            DatagramSocket socket = new DatagramSocket();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type","checkNeighbors");
            byte[] buf = jsonObject.toJSONString().getBytes();
            while (true) {
                try {
                    socket.setSoTimeout(100);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), 7777);
                    System.out.println("prev" + previousNode);
                    System.out.println("js" + jsonObject);
                    System.out.println("pak" + packet);
                    socket.send(packet);
                    packet = new DatagramPacket(new byte[256], 256);
                    socket.receive(packet);
                    teller = 0;
                } catch (SocketTimeoutException e) {
                    System.out.println("Teller is " + teller);
                    teller++;
                    if (teller > 3) {
                        failure(previousNode);
                        System.out.println("In checkNeighbors: Aanroepen failure");
                        Thread.sleep(2000);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            failure(previousNode);
        }
    }


    public void failureCheckListener(DatagramPacket datagramPacket, DatagramSocket datagramSocket) {
try {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type","responseOK");
    byte[] buf = jsonObject.toJSONString().getBytes();
    DatagramPacket reply = new DatagramPacket(buf, buf.length, datagramPacket.getAddress(), datagramPacket.getPort());
    datagramSocket.send(reply);
}
catch (Exception e){
    e.printStackTrace();
}
    }


    public void failure(int hash) {
        System.out.println("Failure detected");
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            try {
                // sending hostname of failed node to the Naming server
                byte[] buf = String.valueOf(hash).getBytes();
                System.out.println("In failure: " + hostName);
                DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, InetAddress.getByName(NAMINGSERVERADDRESS), FAILUREPORT);
                datagramSocket.send(datagramPacket);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
