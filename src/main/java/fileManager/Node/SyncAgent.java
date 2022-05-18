package fileManager.Node;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;

import jade.core.Agent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

// https://jade.tilab.com/doc/api/jade/core/Agent.html
public class SyncAgent extends Agent {
    private JSONArray array;
    // werkt voor geen meter
    /*@Override
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
    }*/
}
