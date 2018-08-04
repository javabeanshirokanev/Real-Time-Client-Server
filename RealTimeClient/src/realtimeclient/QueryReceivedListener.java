/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeclient;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author USER
 */
public interface QueryReceivedListener {
    void serverQueryReceived(RealTimeClient sender, DataInputStream reader) throws IOException;
    void queryUnswerReceived(RealTimeClient sender, int queryType, DataInputStream reader) throws IOException;
}
