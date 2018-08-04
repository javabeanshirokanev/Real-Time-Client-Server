/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 * @author Широканев
 */
public class WriterReader {
    private WriterReader() {}

    public static Class[] types;
    
    /**
     * Прочитать блок данных
     * @param stream Поток
     * @return Блок данных
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException 
     */
    public static DataBlock readData(DataInputStream stream) throws IOException, IllegalAccessException, InstantiationException
    {
        short typeIndex = stream.readShort();
        Class type = types[typeIndex];
        DataBlock block = (DataBlock)type.newInstance();
        block.readData(stream);
        return block;
    }
    
    /**
     * Прочитать индекс типа блока (Не рекомендуется использовать без необходимости)
     * @param stream Поток
     * @return Индекс типа
     * @throws IOException 
     */
    public static short readTypeIndex(DataInputStream stream) throws IOException {
        return stream.readShort();
    }
    /**
     * Прочитать блок данных по индексу типа (Не рекомендуется использовать без необходимости)
     * @param stream Поток
     * @param typeIndex Индекс типа
     * @return Блок данных
     * @throws InstantiationException
     * @throws IOException
     * @throws IllegalAccessException 
     */
    public static DataBlock readDataBlock(DataInputStream stream, short typeIndex) throws InstantiationException, IOException, IllegalAccessException {
        Class type = types[typeIndex];
        DataBlock block = (DataBlock)type.newInstance();
        block.readData(stream);
        return block;
    }
    
    /**
     * Прочитать блок данных и соответствующий идентификатор
     * @param stream Поток
     * @return Пара - идентификатор, блок данных
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException 
     */
    public static Pair<Byte, DataBlock> readIdAndData(DataInputStream stream) throws IOException, IllegalAccessException, InstantiationException
    {
        short typeIndex = stream.readShort();
        Class type = types[typeIndex];
        DataBlock block = (DataBlock)type.newInstance();
        block.readData(stream);
        Pair<Byte, DataBlock> pair = new Pair(typeIndex, block);
        return pair;
    }
    
    /**
     * Записать блок данных
     * @param stream Поток
     * @param block Блок данных
     * @throws IOException 
     */
    public static void writeData(DataOutputStream stream, DataBlock block) throws IOException {
        short number = block.getBlockIdentificator();
        stream.writeShort(number);
        block.writeData(stream);
    }
    
    public static DataBlock convertToBlock(byte[] bytes) {
        DataBlock b = null;
        try(ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            try(DataInputStream in = new DataInputStream(stream)) {
                b = readData(in);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(WriterReader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(WriterReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch(IOException ex) {
            
        }
        return b;
    }
    public static byte[] convertToBytes(DataBlock block) {
        byte[] res = null;
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream(block.getByteCount())) {
            try(DataOutputStream out = new DataOutputStream(stream)) {
                writeData(out, block);
                res = stream.toByteArray();
            }
        } catch(IOException ex) {
            
        }
        return res;
    }
}
