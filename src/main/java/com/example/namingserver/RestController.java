package com.example.namingserver;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.Inet4Address;
import java.net.UnknownHostException;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    public RestController() {
    }

    @PostMapping("/addNode")
    @ResponseStatus(HttpStatus.CREATED)
    public String addNode(@RequestParam String nodeName, @RequestParam String nodeIP) {
        try {
            int hash = Naming.addNode(nodeName, (Inet4Address) Inet4Address.getByName(nodeIP));
            return (hash == -1) ? "Node already exists, remove him first" : "Node " + nodeName + " added with hash " + hash;
        } catch (UnknownHostException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong IP address");
        }
    }

    @DeleteMapping("/removeNode")
    @ResponseStatus(HttpStatus.OK)
    public String removeNode(@RequestParam String nodeName) {
        int hash = Naming.removeNode(nodeName);
        return (hash == -1) ? "Node doesn't exist" : "Node " + nodeName + " removed with hash " + hash;
    }

    @GetMapping(value = "/namingRequest")
    public Inet4Address NamingRequest(@RequestParam String fileName) {
        return Naming.getRequest(fileName);
    }

    @GetMapping(value = "/getNodeInfo")
    public Inet4Address getNodeInfo(@RequestParam int id) {
        return Naming.getNodeInfo(id);
    }


}
