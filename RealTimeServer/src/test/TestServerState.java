/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import data.DataBlock;
import data.WriterReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import realtimeserver.RealTimeServer;
import realtimeserver.ServerState;

/**
 *
 * @author USER
 */
public class TestServerState implements ServerState {

    public TestServerState() {
        WriterReader.types = new Class[] { TestBlock.class };
    }
    
    @Override
    public boolean isParamsCorrect(byte[] params) {
        return true;
    }

    @Override
    public DataBlock getStateForConnectedClient(InetAddress ip, int port, int clientID) {
        double[] startState = new double[] { 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8 };
        return new TestBlock(startState);
    }

    @Override
    public DataBlock getUnswerToQuery(InetAddress ip, int port, short blockType, DataBlock receivedBlock) {
        if(blockType == 0) {
            TestBlock rB = (TestBlock)receivedBlock;
            double[] ws = rB.getWS();
            for(int i = 0; i < ws.length; i++) {
                ws[i] *= 2;
            }
            return new TestBlock(ws);
        }
        return null;
    }

    @Override
    public void update(RealTimeServer server) {
        System.out.println("world updated");
    }

    @Override
    public void updatingMessageReceived(RealTimeServer server, DataInputStream reader) throws IOException {
        System.out.println("update message received");
        List<DataBlock> list = WriterReader.readDataBlocks(reader);
    }
    
}
