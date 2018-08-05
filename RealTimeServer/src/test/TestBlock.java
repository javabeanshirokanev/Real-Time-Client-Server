/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import data.DataBlock;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author USER
 */
public class TestBlock implements DataBlock {
    private double[] ws;
    
    public double[] getWS() { return ws; }

        public TestBlock() { }
        public TestBlock(double[] ws) {
            this.ws = ws;
        }
        
        @Override
        public short getBlockIdentificator() {
            return 0;
        }

        @Override
        public void readData(DataInputStream stream) throws IOException {
            int count = stream.readInt();
            ws = new double[count];
            for(int i = 0; i < count; i++) {
                ws[i] = stream.readDouble();
            }
        }

        @Override
        public void writeData(DataOutputStream stream) throws IOException {
            stream.writeInt(ws.length);
            for(int i = 0; i < ws.length; i++) {
                stream.writeDouble(ws[i]);
            }
        }

        @Override
        public int getByteCount() {
            return 8 * ws.length + 4;    //8 - размер double, 4 - размер int
        }
}
