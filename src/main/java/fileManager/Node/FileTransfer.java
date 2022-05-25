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
    private static final int DELETEFILEPORT = 9955;

    private final LogHandler logHandler;
    NetworkManager networkManager;
    private Path path_ReplicationFiles;

    /**
     * constructor
     * starts fileListener in new thread
     *
     * @param networkManager Networkmanager that does everything with the network
     */
    public FileTransfer(NetworkManager networkManager, LogHandler logHandler) {
        this.networkManager = networkManager;
        this.logHandler = logHandler;
        try {
            path_ReplicationFiles = Paths.get("src/main/java/fileManager/Node/Replicated_files/");
            Files.createDirectories(path_ReplicationFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this::fileListener).start();
        new Thread(this::deleteListener).start();
    }

    /**
     * send the file to the node with IP
     *
     * @param ip         ip where to send the file
     * @param fileToSend file to send
     */
    public void sendFile(Inet4Address ip, File fileToSend, int hostnamehash) {
        System.out.println("Sending file");
        try {
            Socket socket = new Socket(ip, FILEPORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            String filename = fileToSend.getName();
            //3 packets: filename, filecontent and hash of hostname
            byte[] fileNameBytes = filename.getBytes();
            byte[] fileContentBytes = Files.readAllBytes(fileToSend.toPath());
            byte[] hostnamehashbyte = String.valueOf(hostnamehash).getBytes();
            dataOutputStream.writeInt(hostnamehashbyte.length);
            dataOutputStream.write(hostnamehashbyte);

            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);

            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            System.out.println("File is sent! : " + filename);
            socket.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
        System.out.println("sendfile completed");
    }

    /**
     * Sends a delete message to the owner of the local file that is deleted
     *
     * @param ip          IP adddress of the replicated node
     * @param deletedFile Deleted file
     */
    public void sendDeleteMessage(Inet4Address ip, File deletedFile) {
        System.out.println("sending delete message");
        try {
            Socket socket = new Socket(ip, DELETEFILEPORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            String filename = deletedFile.getName();
            byte[] fileNameBytes = filename.getBytes();
            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);
            System.out.println("Delete File is sent! : " + filename);
            socket.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    /**
     * listens to all incomming files and puts them in Replicated files and add it to log
     */
    public void fileListener() {
        System.out.println("Start fileListener");
        try {
            ServerSocket serverSocket = new ServerSocket(FILEPORT);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                            //get hostnameLength
                            int hostnameLength = dataInputStream.readInt();
                            if (hostnameLength > 0) {
                                byte[] hostnameBytes = new byte[hostnameLength];
                                dataInputStream.readFully(hostnameBytes, 0, hostnameBytes.length);
                                int hostnamehash = Integer.parseInt(new String(hostnameBytes));
                                int fileNameLength = dataInputStream.readInt();
                                if (fileNameLength > 0) {
                                    byte[] fileNameBytes = new byte[fileNameLength];
                                    dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                                    String fileName = new String(fileNameBytes);
                                    int fileContentLength = dataInputStream.readInt();
                                    if (fileContentLength > 0) {
                                        byte[] fileContentBytes = new byte[fileContentLength];
                                        dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                                        System.out.println(Arrays.toString(fileContentBytes));
                                        File fileToDownload = new File(path_ReplicationFiles + "/" + fileName);
                                        System.out.println(fileToDownload);
                                        FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                        fileOutputStream.write(fileContentBytes);
                                        fileOutputStream.close();
                                        logHandler.addFileToLog(fileName, hostnamehash, "replicated");
                                        System.out.println("in file listener filename: " + fileName);
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

    /**
     * listens to all packets from the nodes with the local files that deleted a file
     * then it is checked in the log if the file exists somewhere else as a local file, is it not the case the file and log are deleted
     */
    public void deleteListener() {
        System.out.println("Start deletefileListener");
        try {
            ServerSocket serverSocket = new ServerSocket(DELETEFILEPORT);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                            int fileNameLength = dataInputStream.readInt();
                            String fileName = null;
                            if (fileNameLength > 0) {
                                byte[] fileNameBytes = new byte[fileNameLength];
                                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                                fileName = new String(fileNameBytes);
                            }
                            dataInputStream.close();
                            System.out.println("deleteFile received!");
                            socket.close();

                            // delete file if it was the only local file
                            File fileToDelete = new File(path_ReplicationFiles + "/" + fileName);
                            logHandler.removeFileLog(fileName, "replicated");
                            // if no other logs -> no other downloadlocation -> can be deleted
                            if (logHandler.checkDownloadlocations(fileName, "replicated") == null) {
                                if (fileToDelete.delete()) {
                                    System.out.println(fileToDelete.getName() + " deleted");
                                } else {
                                    System.out.println("failed to delete file");
                                }
                            }
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

    /**
     * @return loghandler
     */
    public LogHandler getLogHandler() {
        return logHandler;
    }
}
