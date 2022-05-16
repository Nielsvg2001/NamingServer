package fileManager.Node;

import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileTransfer {

    private static final int FILEPORT = 9996;
    private LogHandler logHandler;
    NetworkManager networkManager;
    private Path path_ReplicationFiles;

    public FileTransfer(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.logHandler = new LogHandler();

        try {
            Path path_ReplicationFiles = Paths.get("src/main/java/fileManager/Node/Replicated_files/");
            Files.createDirectories(path_ReplicationFiles);
        }catch (IOException e){
            e.printStackTrace();
        }
        new Thread(this::fileListener).start();
    }

    // sent file (filePath) to node (ID)
    public void sendFile(Inet4Address ip, File fileToSend) {
        System.out.println("Sending file");

        try {
            Socket socket = new Socket(ip, FILEPORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            String filename = fileToSend.getName();
            byte[] fileNameBytes = filename.getBytes();

            byte[] fileContentBytes = Files.readAllBytes(fileToSend.toPath());

            byte[] hostname = Inet4Address.getLocalHost().getHostName().getBytes();

            dataOutputStream.writeInt(hostname.length);
            dataOutputStream.write(hostname);

            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);

            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            System.out.println("File is sent! : "+ filename);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    public void fileListener() {
        System.out.println("Start fileListener");
        try {
            ServerSocket serverSocket = new ServerSocket(FILEPORT);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> {
                    try {
                        while(!socket.isClosed()) {
                            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                            int hostnameLength = dataInputStream.readInt();
                            if (hostnameLength > 0) {
                                byte[] hostnameBytes = new byte[hostnameLength];
                                dataInputStream.readFully(hostnameBytes, 0, hostnameBytes.length);
                                String hostname = new String(hostnameBytes);

                                int fileNameLenght = dataInputStream.readInt();
                                if (fileNameLenght > 0) {
                                    byte[] fileNameBytes = new byte[fileNameLenght];
                                    dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                                    String fileName = new String(fileNameBytes);

                                    // check if received file isn't already a local file of the node
                                    boolean isALocalFile = false;
                                    File path = new File("src/main/java/fileManager/Node/Local_files");
                                    File[] files = path.listFiles();
                                    assert files != null;
                                    for (File file : files) {
                                        if (Node.hashCode(file.toString()) == Node.hashCode(fileName)) {
                                            isALocalFile = true;
                                        }
                                    }

                                    int fileContentLenght = dataInputStream.readInt();
                                    if (fileContentLenght > 0) {
                                        if (isALocalFile) {
                                            File fileToSend = new File("src/main/java/fileManager/Node/Local_files/" + fileName);
                                            sendFile(networkManager.getPreviousIP(), fileToSend);
                                        } else {
                                            byte[] fileContentBytes = new byte[fileContentLenght];
                                            dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                                            File fileToDownload = new File(path_ReplicationFiles + fileName);
                                            FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                            fileOutputStream.write(fileContentBytes);
                                            fileOutputStream.close();
                                            logHandler.addFileToLog(fileName, Node.hashCode(hostname), "replicated");
                                            System.out.println(fileName);
                                            System.out.println(Arrays.toString(fileContentBytes));
                                        }
                                    }
                                }
                            }

                            dataInputStream.close();
                            System.out.println("File received!");
                            socket.close();
                    }
                } catch (IOException error) {
                        error.printStackTrace();
                    }
                });
                thread.start();
            }
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}
