/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import data.DataBlock;
import data.WriterReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import realtimeclient.RealTimeClient;
import realtimeclient.UpdatedListener;

/**
 *
 * @author USER
 */
public class TestClient implements UpdatedListener {
    
    
    public static void main(String[] args) throws SocketException, IOException, IllegalAccessException, InstantiationException {
        RealTimeClient client = new RealTimeClient(32, 300, 1);
        InetAddress ip = InetAddress.getLocalHost();
        
        WriterReader.types = new Class[] { TestBlock.class };
        client.addUpdatedListener(new TestClient());
        client.connect(ip, 4000);
        double[] ds = new double[] { 1.2, 1.3, 0, 1, 3.1, 2.3, 4, 2.5 };
        TestBlock b = new TestBlock(ds);
        DataBlock result = client.getResultQuery(b);
        DataBlock result2 = client.getResultQuery(b);
        
        TestBlock a = new TestBlock(new double[] { 4.1 });
        client.sendUpdatingMessage(a);
        client.sendUpdatingMessage(a);
        client.update();
        client.update();
        client.update();
        client.update();
        
        client.disconnect();
        
        client.sendAdminCommandClose(new byte[0]);
        
        client.close();
    }

    @Override
    public void dataUpdated(RealTimeClient sender) {
        
    }

    @Override
    public void updatingMessageReceived(RealTimeClient sender, DataInputStream reader) throws IOException {
        List<DataBlock> blocks = WriterReader.readDataBlocks(reader);
        
    }
}
