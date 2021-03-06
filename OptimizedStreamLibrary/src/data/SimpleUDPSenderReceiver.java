/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author Широканев Александр
 */
public class SimpleUDPSenderReceiver implements StaticSenderReceiver {
    
    private final DatagramSocket socket;
    private final DatagramPacket sendPacket;
    private final DatagramPacket recvPacket;
    
    private InetAddress lastIp;
    private int lastPort;
    
    public InetAddress getLastIp() { return lastIp; } 
    public int getLastPort() { return lastPort; }
    
    public SimpleUDPSenderReceiver(int partSize, DatagramSocket socket) {
        this.socket = socket;
        sendPacket = new DatagramPacket(new byte[partSize], partSize);
        recvPacket = new DatagramPacket(new byte[partSize], partSize);
    }
    
    public void setEndPoint(InetAddress ip, int port) { this.lastIp = ip; this.lastPort = port; }

    @Override
    public void send(byte[] buf, int count) {
        try {
            sendPacket.setAddress(lastIp);
            sendPacket.setPort(lastPort);
            sendPacket.setData(buf);
            sendPacket.setLength(count);
            socket.send(sendPacket);
        } catch(IOException e) {
            
        }
    }

    @Override
    public int recv(byte[] buf) {
        try {
            socket.receive(recvPacket);
            byte[] recvMessage = recvPacket.getData();
            int count = recvPacket.getLength();
            lastIp = recvPacket.getAddress();
            lastPort = recvPacket.getPort();
            System.arraycopy(recvMessage, 0, buf, 0, count);
            return count;
        } catch(IOException e) {
            
        }
        return -1;
    }
    
}
