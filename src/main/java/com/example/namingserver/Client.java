package com.example.namingserver;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;


public class Client {

    public static void main(String[] args) {
        Client cl = new Client();
        cl.addNode();
        cl.NamingRequest();
        cl.removeNode();

    }


    public void addNode(){
        System.out.println("addnode");
        HttpResponse<String> response = Unirest.post("http://192.168.128.5:8080/addNode")
                .queryString("nodeName", "testnodename")
                .queryString("nodeIP", "8.7.6.5")
                .asString();
    }


    public void removeNode() {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://192.168.128.5:8080/removeNode")
                .queryString("nodeName", "testnodename")
                .asString();
    }

    public void NamingRequest() {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://192.168.128.5:8080/namingRequest")
                .queryString("fileName", "testfilename")
                .asString();
        System.out.println(response);
    }


}
