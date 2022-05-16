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
    NetworkManager networkManager;
    private Path path_ReplicationFiles;

    public FileTransfer(NetworkManager networkManager) {
        this.networkManager = networkManager;
        try {
            Path path_ReplicationFiles = Paths.get("src/main/java/fileManager/Node/Replicated_files/");
            Files.createDirectories(path_ReplicationFiles);
        }catch (IOException e){
            e.printStackTrace();
        }
        new Thread(this::fileListener).start();
    }

    // sent file (fileToSend) to node with ip (ip)
    public void sendFile(Inet4Address ip, File fileToSend) {
        System.out.println("Sending file");

        try {
            Socket socket = new Socket(ip, FILEPORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            String filename = fileToSend.getName();
            byte[] fileNameBytes = filename.getBytes();

            byte[] fileContentBytes = Files.readAllBytes(fileToSend.toPath());

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
                        DataInputStream dataInputStream = null;
                        while (!socket.isClosed()) {
                            dataInputStream = new DataInputStream(socket.getInputStream());
                            int fileNameLenght = dataInputStream.readInt();
                            System.out.println("filenamelength : " + fileNameLenght);
                            if (fileNameLenght > 0) {
                                byte[] fileNameBytes = new byte[fileNameLenght];
                                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                                String fileName = new String(fileNameBytes);
                                System.out.println("filename : " + fileName);
                                int fileContentLenght = dataInputStream.readInt();
                                System.out.println("filecontentlength: " + fileContentLenght);
                                System.out.println("datainputstream: " + dataInputStream);
                                if (fileContentLenght > 0) {
                                    File fileToSend = new File("src/main/java/fileManager/Node/Local_files/" + fileName);
                                    // sendFile niet zomaar doen, als bij test.txt in begin blijven deze berichten ronddraaien omdat iedereeen dit als local file heeft en dit verder doorstuurd
                                    //sendFile(networkManager.getPreviousIP(), fileToSend);

                                    System.out.println("recieve file");
                                    byte[] fileContentBytes = new byte[fileContentLenght];
                                    dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                                    File fileToDownload = new File(path_ReplicationFiles + fileName);
                                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                    fileOutputStream.write(fileContentBytes);
                                    fileOutputStream.close();
                                    System.out.println(fileName);
                                    System.out.println(Arrays.toString(fileContentBytes));
                                }
                            }
                        }

                        dataInputStream.close();
                        System.out.println("File received!");
                        socket.close();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
