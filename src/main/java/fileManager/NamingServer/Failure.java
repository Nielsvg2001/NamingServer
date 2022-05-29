package fileManager.NamingServer;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Failure {

    private static final int FAILUREPORT = 9997;
    private static final int SHUTDOWNPORT = 9998;

    /**
     * Constructor of Failure
     */
    public Failure() {
    }

    /**
     * this function starts the failure discovery on the Namingserver
     * The Naming listens on a FailurePort to nodes that send a shutdownsignal, or to nodes that notify that another node is failed.
     * Then it sends a new previous or nextnode to the neighbors of the failed/shutdown node
     */
    public static void start() {
        System.out.println("Starting Failure");
        try {
            DatagramSocket datagramSocket = new DatagramSocket(FAILUREPORT);
            while (!datagramSocket.isClosed()) {
                //recieve packet
                DatagramPacket received = new DatagramPacket(new byte[256], 256);
                datagramSocket.receive(received);
                //for every packet is a new thread created
                Thread thread = new Thread(() -> {
                    // Receiving failed node
                    int hash = Integer.parseInt(new String(received.getData(), 0, received.getLength()));

                    // Creating response
                    Integer previousNode = Naming.getNodesList().lowerKey(hash);
                    Integer nextNode = Naming.getNodesList().higherKey(hash);

                    // Extreme case: first node : take last node as previous
                    //               last node : take first node as next
                    previousNode = previousNode == null ? Naming.getNodesList().lastKey() : previousNode;
                    nextNode = nextNode == null ? Naming.getNodesList().firstKey() : nextNode;

                    System.out.println("Hash van hostname: " + hash);
                    System.out.println("previousNode: " + previousNode + " with ip: " + Naming.getNodeInfo(previousNode));
                    System.out.println("nextNode: " + nextNode + " with  ip: " + Naming.getNodeInfo(nextNode));


                    try {
                        //sending new next and previous node to the neighbors of failed/shutdown node
                        DatagramSocket socket = new DatagramSocket();
                        try {
                            // Sending nextNode to previousNode
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("newNextNode", nextNode);
                            byte[] buf = jsonObject.toString().getBytes();
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, Naming.getNodeInfo(previousNode), SHUTDOWNPORT);
                            socket.send(packet);

                            // Sending previousNode to nextNode
                            jsonObject = new JSONObject();
                            jsonObject.put("newPreviousNode", previousNode);
                            buf = jsonObject.toString().getBytes();
                            packet = new DatagramPacket(buf, buf.length, Naming.getNodeInfo(nextNode), SHUTDOWNPORT);
                            socket.send(packet);
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }

                    // Remove failed node to the list
                    Naming.removeNode(hash);
                });
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
