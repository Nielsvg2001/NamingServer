package fileManager.Node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.nio.channels.FileLock;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

// https://jade.tilab.com/doc/api/jade/core/Agent.html
public class SyncAgent {
    private JSONArray listArray;
    private NetworkManager networkManager;
    private static int LISTPORT = 9994;

    public SyncAgent(NetworkManager networkManager) {
        listArray = new JSONArray();
        this.networkManager = networkManager;
        File path = new File("src/main/java/fileManager/Node/Local_files");
        File[] files = path.listFiles();
        System.out.println("Sync agent: replic files" + Arrays.toString(files));

        assert files != null;
        for (File file : files) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fileName", file.getName());
            System.out.println("Sync agent: file object" + jsonObject);
            listArray.add(jsonObject);
        }
        System.out.println("Sync agent: creating file list:" + listArray);
        new Thread(this::sync).start();
        new Thread(this::listenForFiles).start();
    }

    public void sync() {
        System.out.println("Sync agent: sync started!");
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            while (true) {
                Thread thread = new Thread(() -> {
                    try {
                        //update own list based on replication files. Necessary if file is added
                        //to the replicated folder of the node
                        File path = new File("src/main/java/fileManager/Node/Local_files");
                        File[] files = path.listFiles();

                        for (File file : files) {
                            for (Object object : listArray) {
                                JSONObject jsonObject = (JSONObject) object;
                                if (!file.getName().equals(jsonObject.get("fileName"))) {
                                    JSONObject object1 = new JSONObject();
                                    object1.put("fileName", file.getName());
                                    listArray.add(jsonObject);
                                }
                            }
                        }

                        //send files to previous node
                        if (!networkManager.getPreviousIP().equals(Inet4Address.getLocalHost())) {
                            for (Object object : listArray) {
                                JSONObject jsonObject = (JSONObject) object;
                                Object fileName = jsonObject.get("fileName");
                                byte[] buf = fileName.toString().getBytes();
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, networkManager.getPreviousIP(), LISTPORT);
                                datagramSocket.send(packet);
                            }
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

    //update own list based on list of the next node
    public void listenForFiles() {
        System.out.println("Sync agent: listening for files");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(LISTPORT);
            while (!datagramSocket.isClosed()) {
                System.out.println("Sync agent: files received!");
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                Thread thread = new Thread(() -> {
                    byte[] bytes = datagramPacket.getData();
                    String fileName = Arrays.toString(bytes);
                    System.out.println("!!!!!!!" + fileName);
                    boolean containsFile = false;
                    for (Object object : listArray) {
                        JSONObject jsonObject = (JSONObject) object;
                        if (fileName.equals(jsonObject.get(fileName))) {
                            containsFile = true;
                            break;
                        }
                    }
                    if (!containsFile) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("fileName", fileName);
                        listArray.add(jsonObject);
                        System.out.println("Sync agent: file added!");
                    }
                    System.out.println("Sync agent: fileList: " + listArray);
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
