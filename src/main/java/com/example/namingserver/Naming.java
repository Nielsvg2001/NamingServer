package com.example.namingserver;

import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class Naming {
    static HashMap<Integer, Inet4Address> nodesList;
    static JSONHelper jsonHelper = new JSONHelper();

    public Naming() {
        nodesList = jsonHelper.readFromFile();
    }

    /**
     * @param fileName name of the file
     * @return ipadres of the node that has the file
     * Gives the ipadres of the node that has the file.
     */
    public static Inet4Address getRequest(String fileName) {
        int hash = hashCode(fileName);
        System.out.println("Requesting file: " + fileName + " with hash: " + hash);
        return checkID(hash);
    }

    /**
     * @param hostName name of the node
     * @param ipadres  ipadres of the node
     * @return hash of the added node
     * Adds a node to the list.
     */
    public static int addNode(String hostName, Inet4Address ipadres) {
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            int hash = hashCode(hostName);
            if (!nodesList.containsKey(hash)) {
                nodesList.put(hash, ipadres);
                jsonHelper.writeToFile(nodesList);
                return hash;
            }
        } finally {
            lock.unlock();
        }
        return -1;
    }

    /**
     * @param hostName name of the node
     * @return hash of the removed node
     * Removes a node from the list.
     */
    public static int removeNode(String hostName) {
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            if (nodesList.containsKey(hashCode(hostName))) {
                nodesList.remove(hashCode(hostName));
                jsonHelper.writeToFile(nodesList);
                return hashCode(hostName);
            }
        } finally {
            lock.unlock();
        }
        return -1;
    }

    /**
     * @param hashFilename hash of the filename
     * @return ipadres of the node that has the file
     * Gives the ipadres of the node that has the file.
     */
    public static Inet4Address checkID(int hashFilename) {
        int closestID = -1;
        int biggestID = -1;
        for (Integer ID : nodesList.keySet()) {
            // N is the collection of nodes with a hash smaller than the hash of the filename
            if (ID < hashFilename) {
                // The node with the smallest difference between its hash  and the file hash is the owner of the file.
                if (Math.abs(ID - hashFilename) < Math.abs(closestID - hashFilename) || closestID == -1)
                    closestID = ID;
            }
            // If N is empty, the node with the biggest hash stores the requested file
            if (ID > biggestID)
                biggestID = ID;
        }
        return nodesList.get((closestID == -1) ? biggestID : closestID);
    }


    /**
     * @param toHash string to hash
     * @return hash of the string
     * Maps a string to a hash between 0 and 32768.
     */
    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode() + 2147483648.0) * (32768 / (2147483648.0 + Math.abs(-2147483648.0))));
    }
}
