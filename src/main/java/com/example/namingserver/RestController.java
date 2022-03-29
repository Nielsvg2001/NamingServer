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
            return "Node " + nodeName + " added with hash " + hash;
        } catch (UnknownHostException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong IP address");
        }
    }

    @GetMapping(value = "/namingRequest")
    public Inet4Address NamingRequest(@RequestParam String fileName) {
        return Naming.getRequest(fileName);
    }


}
