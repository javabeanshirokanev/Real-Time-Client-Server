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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Широканев Александр
 */
public class PartReader {
    
    byte[] smallReceive = new byte[1];     //ОК
    
    private final byte[] recvBuffer;   //Буфер получаемых данных
    private int bufferLength;         //Количество байт, полученных в последний раз
    private byte[] message = null;       //Сообщение, которое нужно получить
    private int messageLength = -1;      //Длина сообщения
    private int blockCount = -1;         //Количество блоков
    private int lastRestBlockCount = 1;    //Сколько блоков осталось считать
    
    private final int partSize;
    private final int identificateByteCount = 4; //4 - int для обозначения номера блока, остальное - полезная информация
    
    public boolean isMessageReaded() {
        return lastRestBlockCount == 1;
    }
    
    public byte[] getRecvBuffer() { return recvBuffer; }
    public int getBufferLength() { return bufferLength; }
    
    private AbstractCryption cryption = new NonCryption();
    
    public void setCryption(AbstractCryption cryption) {
        this.cryption = cryption;
    }
    
    public StaticSenderReceiver getStaticSenderReceiver() {
        return senderReceiver;
    }
    
    private StaticSenderReceiver senderReceiver;
    public void setStaticSenderReceiver(StaticSenderReceiver senderReceiver) {
        this.senderReceiver = senderReceiver;
    }
    
    private void receivedEvent() {
        for(DataReceivedListener listener : listeners) {
            try(ByteArrayInputStream stream = new ByteArrayInputStream(message, 0, messageLength)) {
                try(DataInputStream in = new DataInputStream(stream)) {
                    listener.dataReceived(this, in);    //Событие полного чтения сообщения
                }
            } catch(IOException e) {

            }
        }
    }
    
    private final List<DataReceivedListener> listeners = new ArrayList<>();
    
    public void addDataReceivedListener(DataReceivedListener listener) {
        listeners.add(listener);
    }
    public void removeDataReceivedListener(DataReceivedListener listener) {
        listeners.remove(listener);
    }
    public void clearDataReceivedListener() {
        listeners.clear();
    }
    
    public void writeOK() {
        senderReceiver.send(smallReceive, 1);
    }
    
    /**
     * Инициализация класса частичной отправки
     * @param partSize Размер блока, включающий в себя 4 байта на номер блока и остальные байты на полезную информацию
     */
    public PartReader(int partSize) {
        recvBuffer = new byte[partSize];
        this.partSize = partSize - identificateByteCount;
    }
    
//    public void setMessage(byte[] bytes) {
//        message = bytes;
//        blockCount = message.length / partSize + message.length % partSize != 0 ? 1 : 0;     //Количество блоков сообщения
//    }
    public byte[] getBufferingMessage() {
        return message;
    }
    public void resetMessage() {
        message = null;
        blockCount = -1;
        messageLength = -1;
    }
    public int getMessageLength() {
        return messageLength;
    }
    
    public byte[] getMessage() {
        byte[] resMessage = new byte[messageLength];
        System.arraycopy(this.message, 0, resMessage, 0, messageLength);
        return resMessage;
    }
    
    public static int getInt(byte b0, byte b1, byte b2, byte b3) {
        return (b3 << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) <<  8) | ((b0 & 0xff));
    }
    
    public static int readInt(byte[] bytes, int offset) {
        return getInt(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
    }
    
    public void recvPart() {
        int byteCount = senderReceiver.recv(recvBuffer);    //Сколько байт считано
        this.bufferLength = byteCount;
        cryption.uncrypting(recvBuffer, byteCount);
        
        //Преобразование байтов в int
        //--------------------------------------
        int inverseBlockNumber =
                (recvBuffer[3] << 24) |
                ((recvBuffer[2] & 0xff) << 16) |
                ((recvBuffer[1] & 0xff) <<  8) |
                ((recvBuffer[0] & 0xff));
        int usefullByteCount = byteCount - identificateByteCount;
        lastRestBlockCount = inverseBlockNumber;
        //--------------------------------------
        
        if(message == null) {    //Если сообщение ещё не получали
            blockCount = inverseBlockNumber;    //Отсчёт начинается с 1
            message = new byte[blockCount * partSize];
            messageLength = -1;    //Пока что сообщение не считано
        }
        
        int blockNumber = blockCount - inverseBlockNumber;
        int startIndex = blockNumber * partSize;    //Стартовый индекс, с которого начинать запись
        System.arraycopy(recvBuffer, identificateByteCount, message, startIndex, usefullByteCount);
        
        if(inverseBlockNumber == 1) {   //Последний блок
            messageLength = startIndex + usefullByteCount;   //Сообщение считается считанным
            receivedEvent();
        }
    }
    
    /**
     * Считать блок данных. Первые 4 байта - это номер блока
     */
    public void readShortPart() {
        int byteCount = senderReceiver.recv(recvBuffer);    //Сколько байт считано
        cryption.uncrypting(recvBuffer, byteCount);
        this.bufferLength = byteCount;
        int inverseBlockNumber =
                (recvBuffer[3] << 24) |
                ((recvBuffer[2] & 0xff) << 16) |
                ((recvBuffer[1] & 0xff) <<  8) |
                ((recvBuffer[0] & 0xff));
        lastRestBlockCount = inverseBlockNumber;
        //В буфере хранится полученная информация
    }
    public void appendBufferFromPartReader(PartReader receivedReader) {
        int receivedByteCount = receivedReader.bufferLength - identificateByteCount;
        byte[] buf = receivedReader.recvBuffer;
        int blockNumber = receivedReader.blockCount - receivedReader.lastRestBlockCount;
        //Копирование буфера в сообщение
        //---------------------------------------------------------
        int startIndex = blockNumber * partSize;    //Стартовый индекс, с которого начинать запись
        System.arraycopy(buf, identificateByteCount, message, startIndex, receivedByteCount);
        //---------------------------------------------------------
        if(receivedReader.lastRestBlockCount == 1) {
            messageLength = startIndex + receivedByteCount;
            receivedEvent();
        }
    }
    
    
    /**
     * Считать данные (Информация о полученных данных извлекается методами getMessage, getMessageLength)
     */
    public void readMessage() {
        readMessage(smallReceive);
    }
    
    public void readMessage(byte[] messageOK) {
        recvPart();
        while(messageLength == -1) {
            senderReceiver.send(messageOK, messageOK.length);
            recvPart();
        }
    }
}
