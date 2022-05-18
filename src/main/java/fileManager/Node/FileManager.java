package fileManager.Node;

import com.google.gson.JsonObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.net.Inet4Address;

/**
 * class that mangages the files
 */
public class FileManager {
    FileTransfer fileTransfer;
    NetworkManager networkManager;

    public static int EDGEPORT = 9995;

    /**
     * constructor
     * @param networkManager manager that manages all the network stuff
     */
    public FileManager(NetworkManager networkManager) {
        fileTransfer = new FileTransfer(networkManager); // to transfer the files
        this.networkManager = networkManager;
        startUp();
        new Thread(this::shutdownListener).start();
    }

    /**
     * Startup: scans all the files in the Local_files folder and send them to another node to replicate
     */
    public void startUp(){
        System.out.println("startup files");
        File path = new File("src/main/java/fileManager/Node/Local_files");
        File [] files = path.listFiles();
        System.out.println("array of files" + Arrays.toString(files));
        if(files!= null) {
            for (File file : files) {
                if (file.isFile()) { //this line weeds out other directories/folders
                    System.out.println(file);
                    System.out.println(namingRequest(Node.hashCode(file.getName())));
                    //get IP address of replicated node of that file
                    Inet4Address nodeIp = namingRequest(Node.hashCode(file.getName()));
                    try{
                        // if the normal replicated node of this file is not this node
                        if(nodeIp != InetAddress.getLocalHost()){
                            fileTransfer.sendFile(nodeIp,file, Node.hashCode(Inet4Address.getLocalHost().getHostName()));
                        }
                        // if the normal replicated node of this file is this host, the file is send to the previous node
                        else if (Inet4Address.getLocalHost() != networkManager.getPreviousIP()){
                            fileTransfer.sendFile(networkManager.getPreviousIP(),file,  Node.hashCode(Inet4Address.getLocalHost().getHostName()));
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * send REST request to Naming to get the IP address of the node that must have the file with hash
     * @param hash hash of file
     * @return Inet4Address of the node where the file must be
     */
    public Inet4Address namingRequest(int hash) {
        System.out.println("request");
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NetworkManager.NAMINGSERVERADDRESS + ":" + NetworkManager.NAMINGPORT + "/namingRequest")
                .queryString("hash", hash)
                        .asObject(Inet4Address.class);
        return response.getBody();
    }

    /**
     * Shutdown function
     * sends all the replicated files to the previous node (or the node before if the previous node has this file as local file)
     */
    public void shutdown() {
        System.out.println("shutdown! sending replicated files to previous node");
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();
        assert files != null;
        for (File file : files) {
            String fileName = file.getName();
            try {
                Inet4Address IP = checkIsALocalFile(fileName);
                JSONObject log = fileTransfer.getLogHandler().removeFileLog(fileName, "replicated");
                System.out.println(log.toJSONString());
                int hostnamehash = (int) log.get("downloadlocation");
                fileTransfer.sendFile(IP, file, hostnamehash);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * checkIsALocalFile checks if the file is a local file of the previous IP, if it is, it returns the IP of the previous node of the previous node
     * @param fileName file that will get checked
     * @return IP address where to send the file
     */
    public Inet4Address checkIsALocalFile(String fileName) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            byte[] bytes = fileName.getBytes();
                try {
                    //sending file name to previous node
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, networkManager.getPreviousIP(), EDGEPORT);
                    datagramSocket.send(packet);
                    // listen to response
                    packet = new DatagramPacket(new byte[256], 256);
                    datagramSocket.receive(packet);
                    byte[] bytes1_= packet.getData();
                    String IP = new String(bytes1_, 0, packet.getLength());
                    Inet4Address inet4Address = (Inet4Address) Inet4Address.getByName(IP);
                    return inet4Address;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        } catch (SocketException | RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * listens to files send by a node that calls shutdown
     * sends back IP address of itself or of his previous node depending if the file is his local file
     */
    public void shutdownListener() {
        System.out.println("Listen for packets on edge port");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(EDGEPORT);
            while (!datagramSocket.isClosed()) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                Thread thread = new Thread(() -> {
                    try {
                        // previous node checks if he has the file as a local file
                        datagramSocket.receive(datagramPacket);
                        byte[] bytes = datagramPacket.getData();
                        String fileName = Arrays.toString(bytes);
                        boolean isALocalFile = false;
                        File path = new File("src/main/java/fileManager/Node/Local_files");
                        File[] files = path.listFiles();
                        assert files != null;
                        for (File file: files) {
                            if (Node.hashCode(file.getName()) == Node.hashCode(fileName)) {
                                isALocalFile = true;
                                break;
                            }
                        }
                        //send response
                        DatagramSocket socket = new DatagramSocket();
                        try {
                            // if he already has the local file, send ip address of previous node to the sender
                            if (isALocalFile) {
                                String IP = networkManager.getPreviousIP().getHostAddress();
                                byte[] bytes1 = IP.getBytes();
                                DatagramPacket packet = new DatagramPacket(bytes1, bytes1.length, datagramPacket.getAddress(), datagramPacket.getPort());
                                socket.send(packet);
                            }
                            // if he doesn't have the local file, send ip address of current node to the sender
                            else {
                                String IP = Inet4Address.getLocalHost().getHostAddress();
                                byte[] bytes1 = IP.getBytes();
                                DatagramPacket packet = new DatagramPacket(bytes1, bytes1.length, datagramPacket.getAddress(), datagramPacket.getPort());
                                socket.send(packet);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

}
