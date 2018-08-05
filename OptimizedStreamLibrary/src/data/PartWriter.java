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
public class PartWriter {
    
    byte[] smallReceive = new byte[1];     //ОК
    
    private final byte[] sendBuffer;     //Буфер для обмена данными (номер блока декрементируется, чтобы было понятно, сколько блоков осталось передать)
    
    private final int partSize;
    private final int identificateByteCount = 4; //4 - int для обозначения номера блока, остальное - полезная информация
    
    private byte[] message = null;
    private int blockNumber = -1;
    private int blockCount = -1;
    
    private final byte[] property;
    
    public void writeProperty(byte[] prop) {
        if(prop.length != property.length) throw new IllegalArgumentException("Длины массивов не совпадают!");
        System.arraycopy(prop, 0, property, 0, property.length);
    }
    
    public void readyMessage(byte[] message) {
        this.message = message;
        blockNumber = 0;
        blockCount = message.length / partSize + ((message.length % partSize != 0) ? 1 : 0);
    }
    public void flushMessage() {
        message = null;
        blockNumber = -1;
        blockCount = -1;
    }
    
    public byte[] getSendBuffer() { return sendBuffer; }
    
    private AbstractCryption cryption = new NonCryption();
    
    public void setCryption(AbstractCryption cryption) {
        this.cryption = cryption;
    }
    
    private StaticSenderReceiver senderReceiver;
    public void setStaticSenderReceiver(StaticSenderReceiver senderReceiver) {
        this.senderReceiver = senderReceiver;
    }
    
    /**
     * Инициализация класса частичной отправки
     * @param partSize Размер блока, включающий в себя 4 байта на номер блока и остальные байты на полезную информацию
     */
    public PartWriter(int partSize) {
        this(partSize, 0);
    }
    public PartWriter(int partSize, int propertyLength) {
        sendBuffer = new byte[partSize];
        property = new byte[propertyLength];
        this.partSize = partSize - identificateByteCount - propertyLength;
    }
    
    public static byte getB0(int integer) { return (byte)(integer); }
    public static byte getB1(int integer) { return (byte)(integer >> 8); };
    public static byte getB2(int integer) { return (byte)(integer >> 16); }
    public static byte getB3(int integer) { return (byte)(integer >> 24); }
    
    public static void writeInt(int value, byte[] bytes, int offset) {
        bytes[offset] = (byte)(value >> 24);
        bytes[offset + 1] = (byte)(value >> 16);
        bytes[offset + 2] = (byte)(value >> 8);
        bytes[offset + 3] = (byte)(value);
    }
    
    /**
     * Отправить порцию байтов
     * @param blockNumber Номер блока с прямого отсчёта
     * @param blockCount Количество блоков
     * @param bytes Байты
     */
    public void sendPart(int blockNumber, int blockCount, byte[] bytes) {
        //count <= partSize
        
        //Преобразование int в байты
        //--------------------------------------
        int inverseBlockNumber = blockCount - blockNumber;   //Нумерация от count до 1
        sendBuffer[0] = (byte)(inverseBlockNumber >> 24);
        sendBuffer[1] = (byte)(inverseBlockNumber >> 16);
        sendBuffer[2] = (byte)(inverseBlockNumber >> 8);
        sendBuffer[3] = (byte)(inverseBlockNumber);
        //--------------------------------------
        
        System.arraycopy(property, 0, sendBuffer, identificateByteCount, property.length);
        
        int startIndex = blockNumber * partSize;    //Стартовый индекс, откуда начинать считывание
        int count = (inverseBlockNumber != 1) ? partSize : bytes.length - startIndex;
        System.arraycopy(bytes, startIndex, sendBuffer, identificateByteCount + property.length, count);
        
        cryption.crypting(sendBuffer, count + identificateByteCount + property.length);
        senderReceiver.send(sendBuffer, count + identificateByteCount + property.length);
    }
    
    public void writeShortPart() {
        //count <= partSize
        
        //Преобразование int в байты
        //--------------------------------------
        int inverseBlockNumber = blockCount - blockNumber;   //Нумерация от count до 1
        sendBuffer[0] = (byte)(inverseBlockNumber >> 24);
        sendBuffer[1] = (byte)(inverseBlockNumber >> 16);
        sendBuffer[2] = (byte)(inverseBlockNumber >> 8);
        sendBuffer[3] = (byte)(inverseBlockNumber);
        //--------------------------------------
        
        int startIndex = blockNumber * partSize;    //Стартовый индекс, откуда начинать считывание
        int count = (inverseBlockNumber != 1) ? partSize : message.length - startIndex;
        System.arraycopy(message, startIndex, sendBuffer, identificateByteCount + property.length, count);
        
        cryption.crypting(sendBuffer, count + identificateByteCount + property.length);
        senderReceiver.send(sendBuffer, count + identificateByteCount + property.length);
        
        blockNumber++;
    }
    
    public void writeShortPartAgain() {
        blockNumber--;
        writeShortPart();
    }
    
    /**
     * Передача данных по частям
     * @param bytes Массив байтов
     * @throws java.io.IOException
     */
    public void writeMessage(byte[] bytes) {
        writeMessage(bytes, smallReceive);
    }
    
     public void writeMessage(byte[] bytes, byte[] messageOK) {
        int c;
        int blockCount = bytes.length / partSize + ((bytes.length % partSize != 0) ? 1 : 0);     //Количество блоков сообщения
        if(blockCount == 0) blockCount = 1;       //По предыдущей формуле сообщение длиной 0 оценивается в 0 блоков
        for(c = 0; c < blockCount - 1; c++) {
            sendPart(c, blockCount, bytes);
            senderReceiver.recv(messageOK);
        }
        sendPart(blockCount - 1, blockCount, bytes);
    }
}
