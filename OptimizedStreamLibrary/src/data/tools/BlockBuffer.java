package data.tools;

import java.io.*;

public class BlockBuffer {
    public final int LONG_MESSAGE_SIZE;    //Длина в блоках, когда сообщение считается длинным

    private byte[] generalBuffer = null;
    private final int BLOCK_SIZE;   //размер блока = BS - IDENTIFICATION_BLOCK_BYTE_COUNT - IDENTIFICATION_SENDER_BYTE_COUNT)
    private final int M;       //Количество вызовов перед пересозданием буфера
    private int calledCount = 0;
    private int currentBlockCount = 0;    //Текущее количество блоков в буфере
    private int maxBlockCount = 0;        //Максимальное количество блоков, которое было за последние m вызовов средних сообщений

    private int factBlockCount = 0;
    private int lastByteCount = -1;

    protected byte[] getGeneralBuffer() {
        return generalBuffer;
    }

    public BlockBuffer(int blockSize, int longMessageSize, int callCount) {
        LONG_MESSAGE_SIZE = longMessageSize;
        BLOCK_SIZE = blockSize;
        changeBufferSize(1);
        if(callCount < 1) {
            throw new IllegalArgumentException("Количество вызовов для пересоздания буфера не может быть не положительным");
        }
        this.M = callCount;
    }

    protected void bufferSizeChanged(byte[] generalBuffer, int blockSize) {

    }

    private void changeBufferSize(int blockCount) {
        this.currentBlockCount = blockCount;
        this.generalBuffer = new byte[currentBlockCount * BLOCK_SIZE];
        bufferSizeChanged(generalBuffer, currentBlockCount);
        calledCount = 0;
    }

    protected final void updateByMessage(int receivedBlockCount) {
        if(currentBlockCount < receivedBlockCount) {
            //Если обрабатываемое сообщение больше размера буфера
            changeBufferSize(receivedBlockCount);
        } else {
            //Если обрабатываемое сообщение меньше или равно размеру буфера
            if (currentBlockCount > LONG_MESSAGE_SIZE) {
                if(receivedBlockCount != currentBlockCount) {
                    //Если было длинное сообщение, то всегда меняем размер буфера для экономии памяти
                    changeBufferSize(receivedBlockCount);
                }
            } else {
                //Среднее сообщение
                if (maxBlockCount < receivedBlockCount) {
                    maxBlockCount = receivedBlockCount;
                }
                calledCount++;
                if (calledCount >= M) {
                    calledCount = 0;
                    if (maxBlockCount < currentBlockCount)
                        changeBufferSize(maxBlockCount);     //Уменьшение размера буфера после M вызовов, если максимальный размер меньше текущего
                }
            }
        }
        lastByteCount = -1;
        factBlockCount = receivedBlockCount;
    }
    private final void updateByBlock(int blockIndex, int usefulBytes) {
        int lastBlockIndex = factBlockCount - 1;
        if(lastBlockIndex == blockIndex) {
            lastByteCount = usefulBytes;
        } else {
            if(usefulBytes != BLOCK_SIZE) {
                //Сбой сообщения
                updateByBlockFailed(blockIndex);
            }
        }
    }

    protected final int getBlockCount() {
        return factBlockCount;
    }
    protected final int getByteCount() {
//        if(lastByteCount == -1) {
//            throw new IllegalArgumentException("Нельзя оценить количество байтов до заполнения буфера");
//        }
        return factBlockCount * (BLOCK_SIZE - 1) + lastByteCount;
    }
    protected final int readBlockAt(int blockIndex, byte[] outLocalBuffer, int startIndex) {
        int byteCount = (blockIndex == factBlockCount - 1) ? lastByteCount : BLOCK_SIZE;
        System.arraycopy(generalBuffer, blockIndex * BLOCK_SIZE,
                outLocalBuffer, startIndex, byteCount);
        return byteCount;
    }
    protected final void writeBlockTo(int blockIndex, byte[] srcBuffer, int startIndex, int count) {
        updateByBlock(blockIndex, count);
        System.arraycopy(srcBuffer, startIndex, generalBuffer, blockIndex * BLOCK_SIZE, count);
    }
    protected final void updateBlockCountBySize(int messageSize) {
        factBlockCount = (messageSize + BLOCK_SIZE - 1) / BLOCK_SIZE;
        lastByteCount = messageSize % BLOCK_SIZE;
    }
    protected final void writeMessage(byte[] message) {
        int len = message.length;
        updateBlockCountBySize(len);
        updateByMessage(factBlockCount);      //Меняем размер буфера, если нужно
        System.arraycopy(message, 0, generalBuffer, 0, len);
    }
    protected final byte[] readMessage() {
        int count = getByteCount();
        byte[] result = new byte[count];
        System.arraycopy(generalBuffer, 0, result, 0, count);
        return result;
    }

    protected void updateByBlockFailed(int blockIndex) {
        throw new IllegalArgumentException("Блок " + blockIndex + " выдал ошибку: неверное количество байтов в блоке");
    }
}
