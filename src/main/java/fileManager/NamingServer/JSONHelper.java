package fileManager.NamingServer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.TreeMap;

public class JSONHelper {

    public JSONHelper() {
    }

    /**
     * @param nodesList - HashMap of nodes and their IP addresses
     *                  Writes the HashMap to a JSON file.
     */
    public void writeToFile(TreeMap<Integer, Inet4Address> nodesList) {
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
    public TreeMap<Integer, Inet4Address> readFromFile() {
        JSONParser parser = new JSONParser();
        TreeMap<Integer, Inet4Address> nodesList = new TreeMap<>();
        if ((new File("src/main/resources/nodes.json").exists())) {
            try (FileReader reader = new FileReader("src/main/resources/nodes.json")) {
                Object obj = parser.parse(reader);
                JSONArray jsonArray = (JSONArray) obj;
                for (Object o : jsonArray) {
                    JSONObject jsonObject = (JSONObject) o;
                    for (Object key : jsonObject.keySet()) {
                        nodesList.put(Integer.valueOf((String) key), (Inet4Address) Inet4Address.getByName((String) jsonObject.get(key)));
                    }
                }
            } catch (ParseException | IOException e) {
                System.out.println("File is empty");
            }
        } else {
            System.out.println("File does not exist");
        }
        return nodesList;
    }
}
