package fileManager.Node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jade.core.Agent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

// https://jade.tilab.com/doc/api/jade/core/Agent.html
public class SyncAgent extends Agent {
    private JSONArray listArray;
    private NetworkManager networkManager;
    private static int LISTPORT = 9994;

    public SyncAgent(NetworkManager networkManager) {
        listArray = new JSONArray();
        this.networkManager = networkManager;
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();

        assert files != null;
        for (File file : files) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fileName", file.getName());
            listArray.add(jsonObject);
        }
        new Thread(this::sync).start();
        new Thread(this::listenForFiles).start();
    }

    public void sync() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            Thread thread = new Thread(() -> {
                try {
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
            }); thread.start();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForFiles() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(LISTPORT);
            while (!datagramSocket.isClosed()) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(datagramPacket);
                Thread thread = new Thread(() -> {
                    byte[] bytes = datagramPacket.getData();
                    String fileName = Arrays.toString(bytes);
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
