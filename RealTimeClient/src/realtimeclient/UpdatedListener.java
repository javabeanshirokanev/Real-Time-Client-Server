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
 * @author Широканев Александр
 */
public interface UpdatedListener {
    void dataUpdated(RealTimeClient sender);
    void updatingMessageReceived(RealTimeClient sender, DataInputStream reader) throws IOException;
}
