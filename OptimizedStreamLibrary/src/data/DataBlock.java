/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Широканев
 */
public interface DataBlock {
    short getBlockIdentificator();
    void readData(DataInputStream stream) throws IOException;
    void writeData(DataOutputStream stream) throws IOException;
    int getByteCount();
}
