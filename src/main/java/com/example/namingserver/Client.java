package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class Client {

    public static void main(String[] args) throws UnknownHostException {
        Client cl = new Client();

        InetAddress address = InetAddress.getLocalHost();
        cl.addNode(address);
        cl.NamingRequest("testfile name.txt");
        cl.removeNode("testnodename");

    }


    public void addNode(InetAddress ipaddr){
        System.out.println("addnode");
        System.out.println("nodeName"+ ipaddr.getHostName());
        System.out.println("nodeName"+ ipaddr.getHostAddress());
        HttpResponse<String> response = Unirest.post("http://host0.group6.6dist:8080/addNode")
                .queryString("nodeName", ipaddr.getHostName())
                .queryString("nodeIP", ipaddr.getHostAddress())
                .asString();
    }


    public void removeNode(String nodeName) {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://host0.group6.6dist:8080/removeNode")
                .queryString("nodeName",nodeName )
                .asString();
    }

    public void NamingRequest(String fileName) {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://host0.group6.6dist:8080/namingRequest")
                .queryString("fileName", fileName)
                .asString();
        System.out.println("response: " +response);
    }


}
