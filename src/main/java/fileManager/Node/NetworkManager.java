package fileManager.Node;

import fileManager.NamingServer.Naming;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.*;


public class NetworkManager {

    public Node node;
    public String hostName;
    private int currentID;
    private InetAddress ipAddress;
    private int previousNode;
    private int nextNode;
    public static final int DISCOVERYPORT = 9999;
    public static final String NAMINGPORT = "8080";
    public static int SHUTDOWNPORT = 9998;
    public static int FAILUREPORT = 9997;
    public static int CHECKPORT = 9987;
    public static String NAMINGSERVERADDRESS = "localhost";
    private final String MULTICASTADDRESS = "230.0.0.1";
    private MulticastSocket msocket;
    private InetAddress multicastGroup;

    /**
     * constructor of NetworkManager
     *
     * @param node the node is passed so that the NetworkManager can acces all functions (also the functions of FileManager and FileTransfer)
     */
    public NetworkManager(Node node) {
        this.node = node;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            ipAddress = InetAddress.getLocalHost();
            currentID = Node.hashCode(hostName);
            multicastGroup = InetAddress.getByName(MULTICASTADDRESS);
        } catch (UnknownHostException e) {
            System.out.println("Could not get LocalHost information: " + e.getMessage());
        }
        // start services
        int numNodesWhenEntered = dicovery();
        new Thread(this::listenForNewNodes).start();
        new Thread(this::shutdownListener).start();
        new Thread(this::checkNeighbors).start();
        new Thread(this::failureCheckListener).start();

        System.out.println("I'm node " + hostName + "(" + currentID + ")" + " and my ip is " + ipAddress);
        System.out.println("There are " + numNodesWhenEntered + " nodes in the network \nThe previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
    }

    /**
     * Add Node to the network
     * sends POST request to REST server
     *
     * @param ipaddr Ip adress of new node
     */
    public void addNode(InetAddress ipaddr) {
        System.out.println("addnode");
        System.out.println("nodeName" + ipaddr.getHostName());
        System.out.println("nodeName" + ipaddr.getHostAddress());
        HttpResponse<String> response = Unirest.post("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/addNode")
                .queryString("nodeName", ipaddr.getHostName())
                .queryString("nodeIP", ipaddr.getHostAddress())
                .asString();
    }

    /**
     * remove Node from the network
     * sends DELETE request to REST server
     *
     * @param nodeName String name of node
     */
    public void removeNode(String nodeName) {
        System.out.println("removenode");
        HttpResponse<String> response = Unirest.delete("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/removeNode")
                .queryString("nodeName", nodeName)
                .asString();
    }

