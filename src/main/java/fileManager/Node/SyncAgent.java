package fileManager.Node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
    //private JSONArray array;
    private ArrayList<String> fileList;
    private NetworkManager networkManager;
    private static int LISTPORT = 9994;

    public SyncAgent(NetworkManager networkManager) {
        //array = new JSONArray();
        this.networkManager = networkManager;
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();

        assert files != null;
        for (File file : files) {
            fileList.add(file.toString());
        }
        //array.addAll(Arrays.asList(files));
        new Thread(this::sync).start();
    }

    /*
    public JSONArray getArray() {
        return array;
    }
    */

    public void sync() {
        Thread thread = new Thread(() -> {
            JSONArray nextArray = new JSONArray();

        });
        thread.start();

    }

    public void listenNextList() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();

            byte[] bytes = array;
            while (true) {
                Thread thread = new Thread(() -> {
                    try {
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, networkManager.getNextIP(), LISTPORT);
                        datagramSocket.send(packet);

                        DatagramPacket datagramPacket = new DatagramPacket(new byte[256], 256);
                        datagramSocket.receive(datagramPacket);
                        byte[] bytes1 = datagramPacket.getData();
                        String fileName = bytes.toString();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setup() {
        array = new JSONArray();
    }
    public void lockFileListener() {
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();

        String lockStand = "NotLocked";

        for (File file : files) {
            JSONObject jsonObject = new JSONObject();
            for (JSONObject object : array) {
                if (!object.get("file").equals(file)) {
                    jsonObject.put("fileStand", lockStand);
                    jsonObject.put("file", file);
                    array.add(jsonObject);
                }
            }
        }

        while(true) {
            Thread thread = new Thread(() -> {
                for (JSONObject object : array) {
                    if (object.get("fileStand").equals("Locked")) {
                        RandomAccessFile file = (RandomAccessFile) object.get("file");
                        FileLock lock;
                        try {
                            lock = file.getChannel().lock();
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                    if (object.get("fileStand").equals("NotLocked")) {
                        lock.release();
                    }
                }
            });
            thread.start();
        }
    }
}
