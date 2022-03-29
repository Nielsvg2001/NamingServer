package com.example.namingserver;

import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.util.HashMap;

@Service
public class Naming {
    static HashMap<Integer, Inet4Address> nodesList = new HashMap<>();

    public Naming() {
    }

    public static Inet4Address getRequest(String fileName) {
        int hash = hashCode(fileName);
        System.out.println("Requesting file: " + fileName + " with hash: " + hash);
        return checkID(hash);
    }

    public static int addNode(String hostName, Inet4Address ipadres) {
        int hash = hashCode(hostName);
        nodesList.put(hash, ipadres);
        return hash;
    }

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

    public static int hashCode(String toHash) {
        return (int) ((toHash.hashCode()+2147483648.0)*(32768/(2147483648.0+Math.abs(-2147483648.0))));
    }
}
