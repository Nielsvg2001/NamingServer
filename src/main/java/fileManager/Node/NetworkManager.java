package fileManager.Node;


import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
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
        new Thread(this::listenForNewNodes).start();
        new Thread(this::shutdownListener).start();
        new Thread(this::checkNeighbors).start();
        new Thread(this::failureCheckListener).start();

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
        System.out.println("getNodeInfo");
        System.out.println(NAMINGSERVERADDRESS);
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/getNodeInfo")
                .queryString("id", id)
                .asObject(Inet4Address.class);

        System.out.println(response.getBody());
        return response.getBody();
    }

    public int dicovery() {
        try {
            // Send hostname (+ ip) to naming server and other nodes.
            msocket = new MulticastSocket();
            byte[] buf = hostName.getBytes();
            msocket.joinGroup(multicastGroup);
            DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, multicastGroup, DISCOVERYPORT);
            datagramPacket.setAddress(multicastGroup);
            msocket.send(datagramPacket);

            // Receive response from naming server
            datagramPacket = new DatagramPacket(new byte[1024], 1024);
            msocket.receive(datagramPacket);

            System.out.println(new String(datagramPacket.getData(),0, datagramPacket.getLength()));

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
    public void listenForNewNodes() {
        System.out.println("Starting listenForNewNodes");
        try {
            msocket = new MulticastSocket(DISCOVERYPORT);
            msocket.joinGroup(multicastGroup);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                msocket.receive(packet);
                Thread thread = new Thread(() -> {
                    String hostname = new String(packet.getData(), 0, packet.getLength());
                    int hash = Node.hashCode(hostname);
                    if ((hash < nextNode && hash > currentID) || (nextNode <= currentID && hash > currentID) || (nextNode <= currentID && hash < nextNode)) {
                        nextNode = hash;
                    }
                    if ((hash > previousNode && hash < currentID) || (previousNode >= currentID && hash < currentID) || (previousNode >= currentID && hash > previousNode)) {
                        previousNode = hash;
                    }
                    System.out.println("In listenForNewNodes: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        System.out.println("shutdown");
        try {
            DatagramSocket socket = new DatagramSocket();
            try {
                // Sending nextNode to previousNode
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("newNextNode", nextNode);
                byte[] buf = jsonObject.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), SHUTDOWNPORT);
                socket.send(packet);

                // Sending previousNode to nextNode
                jsonObject = new JSONObject();
                jsonObject.put("newPreviousNode", previousNode);
                buf = jsonObject.toString().getBytes();
                packet = new DatagramPacket(buf, buf.length, getNodeInfo(nextNode), SHUTDOWNPORT);
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
        System.exit(0);
    }


    public void shutdownListener() {
        System.out.println("Starting Shutdown Listener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(SHUTDOWNPORT);
            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                Thread thread = new Thread(() -> {
                    try {
                        if (datagramPacket.getAddress() != ipAddress) {
                            // Handle received data
                            JSONParser parser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));

                            System.out.println("In shudownListener: Received " + jsonObject.toJSONString());

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
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkNeighbors() {
        System.out.println("Checking for failure...");
        int teller = 0;
        try {
            DatagramSocket socket = new DatagramSocket();

            byte[] buf = "test".getBytes();
            while (true) {
                try {
                    socket.setSoTimeout(100);
                    System.out.println("in checkneighbors getnodeinfo prev " + getNodeInfo(previousNode));
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), CHECKPORT);
                    socket.send(packet);
                    packet = new DatagramPacket(new byte[256], 256);
                    socket.receive(packet);
                    teller = 0;
                    String packetString = new String(packet.getData(), 0, packet.getLength());
                    if (!packetString.equals("OK")) {
                        failure(previousNode);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Teller is " + teller);
                    teller++;
                    if (teller>3){
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


    public void failureCheckListener() {
        System.out.println("Starting FailureCheckListener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(CHECKPORT);
            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);

                byte[] response = "OK".getBytes();
                DatagramPacket reply = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
                datagramSocket.send(reply);
            }
        } catch (IOException e) {
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
