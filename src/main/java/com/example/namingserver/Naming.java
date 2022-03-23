package com.example.namingserver;

import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.util.HashMap;

@Service
public class Naming {
    static HashMap<Integer, Inet4Address> nodesList = new HashMap<>();

    public Naming() {}

    public Inet4Address getRequest(String fileName){
        int hash = (int) ((fileName.hashCode()+2147483648.0)*(32768/(2147483648.0+Math.abs(-2147483648))));
        Inet4Address IPaddress = checkID(hash);
        return IPaddress;
    }

    public void addNode(String hostName, Inet4Address ipadres){
        int hash = (int) ((hostName.hashCode()+2147483648.0)*(32768.0 /(2147483648.0 +Math.abs(-2147483648.0))));
        nodesList.put(hash, ipadres);
    }


    public Inet4Address checkID(int hash) {
        Integer closestID = -1;
        for (Integer ID : this.nodesList.keySet()) {
            if (hash < ID) {
                closestID = ID;
            }
            else {
                Integer min = 32769;
                if (Math.abs(hash-ID) < min) {
                    min = ID;
                }
            }
        }
        return nodesList.get(closestID);
    }
}
