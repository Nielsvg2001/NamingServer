package com.example.namingserver;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.Map;
import java.util.Objects;

public class Discovery {
    private static final int PORT = 9999;

    public Discovery() {
    }

    public static void start() {
        System.out.println("Starting Discovery");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(packet);
                        String hostname = new String(packet.getData(), 0, packet.getLength());

                        Naming.addNode(hostname, (Inet4Address) packet.getAddress());

                        Integer hash = Naming.hashCode(hostname);
                        Integer  previousNode = Naming.getNodesList().lowerKey(hash);
                        Integer nextNode = Naming.getNodesList().higherKey(hash);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("numberOfNodes", Naming.numberOfNodes());
                        jsonObject.put("previousNode", (previousNode == null) ? Naming.getNodesList().lastKey() : previousNode);
                        jsonObject.put("nextNode", (nextNode == null) ? Naming.getNodesList().firstKey() : nextNode);

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