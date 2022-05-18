package fileManager.Node;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.Arrays;

// https://fullstackdeveloper.guru/2020/12/23/how-to-watch-a-folder-directory-or-changes-using-java/
public class WatchFolder {
    public FileManager fileManager;
    public FileTransfer fileTransfer;

    public String localPath;

    public WatchFolder(FileManager fileManagere) {
        fileManager = fileManagere;
        fileTransfer = fileManager.fileTransfer;
        localPath = "src/main/java/fileManager/Node/Local_files/";

        File folder = new File(localPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        new Thread(this::startWatchFolder).start();
    }

    private void startWatchFolder() {

        try {

            System.out.println("Watching directory for changes");

            // STEP1: Create a watch service
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // STEP2: Get the path of the directory which you want to monitor.
            Path directory = Path.of("src/main/java/fileManager/Node/Local_files/");

            // STEP3: Register the directory with the watch service
            WatchKey watchKey = directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            // STEP4: Poll for events
            while (true) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {

                    // STEP5: Get file name from even context
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;

                    Path fileName = pathEvent.context();
                    File file = new File(directory + "/"+ fileName);

                    // STEP6: Check type of event.
                    WatchEvent.Kind<?> kind = event.kind();

                    // STEP7: Perform necessary action with the event
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        System.out.println("A new file is created : " + fileName);
                        System.out.println(file.getName());
                        System.out.println(file.getAbsolutePath());
                        System.out.println(file);
                        //check replicated node of file
                        System.out.println("hash = " + Node.hashCode(String.valueOf(fileName)));
                        Inet4Address ipaddress = fileManager.namingRequest(Node.hashCode(String.valueOf(fileName)));
                        System.out.println("ip = " + ipaddress);
                        Inet4Address localIP = (Inet4Address) InetAddress.getLocalHost();
                        //check if replicated node is itself, then send it to previous node
                        if(Arrays.equals(localIP.getAddress(), ipaddress.getAddress())){
                            System.out.println("ip = zelfde");
                            Inet4Address previousIP = fileManager.networkManager.getPreviousIP();
                            System.out.println("ip vorige node : "+ previousIP);
                            //if previous node is itself, then it is the only node in the netwerk so don't send it
                            if(!(previousIP == localIP)){
                                fileTransfer.sendFile(previousIP,file);
                                System.out.println("file send to priveous node");
                            }
                        }
                        else{
                            fileTransfer.sendFile(ipaddress,file);
                            System.out.println("file send to ipaddress");
                        }
                        System.out.println("end of watchevent file added");
                    }

                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                        System.out.println("A file has been deleted: " + fileName);
                    }
                    /*
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                        System.out.println("A file has been modified: " + fileName);
                    }
                     */

                }

                // STEP8: Reset the watch key everytime for continuing to use it for further event polling
                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
