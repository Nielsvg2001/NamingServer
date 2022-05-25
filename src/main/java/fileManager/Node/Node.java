package fileManager.Node;

public class Node {
    public NetworkManager networkManager;
    public FileManager fileManager;
    public WatchFolder watchfolder;
    public LogHandler logHandler;

    /**
     * to run node
     */
    public static void main(String[] args) throws InterruptedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        Node cl = new Node();
        //cl.fileManager.namingRequest("testfile name.txt");;
        //Thread.sleep(60000);
        //cl.shutdown();

    }

    /**
     * constructor of node that creates networkmanager, filemanager, watchfolder
     */
    public Node() {
        // get own information
        networkManager = new NetworkManager(this);
        logHandler = new LogHandler();
        fileManager = new FileManager(networkManager, logHandler);
        watchfolder = new WatchFolder(fileManager);
    }

    /**
     * calls shutdown of networkmanager and filemanager and exits the system
     */
    public void shutdown() {
        networkManager.shutdown();
        fileManager.shutdown();
        System.exit(0);
    }

    /**
     * calculate the hash of a file or hostname
     *
     * @param toHash String that must be hashed
     * @return int hash
     */
    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode() + 2147483648.0) * (32768 / (2147483648.0 + Math.abs(-2147483648.0))));
    }

    /**
     * calls filemanager.startup
     */
    public void sendReplicatedfiles() {
        fileManager.startUp();
    }
}
