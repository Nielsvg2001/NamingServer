package com.example.namingserver;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.Map;

public class Discovery {
    private static final int PORT = 9999;
    private static final int SHUTDOWNPORT = 9998;

    public Discovery() {
    }

    public static void start() {
        System.out.println("Starting Discovery");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        // Receiving new node
                        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(packet);
                        String hostname = new String(packet.getData(), 0, packet.getLength());

                        // Adding new node to the list
                        Naming.addNode(hostname, (Inet4Address) packet.getAddress());

                        // Creating response
                        Integer hash = Naming.hashCode(hostname);
                        Map.Entry<Integer, Inet4Address> previousNode = Naming.getNodesList().lowerEntry(hash);
                        Map.Entry<Integer, Inet4Address> nextNode = Naming.getNodesList().higherEntry(hash);

                        // Structuring response
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("numberOfNodes", Naming.numberOfNodes());
                        jsonObject.put("previousNode_id", (previousNode == null) ? Naming.getNodesList().lastKey() : previousNode.getKey());
                        jsonObject.put("nextNode_id", (nextNode == null) ? Naming.getNodesList().firstKey() : nextNode.getKey());
                        jsonObject.put("previousNode_ip", (previousNode == null) ? Naming.getNodesList().lastEntry().getValue().getHostAddress() : previousNode.getValue().getHostAddress());
                        jsonObject.put("nextNode_ip", (nextNode == null) ? Naming.getNodesList().firstEntry().getValue().getHostAddress() : nextNode.getValue().getHostAddress());

                        // Send numberOfNodes, previousNode and nextNode to the new node
                        byte[] data = jsonObject.toJSONString().getBytes();
                        DatagramPacket reply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                        datagramSocket.send(reply);
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