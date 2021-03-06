package com.example.namingserver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Inet4Address;
import java.util.HashMap;

public class JSONHelper {

    public JSONHelper() {
    }

    /**
     * @param nodesList - HashMap of nodes and their IP addresses
     *                  Writes the HashMap to a JSON file.
     */
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

    /**
     * @return HashMap of nodes and their IP addresses
     * Reads the JSON file and returns a HashMap of nodes and their IP addresses.
     */
    public HashMap<Integer, Inet4Address> readFromFile() {
        JSONParser parser = new JSONParser();
        HashMap<Integer, Inet4Address> nodesList = new HashMap<>();
        if( (new File("src/main/resources/nodes.json").exists()) ) {
            try (FileReader reader = new FileReader("src/main/resources/nodes.json")) {
                BufferedReader br = new BufferedReader(reader);
                if (br.readLine() != null) {
                    Object obj = parser.parse(reader);
                    JSONArray jsonArray = (JSONArray) obj;
                    for (Object o : jsonArray) {
                        JSONObject jsonObject = (JSONObject) o;
                        for (Object key : jsonObject.keySet()) {
                            nodesList.put(Integer.valueOf((String) key), (Inet4Address) Inet4Address.getByName((String) jsonObject.get(key)));
                        }
                    }
                } else {
                    System.out.println("File is empty");
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("File does not exist");
        }
        return nodesList;
    }
}
