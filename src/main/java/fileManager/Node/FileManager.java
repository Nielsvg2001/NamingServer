package fileManager.Node;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class FileManager {

    public FileManager() {
    }


    public void namingRequest(String fileName) {
        System.out.println("request");
        HttpResponse<String> response = Unirest.get("http://" + NetworkManager.NAMINGSERVERADDRESS + ":" + NetworkManager.NAMINGPORT + "/namingRequest")
                .queryString("fileName", fileName)
                .asString();
        System.out.println("responsebody: " + response.getBody());
    }
}
