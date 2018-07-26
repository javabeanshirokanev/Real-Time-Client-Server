/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author Широканев Александр
 */
public interface ServerState {
    boolean isParamsCorrect(byte[] params);
    byte[] getStateForConnectedClient(InetAddress ip, int port, int clientID);
    byte[] getUnswerToQuery(InetAddress ip, int port, DataInputStream in);
    void update();
    void updatingMessageReceived(DataInputStream reader) throws IOException;
}
