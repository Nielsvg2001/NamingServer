package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;


public class Node {
    public static String hostName;
    private static int currentID;
    private static InetAddress ipAddress;
    private static int numNodesWhenEntered;
    private static int previousNode;
    private static int nextNode;
    public static final int LISTENPORT = 9999;
    public static final String NAMINGPORT = "8080";
    public static int SHUTDOWNPORT = 9998;
    public String NAMINGSERVERADDRESS = "localhost";


    public static void main(String[] args) throws InterruptedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        Node cl = new Node();
        System.out.println("I'm node " + hostName + " and my ip is " + ipAddress);
        System.out.println("There are " + numNodesWhenEntered + " nodes in the network \nThe previous node is " + previousNode + " (" + cl.getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + cl.getNodeInfo(nextNode) + ")");
        cl.namingRequest("testfile name.txt");
        Thread.sleep(120000);
        cl.shutdown();

    }

    public Node() {
        // get own infromation
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            ipAddress = InetAddress.getLocalHost();
        }catch (UnknownHostException e) {
            System.out.println("Could not get LocalHost information: " + e.getMessage());
        }
        currentID = hashCode(hostName);

        // start services
        numNodesWhenEntered = dicovery();
        listenForNewNodes();
        shutdownListener();
    }

    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode() + 2147483648.0) * (32768 / (2147483648.0 + Math.abs(-2147483648.0))));
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
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/getNodeInfo")
                .queryString("id", id)
                .asObject(Inet4Address.class);
        return response.getBody();
    }

    public void namingRequest(String fileName) {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/namingRequest")
                .queryString("fileName", fileName)
                .asString();
        System.out.println("responsebody: " + response.getBody());
    }

    public int dicovery() {
        try {
            // Send hostname (+ ip) to naming server
            DatagramSocket socket = new DatagramSocket();
            byte[] buf = Inet4Address.getLocalHost().getHostName().getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, InetAddress.getByName("255.255.255.255"), 9999);
            socket.send(datagramPacket);

            // Receive a response
            datagramPacket = new DatagramPacket(new byte[1024], 1024);
            socket.receive(datagramPacket);

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
        System.out.println("Starting Listening");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(LISTENPORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(packet);
                        String hostname = new String(packet.getData(), 0, packet.getLength());
                        int hash = hashCode(hostname);
                        if ((hash < nextNode && hash > currentID) || (nextNode<= currentID && hash> currentID) || (nextNode<= currentID && hash<nextNode)) {
                            nextNode = hash;
                        }
                        if ((hash>previousNode && hash< currentID) ||(previousNode>= currentID && hash< currentID) || (previousNode>= currentID && hash>previousNode)) {
                            previousNode = hash;
                        }
                        System.out.println("In listen: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // Remove this node from the list of nodes
        removeNode(hostName);
    }


    public void shutdownListener(){
        System.out.println("Starting Shutdown Listener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(SHUTDOWNPORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(datagramPacket);
                        if (datagramPacket.getAddress() != ipAddress) {
                            // Handle received data
                            JSONParser parser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));

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
                    } catch(IOException | ParseException e){
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
