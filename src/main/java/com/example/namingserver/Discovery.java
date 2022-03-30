package com.example.namingserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;

public class Discovery {

    public static final int PORT = 9999;

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
                        
                        byte[] numNodes = String.valueOf(Naming.numberOfNodes()).getBytes();
                        DatagramPacket reply = new DatagramPacket(numNodes, numNodes.length, packet.getAddress(), packet.getPort());
                        reply.setData(String.valueOf(Naming.numberOfNodes()).getBytes());
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