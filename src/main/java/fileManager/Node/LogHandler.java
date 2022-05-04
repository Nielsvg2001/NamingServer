package fileManager.Node;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class LogHandler {

    private File logFile;
    JSONObject log = new JSONObject();

    public static void main(String[] args) throws InterruptedException {
        LogHandler logHandler = new LogHandler();
        logHandler.addFileToLog("testfile", 2748950, "local");
        logHandler.addFileToLog("testfile2", 50000, "local");
        logHandler.addFileToLog("testfile3", 8000, "replicated");
        logHandler.addFileToLog("testfile4", 89393, "replicated");

        Thread.sleep(15000);
        logHandler.removeFileLog("testfile", "local");
    }

    public LogHandler() {
        createLogFile();
    }

    private void createLogFile() {
        logFile = new File("src/main/java/fileManager/Node/Log.json");
        try {
            logFile.createNewFile();
            log.put("local", new JSONArray());
            log.put("replicated", new JSONArray());
            writeLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFileToLog(String fileName, int downloadlocation, String location) {
        JSONArray locationArray = (JSONArray) log.get(location);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileName", fileName);
        jsonObject.put("downloadlocation", downloadlocation);

        locationArray.add(jsonObject);

        writeLog();
    }

    private JSONObject removeFileLog(String fileName, String location) {
        JSONArray locationArray = (JSONArray) log.get(location);

        for (Object object : locationArray) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.get("fileName").equals(fileName)) {
                locationArray.remove(jsonObject);
                writeLog();
                return jsonObject;
            }
        }
        return null;
    }

    public void writeLog() {
        try {
            FileWriter file = new FileWriter(logFile);
            file.write(log.toJSONString());
            file.flush();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
