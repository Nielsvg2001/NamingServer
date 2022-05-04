package fileManager.Node;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;

public class FileManager {
    FileTransfer fileTransfer;

    public FileManager() {
        fileTransfer = new FileTransfer();
    }

    public void startUp(){
        fileTransfer.receive();
        File path = new File("Local_files");
        File [] files = path.listFiles();
        if(files!= null) {
            for (File file : files) {
                if (file.isFile()) { //this line weeds out other directories/folders
                    System.out.println(file);
                    System.out.println(namingRequest(file.getName()));
                    fileTransfer.sendFile(namingRequest(file.getName()),file);
                }
            }
        }
    }


    public Inet4Address namingRequest(String fileName) {
        System.out.println("request");
        HttpResponse<Inet4Address> response = Unirest.get("http://" + NetworkManager.NAMINGSERVERADDRESS + ":" + NetworkManager.NAMINGPORT + "/namingRequest")
                .queryString("fileName", fileName)
                        .asObject(Inet4Address.class);
        System.out.println("responsebody: " + response.getBody());
        return response.getBody();
    }

    public void shutdown() {
        System.out.println("shutdown");
        File path = new File("Replicated_files");
        File [] files = path.listFiles();
        fileTransfer.sendFile(, files);
        System.exit(0);
    }

}
