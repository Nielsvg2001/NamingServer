package fileManager;

import fileManager.NamingServer.Naming;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    public RestController() {
    }

    /**
     * receive REST Post request /addNode
     * then hashes the name and add it to the namingslist in Naming.java
     * @param nodeName name of node that must be added
     * @param nodeIP ip of node that must be added
     * @return String with error if node already exists or with confirmation that node is added
     */
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

    /**
     * Receive Rest Delete request /removeNode
     * removes node from the naminglist
     * @param nodeName name of node that must be removed
     * @return String with error if node was not in list or confirmation that node is deleted
     */
    @DeleteMapping("/removeNode")
    @ResponseStatus(HttpStatus.OK)
    public String removeNode(@RequestParam String nodeName) {
        int hash = Naming.removeNode(nodeName);
        return (hash == -1) ? "Node doesn't exist" : "Node " + nodeName + " removed with hash " + hash;
    }

    /**
     * receive REST Get request /namingRequest
     * @param hash hash value of file
     * @return Inet4Addres IP address of node where the file must be normally
     */
    @GetMapping(value = "/namingRequest")
    public Inet4Address NamingRequest(@RequestParam int hash) {
        return Naming.getRequest(hash);
    }

    /**
     * receive REST Get request /getNodeInfo
     * gets the address over the node with the hash
     * @param id hash of node
     * @return Inet4Address: IP addres of node that has id as hash
     */
    @GetMapping(value = "/getNodeInfo")
    public Inet4Address getNodeInfo(@RequestParam int id) {
        return Naming.getNodeInfo(id);
    }

}
