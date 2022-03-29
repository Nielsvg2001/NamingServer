package com.example.namingserver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.HashMap;

public class JSONHelper {

    public JSONHelper() {
    }

    public void writeToFile(HashMap<Integer, Inet4Address> nodesList) {
        JSONArray jsonArray = new JSONArray();
        for (Integer key : nodesList.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, nodesList.get(key).getHostAddress());
            jsonArray.add(jsonObject);
        }
        try (FileWriter file = new FileWriter("src/main/resources/nodes.json")) {
            file.write(jsonArray.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, Inet4Address> readFromFile() {
        JSONParser parser = new JSONParser();
        HashMap<Integer, Inet4Address> nodesList = new HashMap<>();
        try (FileReader reader = new FileReader("src/main/resources/nodes.json")) {
            Object obj = parser.parse(reader);
            JSONArray jsonArray = (JSONArray) obj;
            for (Object o : jsonArray) {
                JSONObject jsonObject = (JSONObject) o;
                for (Object key : jsonObject.keySet()) {
                    nodesList.put(Integer.valueOf((String) key), (Inet4Address) Inet4Address.getByName((String) jsonObject.get(key)));
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return nodesList;
    }
}