    /**
     * returns the Inet4Addres of the node with id
     *
     * @param id Int hash of the hostname
     * @return Inet4Address IP address of node
     */
    public Inet4Address getNodeInfo(int id) {
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NAMINGSERVERADDRESS + ":" + NAMINGPORT + "/getNodeInfo")
                .queryString("id", id)
                .asObject(Inet4Address.class);
        return response.getBody();
    }

    /**
     * @return Inet4Address IP address of previous node
     */
    public Inet4Address getPreviousIP() {
        return getNodeInfo(previousNode);
    }

    /**
     * sends a multicast to all nodes in the network to join the network
     * then it get a response from the namingserver with the previousNode and nextnode and set these variables
     *
     * @return number of nodes in the network
     */
    public int dicovery() {
        try {
            // Send hostname (+ ip) to naming server and other nodes.
            msocket = new MulticastSocket();
            byte[] buf = hostName.getBytes();
            msocket.joinGroup(multicastGroup);
            DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, multicastGroup, DISCOVERYPORT);
            datagramPacket.setAddress(multicastGroup);
            msocket.send(datagramPacket);

            // Receive response from naming server
            datagramPacket = new DatagramPacket(new byte[1024], 1024);
            msocket.receive(datagramPacket);

            // Handle received data
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));
            NAMINGSERVERADDRESS = datagramPacket.getAddress().getHostAddress();
            previousNode = Integer.parseInt(jsonObject.get("previousNode").toString());
            nextNode = Integer.parseInt(jsonObject.get("nextNode").toString());
            System.out.println("In discovery: The previous node is " + previousNode + " and the next node is " + nextNode);
            return Integer.parseInt(jsonObject.get("numberOfNodes").toString());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * listens to multicasts from nodes that join the network and if it receives a packet, the node checks if it must update the previous or next node
     */
    public void listenForNewNodes() {
        System.out.println("Starting listenForNewNodes");
        try {
            // join multicastgroup
            msocket = new MulticastSocket(DISCOVERYPORT);
            msocket.joinGroup(multicastGroup);
            while (!msocket.isClosed()) {
                //receive packet
                DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                msocket.receive(packet);
                //create new thread for every packet
                Thread thread = new Thread(() -> {
                    //if it is the only node in the network set onlynode
                    boolean onlynode = nextNode == previousNode && nextNode == currentID;
                    //get hostname of node where the packet came from
                    String hostname = new String(packet.getData(), 0, packet.getLength());
                    int hash = Node.hashCode(hostname);
                    //set new nextnode if the newnode is between the nextnode and the current id
                    //or if the current node has the biggest hash, then  the new nextnode is set if the new node has the biggest or smallest hash
                    if ((hash < nextNode && hash > currentID) || (nextNode <= currentID && hash > currentID) || (nextNode <= currentID && hash < nextNode)) {
                        nextNode = hash;
                        checkReplicationValidity(hash, (Inet4Address) packet.getAddress()); // check if new node should be the owner of replicated files in this node
                    }
                    //set new previousnode if the newnode is between the previousnode and the current id
                    // or if the currentnode has the smallest hash, then the previousnode is set if the newnode has the smallest or the biggest hash
                    if ((hash > previousNode && hash < currentID) || (previousNode >= currentID && hash < currentID) || (previousNode >= currentID && hash > previousNode)) {
                        previousNode = hash;
                    }
                    System.out.println("In listenForNewNodes: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
                    //if the node was the only node in the network and now there is another nextnode, it has to send its local files to be replicated
                    if (onlynode && nextNode != currentID) {
                        //sleep because other node is still starting up
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        node.sendReplicatedfiles();
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * check for every file of replicated_files if it is smaller than the hash of the new node -> then new node becomes owner of these files
     *
     * @param insertedNodeHash hash of hostname new node
     * @param address          Inet4Address IP address of new node
     */
    public void checkReplicationValidity(int insertedNodeHash, Inet4Address address) {
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();
        assert files != null;
        // check if replicated file hashes are closer to hash of inserted node than the hash of the owner
        for (File file : files) {
            // here fout, kijken naar namingserver -> ni geraken bij Naming
            if (insertedNodeHash < Node.hashCode(file.getName()) | insertedNodeHash == Naming.getNodesList().lastKey()) {
                //newnode.getip==node.fileManager.namingRequest(Node.hashCode(file.getName()));
                LogHandler logHandler = node.fileManager.fileTransfer.getLogHandler();
                JSONObject log = logHandler.removeFileLog(file.getName(), "replicated"); // remove file from the log file
                int hostnamehash = (int) log.get("downloadlocation");
                node.fileManager.fileTransfer.sendFile(address, file, hostnamehash); // send file to node
                // delete file out of location
                if (!file.delete()) {
                    System.out.println("error deleting file in checkReplicationValidity");
                }
            }
        }
        path = new File("src/main/java/fileManager/Node/Local_files");
        files = path.listFiles();
        assert files != null;
        for (File file : files) {
            Inet4Address ip = node.fileManager.namingRequest(Node.hashCode(file.getName()));
            // if normal replicated node is itself or new next node
            if (ip.equals(address) | ip.equals(node.networkManager.getNodeInfo(nextNode))) {
                // move file from previous node to new inserted (next) node
                if(previousNode != currentID | previousNode == nextNode) {
                    node.fileManager.fileTransfer.sendDeleteMessage(node.networkManager.getPreviousIP(), file);
                    node.fileManager.fileTransfer.sendFile(node.networkManager.getNodeInfo(nextNode), file, currentID);
                }
            }
        }
    }

    /**
     * shutdown: sends nextnode to previousnode
     * sends previousnode to nextnode
     * remove hostname from list of nodes in Namingserver
     */
    public void shutdown() {
        System.out.println("shutdown");
        try {
            DatagramSocket socket = new DatagramSocket();
            try {
                // Sending nextNode to previousNode
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("newNextNode", nextNode);
                byte[] buf = jsonObject.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), SHUTDOWNPORT);
                socket.send(packet);

                // Sending previousNode to nextNode
                jsonObject = new JSONObject();
                jsonObject.put("newPreviousNode", previousNode);
                buf = jsonObject.toString().getBytes();
                packet = new DatagramPacket(buf, buf.length, getNodeInfo(nextNode), SHUTDOWNPORT);
                socket.send(packet);
                msocket.leaveGroup(multicastGroup);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // Remove this node from the list of nodes
        removeNode(hostName);
    }


    /**
     * listens to shutdown packets of neighbors or Namingserver (if a node has failed) and updates the next or previous node
     */
    public void shutdownListener() {
        System.out.println("Starting Shutdown Listener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(SHUTDOWNPORT);
            while (!datagramSocket.isClosed()) {
                //receive packet
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                System.out.println("shutdown received packet");
                //create new thread for every new packet
                Thread thread = new Thread(() -> {
                    try {
                        if (datagramPacket.getAddress() != ipAddress) {
                            // Handle received data
                            JSONParser parser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) parser.parse(new String(datagramPacket.getData(), 0, datagramPacket.getLength()));

                            // update previousNode
                            if (jsonObject.containsKey("newPreviousNode")) {
                                previousNode = Integer.parseInt(jsonObject.get("newPreviousNode").toString());
                            }

                            // update nextNode
                            if (jsonObject.containsKey("newNextNode")) {
                                nextNode = Integer.parseInt(jsonObject.get("newNextNode").toString());
                            }
                            System.out.println("In shutdonwListener: The previous node is " + previousNode + " (" + getNodeInfo(previousNode) + ") and the next node is " + nextNode + " (" + getNodeInfo(nextNode) + ")");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the neighbors by sending UDP packets every 4s and waiting for a response
     */
    public void checkNeighbors() {
        System.out.println("Checking for failure...");
        int teller = 0;
        try {
            DatagramSocket socket = new DatagramSocket();

            byte[] buf = "test".getBytes();
            while (!socket.isClosed()) {
                try {
                    socket.setSoTimeout(100);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, getNodeInfo(previousNode), CHECKPORT);
                    socket.send(packet);
                    packet = new DatagramPacket(new byte[256], 256);
                    socket.receive(packet);
                    teller = 0;
                    String packetString = new String(packet.getData(), 0, packet.getLength());
                    if (!packetString.equals("OK")) {
                        failure(previousNode);
                    }
                    //after 100ms a timeout exception is trown
                } catch (SocketTimeoutException e) {
                    System.out.println("Teller is " + teller);
                    teller++;
                    // if teller reaches 11 failure is called
                    if (teller > 10) {
                        failure(previousNode);
                        System.out.println("In checkNeighbors: Aanroepen failure");
                        Thread.sleep(2000);
                    }
                }
                Thread.sleep(4000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            failure(previousNode);
        }
    }


    /**
     * listen for UDP packets to check if the node is still working and sending a response
     */
    public void failureCheckListener() {
        System.out.println("Starting FailureCheckListener");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(CHECKPORT);
            while (!datagramSocket.isClosed()) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                //send response
                byte[] response = "OK".getBytes();
                DatagramPacket reply = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
                datagramSocket.send(reply);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * if failure is detected, the hostname is send to the namingserver
     *
     * @param hash hash of hostname that failed
     */
    public void failure(int hash) {
        System.out.println("Failure detected");
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            try {
                // sending hostname of failed node to the Naming server
                byte[] buf = String.valueOf(hash).getBytes();
                System.out.println("In failure: " + hostName);
                DatagramPacket datagramPacket = new DatagramPacket(buf, 0, buf.length, InetAddress.getByName(NAMINGSERVERADDRESS), FAILUREPORT);
                datagramSocket.send(datagramPacket);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
