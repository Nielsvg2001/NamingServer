package fileManager.NamingServer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Listener {
    private static Discovery discovery;
    public Listener(Discovery discover) {
        discovery = discover;
    }

    public void start() {
        try {
            MulticastSocket mSocket = new MulticastSocket(9999);
            String multicastAddress = "230.0.0.1";
            InetAddress mGroup = InetAddress.getByName(multicastAddress);
            mSocket.joinGroup(mGroup);
            while (!mSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                mSocket.receive(packet);
                Thread thread = new Thread(() -> {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = (JSONObject) parser.parse(new String(packet.getData(), 0, packet.getLength()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    assert jsonObject != null;
                    String type = jsonObject.get("type").toString();
                    switch (type) {
                        case "discovery":
                            discovery.start((String) jsonObject.get("hostname"),packet);
                            break;
                        case "else":
                            //code
                            break;

                    }
                });
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
