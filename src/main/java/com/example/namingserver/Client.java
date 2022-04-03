package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.IOException;
import java.net.InetAddress;


public class Client {
    public static final String NAMINGPORT = "8080";
    public String NAMINGSERVERADDRESS = "localhost";
    // public String NAMINGSERVERADDRESS = "host0.group6.6dist";

    public static void main(String[] args) throws IOException, InterruptedException {
        Client cl = new Client();
        InetAddress localHost = InetAddress.getLocalHost();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        // Add a node with a unique node name
        System.out.println("Add a node with a unique node name");
        cl.addNode(localHost);
        Thread.sleep(2000);

        // Add a node with an existing node name
        System.out.println("Add a node with an existing node name");
        cl.addNode(localHost);
        Thread.sleep(2000);

        // Add second node with unique node name
        System.out.println("Add second node with unique node name");
        InetAddress node = InetAddress.getByName("192.168.0.100");
        cl.addNode(node);
        Thread.sleep(2000);

        // Send a filename with a hash smaller than the smallest hash of the nodes.
        System.out.println("Send a filename with a hash smaller than the smallest hash of the nodes");
        cl.NamingRequest("testfile.txt");
        Thread.sleep(2000);

        // Remove a node from the MAP
        System.out.println("Remove a node from the MAP");
        cl.removeNode(localHost.getHostName());
        Thread.sleep(2000);
    }


    public void addNode(InetAddress localHost) {
        HttpResponse<String> response = Unirest.post("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/addNode")
                .queryString("nodeName", localHost.getHostName())
                .queryString("nodeIP", localHost.getHostAddress())
                .asString();
        System.out.println("responsebody: " + response.getBody());
    }


    public void removeNode(String nodeName) {
        HttpResponse<String> response = Unirest.delete("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/removeNode")
                .queryString("nodeName", nodeName)
                .asString();
        System.out.println("responsebody: " + response.getBody());
    }

    public void NamingRequest(String fileName) {
        HttpResponse<String> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/namingRequest")
                .queryString("fileName", fileName)
                .asString();
        System.out.println("responsebody: " + response.getBody());
    }
}