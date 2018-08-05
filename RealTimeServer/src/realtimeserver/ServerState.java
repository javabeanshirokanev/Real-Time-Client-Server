/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeserver;

import data.DataBlock;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author Широканев Александр
 */
public interface ServerState {
    boolean isParamsCorrect(byte[] params);
    DataBlock getStateForConnectedClient(InetAddress ip, int port, int clientID);
    DataBlock getUnswerToQuery(InetAddress ip, int port, short blockType, DataBlock receivedBlock);
    void update(RealTimeServer server);
    void updatingMessageReceived(RealTimeServer server, DataInputStream reader) throws IOException;
}
