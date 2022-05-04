package fileManager.Node;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

import static fileManager.Node.NetworkManager.getNodeInfo;

public class FileTransfer {

    private static final int FILEPORT = 9996;


    public FileTransfer() {
        new Thread(this::fileListener).start();
    }

    // sent file (filePath) to node (ID)
    public void sentFile(int ID, String filePath) {
        System.out.println("Sending file");
        File fileToSend = new File(filePath);

        try {
            Socket socket = new Socket(getNodeInfo(ID), FILEPORT);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            String filename = fileToSend.getName();
            byte[] fileNameBytes = filename.getBytes();

            byte[] fileContentBytes = Files.readAllBytes(fileToSend.toPath());

            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);

            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            System.out.println("File is sent!");
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
                                    File fileToDownload = new File(fileName);
                                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                    fileOutputStream.write(fileContentBytes);
                                    fileOutputStream.close();
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
