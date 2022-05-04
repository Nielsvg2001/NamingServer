package fileManager.Node;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;
import java.net.Inet4Address;

public class FileManager {
    FileTransfer fileTransfer;

    public FileManager() {
        fileTransfer = new FileTransfer();
        startUp();
    }

    public void startUp(){
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
}
