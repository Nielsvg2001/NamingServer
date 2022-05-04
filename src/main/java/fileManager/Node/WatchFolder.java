package fileManager.Node;

import java.nio.file.*;

// https://fullstackdeveloper.guru/2020/12/23/how-to-watch-a-folder-directory-or-changes-using-java/
public class WatchFolder {
    public FileManager fileManager;
    public FileTransfer fileTransfer;

    public WatchFolder(FileManager fileManagere) {
        fileManager = fileManagere;
        fileTransfer = fileManager.fileTransfer;
        new Thread(this::startWatchFolder).start();
    }

    private void startWatchFolder() {

        try {

            System.out.println("Watching directory for changes");

            // STEP1: Create a watch service
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // STEP2: Get the path of the directory which you want to monitor.
            Path directory = Path.of("src/main/java/fileManager/Node/Local_files");

            // STEP3: Register the directory with the watch service
            WatchKey watchKey = directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            // STEP4: Poll for events
            while (true) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {

                    // STEP5: Get file name from even context
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;

                    Path fileName = pathEvent.context();

                    // STEP6: Check type of event.
                    WatchEvent.Kind<?> kind = event.kind();

                    // STEP7: Perform necessary action with the event
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        System.out.println("A new file is created : " + fileName);
                        //fileTransfer.sendFile(fileManager.namingRequest(Node.hashCode(fileName)),file);
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
