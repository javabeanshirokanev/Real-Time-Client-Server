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
    void serverQueryReceived(DataInputStream reader) throws IOException;
    void queryUnswerReceived(int queryType, DataInputStream reader) throws IOException;
}
