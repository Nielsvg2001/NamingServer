package fileManager.Node;

import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;

public class FileTransfer {

    private static final int FILEPORT = 9996;


    public FileTransfer() {
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

            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);

            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            System.out.println("File is sent! : "+ filename);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    // filepath moet nog aangepast worden afhankelijk van waar we files opslaan
    public void fileListener() {
        System.out.println("Start fileListener");
        try {
            ServerSocket serverSocket = new ServerSocket(FILEPORT);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> {
                    try {
                        while(!socket.isClosed()) {
                            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                            int fileNameLenght = dataInputStream.readInt();
                            if (fileNameLenght > 0) {
                                byte[] fileNameBytes = new byte[fileNameLenght];
                                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                                String fileName = new String(fileNameBytes);
                                int fileContentLenght = dataInputStream.readInt();
                                if (fileContentLenght > 0) {
                                    byte[] fileContentBytes = new byte[fileContentLenght];
                                    dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                                    File fileToDownload = new File("Repclicated_files/" +fileName);
                                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                    fileOutputStream.write(fileContentBytes);
                                    fileOutputStream.close();
                                    System.out.println(fileName);
                                    System.out.println(Arrays.toString(fileContentBytes));
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
