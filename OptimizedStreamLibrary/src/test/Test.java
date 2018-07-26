/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import data.PartReader;
import data.PartWriter;
import data.SimpleUDPSenderReceiver;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author USER
 */
public class Test {
    
    public static void main(String[] args) throws SocketException, UnknownHostException {
        SimpleUDPSenderReceiver sr = new SimpleUDPSenderReceiver(8, new DatagramSocket());
        SimpleUDPSenderReceiver sr2 = new SimpleUDPSenderReceiver(8, new DatagramSocket(10000));
        
        byte[] message = new byte[] { 2, 3, 1, 4, 1, 2, 3, 4, 3, 1, 4, 5, 9 };
        PartWriter writer = new PartWriter(8);
        writer.setStaticSenderReceiver(sr);
        
        PartReader reader = new PartReader(8);
        reader.setStaticSenderReceiver(sr2);
        
        sr.setEndPoint(InetAddress.getLocalHost(), 10000);
        
        Thread t1 = new Thread() {
            @Override
            public void run() {
                writer.writeMessage(message);
            }
        };
        t1.start();
        reader.readMessage();
        
        byte[] mess = reader.getMessage();
        for(int i = 0; i < mess.length; i++) {
            System.out.print(mess[i] + " ");
        }
    }
}
