package fileManager.Node;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileWriter;

public class LogHandler {

    private File logFile;
    JSONObject log = new JSONObject();

    //for testing
    public static void main(String[] args) throws InterruptedException {
        LogHandler logHandler = new LogHandler();
        logHandler.addFileToLog("testfile", 2748950, "local");
        logHandler.addFileToLog("testfile2", 50000, "local");
        logHandler.addFileToLog("testfile3", 8000, "replicated");
        logHandler.addFileToLog("testfile4", 89393, "replicated");

        Thread.sleep(15000);
        logHandler.removeFileLog("testfile", "local");
    }

    /**
     * constructor that creates logFile
     */
    public LogHandler() {
        createLogFile();
    }

    /**
     * creates log file
     */
    private void createLogFile() {
        logFile = new File("src/main/java/fileManager/Node/Log.json");
        try {
            if (!logFile.createNewFile()){
                System.out.println("file already exists");
            }
            log.put("local", new JSONArray());
            log.put("replicated", new JSONArray());
            writeLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds file to log
     * @param fileName String :filename to add to log
     * @param downloadlocation int hash of node that has this file as local file
     * @param location string: where the file is stored: replicated or local
     */
    public void addFileToLog(String fileName, int downloadlocation, String location) {
        JSONArray locationArray = (JSONArray) log.get(location);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileName", fileName);
        jsonObject.put("downloadlocation", downloadlocation);
        locationArray.add(jsonObject);
        System.out.println("AddFileToLog: " + log.toJSONString());
        writeLog();
    }

    /**
     * Removes file from log
     * @param fileName String filename to remove
     * @param location location to remove
     * @return returns the jsonObject if it is removed, otherwise it returns null
     */
    public JSONObject removeFileLog(String fileName, String location) {
        System.out.println("RefomveFileLog: " + fileName + " " + location);
        System.out.println("RefomveFileLog: " + log.toJSONString());

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

    /**
     * write jsonobject back to the Log.json
     */
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
