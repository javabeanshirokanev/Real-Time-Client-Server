/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeclient;

import data.DataBlock;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author Широканев Александр
 */
public interface StateGettedListener {
    void clientStateGetted(RealTimeClient sender, DataBlock block) throws IOException;
    void serverStateGetted(RealTimeClient sender, DataInputStream reader) throws IOException;
}
