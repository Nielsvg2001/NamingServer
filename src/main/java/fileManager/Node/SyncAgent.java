package fileManager.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

import jade.core.Agent;

public class SyncAgent extends Agent {

    @Override
    protected void setup() {
        File path = new File("src/main/java/fileManager/Node/Replicated_files");
        File[] files = path.listFiles();

        /*try (FileInputStream fis = new FileInputStream(path.toFile())) {
            FileLock lock = fis.getChannel().lock();
        } catch (IOException exception) {
            exception.printStackTrace();
        }*/
    }
}
