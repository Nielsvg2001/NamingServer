package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.IOException;
import java.net.*;


public class Client {
    public static final int LISTENPORT = 9999;
    public static final String NAMINGPORT = "8080";
    public String NAMINGSERVERADDRESS = "localhost";
    //public String NAMINGSERVERADDRESS = "host0.group6.6dist";

    public static void main(String[] args) throws UnknownHostException, IOException {
        Client cl = new Client();
        System.out.println("There are " + cl.Dicovery() + " nodes in the network");
        cl.Listen();
        InetAddress address = InetAddress.getLocalHost();
        cl.addNode(address);
        cl.NamingRequest("testfile name.txt");
        cl.removeNode("testnodename");

    }


    public void addNode(InetAddress ipaddr){
        System.out.println("addnode");
        System.out.println("nodeName"+ ipaddr.getHostName());
        System.out.println("nodeName"+ ipaddr.getHostAddress());
        HttpResponse<String> response = Unirest.post("http://"+NAMINGSERVERADDRESS+":"+NAMINGPORT+"/addNode")
                .queryString("nodeName", ipaddr.getHostName())
                .queryString("nodeIP", ipaddr.getHostAddress())
                .asString();
    }


    public void removeNode(String nodeName) {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://"+NAMINGSERVERADDRESS+":"+NAMINGPORT+"/removeNode")
                .queryString("nodeName",nodeName )
                .asString();
    }

    public void NamingRequest(String fileName) {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://"+NAMINGSERVERADDRESS+":"+NAMINGPORT+"/namingRequest")
                .queryString("fileName", fileName)
                .asString();
        System.out.println("responsebody: " +response.getBody());
    }

    public int Dicovery() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = Inet4Address.getLocalHost().getHostName().getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, InetAddress.getByName("255.255.255.255"),9999);
        socket.send(datagramPacket);
        socket.receive(datagramPacket);
        return Integer.parseInt(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));
    }

    public void Listen(){
        System.out.println("Starting Listening");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(LISTENPORT);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(packet);
                        String hostname = new String(packet.getData(), 0, packet.getLength());

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
