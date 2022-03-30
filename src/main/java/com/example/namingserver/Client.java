package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.IOException;
import java.net.*;


public class Client {

    public static void main(String[] args) throws IOException {
        Client cl = new Client();
        cl.Dicovery();
        cl.addNode();
        cl.NamingRequest();
        cl.removeNode();

    }


    public void addNode(){
        System.out.println("addnode");
        HttpResponse<String> response = Unirest.post("http://localhost:8080/addNode")
                .queryString("nodeName", "testnodename")
                .queryString("nodeIP", "8.7.6.5")
                .asString();
    }


    public void removeNode() {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://localhost:8080/removeNode")
                .queryString("nodeName", "testnodename")
                .asString();
    }

    public void NamingRequest() {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://localhost:8080/namingRequest")
                .queryString("fileName", "testfilename")
                .asString();
        System.out.println(response);
    }

    public int Dicovery() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = "clienthost".getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, InetAddress.getByName("255.255.255.255"),9999);
        socket.send(datagramPacket);
        System.out.println("send");
        socket.receive(datagramPacket);
        System.out.println("received");
        System.out.println(new String(datagramPacket.getData()));
        return Integer.parseInt(new String(datagramPacket.getData()));
    }


}
