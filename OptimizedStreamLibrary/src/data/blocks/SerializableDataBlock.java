/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.blocks;

import data.DataBlock;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Широканев Александр
 */
public final class SerializableDataBlock implements DataBlock {

    private short id;
    private Object object;
    private int byteCount;
    
    public SerializableDataBlock() {
        
    }
    
    public SerializableDataBlock(Object object, short id, int byteCount) {
        this.object = object;
        this.id = id;
        this.byteCount = byteCount;
    }
    public SerializableDataBlock(Object object, short id) {
        this.object = object;
        this.id = id;
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream(16777216)) {    //Временный буфер на 16 МБ
            try(ObjectOutputStream objOut = new ObjectOutputStream(stream)) {
                objOut.writeObject(object);
            }
            this.byteCount = stream.size();
        } catch(IOException ex) {
            Logger.getLogger(SerializableDataBlock.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Object getObject() { return object; }
    
    @Override
    public short getBlockIdentificator() {
        return id;
    }

    @Override
    public void readData(DataInputStream stream) throws IOException {
        try(ObjectInputStream in = new ObjectInputStream(stream)) {
            object = in.readObject();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SerializableDataBlock.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        try(ObjectOutputStream out = new ObjectOutputStream(stream)) {
            out.writeObject(object);
        }
    }
    
    @Override
    public int getByteCount() {
        return byteCount;
    }
}
