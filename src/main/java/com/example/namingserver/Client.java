package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;


public class Client {
    public static final int LISTENPORT = 9999;
    public static final String NAMINGPORT = "8080";
    public String NAMINGSERVERADDRESS = "localhost";
    //public String NAMINGSERVERADDRESS = "host0.group6.6dist";
    private int previousNode;
    private int nextNode;
    private int hashThisNode;


    public static void main(String[] args) throws IOException {
        Client cl = new Client();
        System.out.println("There are " + cl.dicovery() + " nodes in the network \nThe previous node is " + cl.previousNode + " and the next node is " + cl.nextNode);
        cl.Listen();
        InetAddress address = InetAddress.getLocalHost();
        cl.addNode(address);
        cl.namingRequest("testfile name.txt");
        cl.removeNode("testnodename");

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
            previousNode = Integer.parseInt(jsonObject.get("previousNode").toString());
            nextNode = Integer.parseInt(jsonObject.get("nextNode").toString());
            return Integer.parseInt(jsonObject.get("numberOfNodes").toString());

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

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
                        if (hash < nextNode && hash > hashThisNode) {
                            nextNode = hash;
                        }

                        // here nog previousNode toevoegen + zorgen dat hashTHisnode gevuld wordt


                        /*Naming.addNode(hostname, (Inet4Address) packet.getAddress());

                        byte[] numNodes = String.valueOf(Naming.numberOfNodes()).getBytes();
                        DatagramPacket reply = new DatagramPacket(numNodes, numNodes.length, packet.getAddress(), packet.getPort());
                        reply.setData(String.valueOf(Naming.numberOfNodes()).getBytes());
                        datagramSocket.send(reply);

                         */
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


}
