package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;


public class Node {
    public static InetAddress address;
    public static final int LISTENPORT = 9999;
    public String NAMINGSERVERADDRESS = "localhost";
    public static final String NAMINGPORT = "8080";
    public static int SHUTDOWNPORT = 9998;

    private static int previousNode_id;
    private static int nextNode_id;
    private static InetAddress previousNode_ip;
    private static InetAddress nextNode_ip;

    private static int hashThisNode;


    public static void main(String[] args) throws IOException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        Node cl = new Node();
        address = InetAddress.getLocalHost();
        System.out.println("I'm node " + hashCode(address.getHostName() + "and my ip is " + address.getHostAddress()));
        System.out.println("There are " + cl.dicovery() + " nodes in the network \nThe previous node is " + previousNode_id + " (" + previousNode_ip + ") and the next node is " + nextNode_id + " (" + nextNode_ip + ")");
        hashThisNode = hashCode(address.getHostName());
        cl.Listen();
        // cl.addNode(address);
        cl.namingRequest("testfile name.txt");
        //cl.removeNode("testnodename");

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
            System.out.println("Naming address: " + datagramPacket.getAddress().getHostAddress());
            NAMINGSERVERADDRESS = datagramPacket.getAddress().getHostAddress();
            previousNode_id = Integer.parseInt(jsonObject.get("previousNode_id").toString());
            nextNode_id = Integer.parseInt(jsonObject.get("nextNode_id").toString());
            previousNode_ip = InetAddress.getByName(jsonObject.get("previousNode_ip").toString());
            nextNode_ip = InetAddress.getByName(jsonObject.get("nextNode_id").toString());
            System.out.println("In discovery: The previous node is " + previousNode_id + " (" + previousNode_ip + ") and the next node is " + nextNode_id + " (" + nextNode_ip + ")");
            return Integer.parseInt(jsonObject.get("numberOfNodes").toString());

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // listens and if it receives a packet, the node checks if it must update the previous or next node
    public void Listen() {
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
                        if ((hash < nextNode_id && hash > hashThisNode) || (nextNode_id <=hashThisNode && hash>hashThisNode) || (nextNode_id <=hashThisNode && hash< nextNode_id)) {
                            nextNode_id = hash;
                            nextNode_ip = packet.getAddress();
                        }
                        if ((hash> previousNode_id && hash<hashThisNode) ||(previousNode_id >=hashThisNode && hash<hashThisNode) || (previousNode_id >=hashThisNode && hash> previousNode_id)) {
                            previousNode_id = hash;
                            previousNode_ip = packet.getAddress();
                        }
                        System.out.println("In Listen: The previous node is " + previousNode_id + " (" + previousNode_ip + ") and the next node is " + nextNode_id + " (" + nextNode_ip + ")");
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
                jsonObject.put("newNextNode_id", nextNode_id);
                jsonObject.put("newNextNode_ip", nextNode_ip.getHostAddress());
                byte[] buf = jsonObject.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, previousNode_ip, SHUTDOWNPORT);
                socket.send(packet);

                // Sending previousNode to nextNode
                jsonObject = new JSONObject();
                jsonObject.put("newPreviousNode_id", previousNode_id);
                jsonObject.put("newPreviousNode_ip", previousNode_ip.getHostAddress());
                buf = jsonObject.toString().getBytes();
                packet = new DatagramPacket(buf, buf.length, nextNode_ip, SHUTDOWNPORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // Remove this node from the list of nodes
        removeNode(address.getHostName());
    }


    public static void shutdownListener(){
        System.out.println("Starting Shutdown Listener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(SHUTDOWNPORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(datagramPacket);
                        if (datagramPacket.getAddress() != address) {
                            // Handle received data
                            JSONParser parser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));

                            // update previousNode
                            if (jsonObject.containsKey("newPreviousNode_id")) {
                                previousNode_id = Integer.parseInt(jsonObject.get("newPreviousNode_id").toString());
                                previousNode_ip = InetAddress.getByName(jsonObject.get("newPreviousNode_ip").toString());
                            }

                            // update nextNode
                            if (jsonObject.containsKey("newNextNode_id")) {
                                nextNode_id = Integer.parseInt(jsonObject.get("newNextNode_id").toString());
                                nextNode_ip = InetAddress.getByName(jsonObject.get("newNextNode_ip").toString());
                            }
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
