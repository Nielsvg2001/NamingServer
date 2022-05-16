package fileManager.Node;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.net.Inet4Address;

public class FileManager {
    FileTransfer fileTransfer;
    NetworkManager networkManager;
    public static int EDGEPORT = 9995;

    public FileManager(NetworkManager networkManager) {
        fileTransfer = new FileTransfer(networkManager);
        this.networkManager = networkManager;
        startUp();
        new Thread(this::shutdownListener).start();
    }

    public void startUp(){
        System.out.println("startup files");
        File path = new File("src/main/java/fileManager/Node/Local_files");
        File [] files = path.listFiles();
        System.out.println(Arrays.toString(files));
        if(files!= null) {
            for (File file : files) {
                if (file.isFile()) { //this line weeds out other directories/folders
                    System.out.println(file);
                    System.out.println(namingRequest(Node.hashCode(file.getName())));
                    Inet4Address nodeIp = namingRequest(Node.hashCode(file.getName()));
                    try{
                        if(nodeIp != InetAddress.getLocalHost()){
                            fileTransfer.sendFile(nodeIp,file);
                        }
                        else if (Inet4Address.getLocalHost() != networkManager.getPreviousIP()){
                            fileTransfer.sendFile(networkManager.getPreviousIP(),file);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
    }


    public Inet4Address namingRequest(int hash) {
        System.out.println("request");
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NetworkManager.NAMINGSERVERADDRESS + ":" + NetworkManager.NAMINGPORT + "/namingRequest")
                .queryString("hash", hash)
                        .asObject(Inet4Address.class);
        return response.getBody();
    }

    public void shutdown() {
        System.out.println("shutdown! sending replicated files to previous node");
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();

        assert files != null;
        for (File file : files) {
            String fileName = file.getName();
            try {
                Inet4Address IP = checkIsALocalFile(fileName);
                fileTransfer.sendFile(IP, file);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public Inet4Address checkIsALocalFile(String fileName) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();

            byte[] bytes = fileName.getBytes();
            while (true) {
                try {
                    //sending file name to previous node
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, networkManager.getPreviousIP(), EDGEPORT);
                    datagramSocket.send(packet);

                    packet = new DatagramPacket(new byte[256], 256);
                    datagramSocket.receive(packet);
                    byte[] bytes1_= packet.getData();
                    String IP = Arrays.toString(bytes1_);
                    Inet4Address inet4Address = (Inet4Address) Inet4Address.getByName(IP);
                    System.out.println("Adres: " + inet4Address);
                    return inet4Address;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SocketException | RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownListener() {
        System.out.println("Listen for packets on edge port");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(EDGEPORT);
            while (true) {
                Thread thread = new Thread(() -> {
                    try {
                        // previous node checks if he has the file as a local file
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(datagramPacket);
                        byte[] bytes = datagramPacket.getData();
                        String fileName = bytes.toString();

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

                        DatagramSocket socket = new DatagramSocket();
                        try {
                            // if he already has the local file, send ip address of previous node to the sender
                            if (isALocalFile) {
                                bytes = networkManager.getPreviousIP().toString().getBytes();
                                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, datagramPacket.getAddress(), datagramPacket.getPort());
                                socket.send(packet);
                            }
                            // if he doesn't have the local file, send ip address of current node to the sender
                            else {
                                bytes = InetAddress.getLocalHost().toString().getBytes();
                                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, datagramPacket.getAddress(), datagramPacket.getPort());
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
