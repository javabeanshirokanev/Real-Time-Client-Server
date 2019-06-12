package data.tools;

public abstract class BlockBufferReader extends BlockBuffer {
    public final int IDENTIFICATION_BLOCK_BYTE_COUNT;    //Количество байт, выделенных под идентификацию номера блока
    public final int IDENTIFICATION_SENDER_BYTE_COUNT;   //Количество байт, выделенных под идентификацию отправителя сообщения

    private final int BS;      //Размер буфера сообщения
    private boolean isBufferAppending = false;   //Накапливается ли сейчас буфер
    private int blockIndex = 0;      //Индекс текущего блока
    private int receivedBlockCount = 0;    //Количество блоков в полученном сообщении

    public BlockBufferReader(int blockSize, int identificationBlockByteCount, int identificationSenderByteCount, int longMessageSize, int callCount) {
        super(blockSize, longMessageSize, callCount);
        IDENTIFICATION_BLOCK_BYTE_COUNT = identificationBlockByteCount;
        IDENTIFICATION_SENDER_BYTE_COUNT = identificationSenderByteCount;
        BS = blockSize + IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT;
        if(BS < 16) {
            throw new IllegalArgumentException("Размер буфера не может быть меньше 16");
        }
    }
    public BlockBufferReader() {
        this(1016, 4, 4, 16, 20);
    }

    public final void appendBuffer(byte[] messageBlock, int readedByteCount) {
        if(readedByteCount > BS) {
            //Сбой при получении данных
        }
        int inverseBlockNumber = ByteParser.bytes2int(messageBlock);
        if(inverseBlockNumber != 0 && readedByteCount != BS) {
            //Сбой при получении не последнего блока
        }
        int usefullByteCount = readedByteCount - IDENTIFICATION_BLOCK_BYTE_COUNT - IDENTIFICATION_SENDER_BYTE_COUNT;
        if(isBufferAppending) {
            int index = receivedBlockCount - inverseBlockNumber - 1;
            if(blockIndex != index) {
                //Сбой при передаче сообщений
                incorrectBlockObtained(blockIndex);
            } else {
                super.writeBlockTo(blockIndex, messageBlock, IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT, usefullByteCount);
                blockIndex++;
                if(inverseBlockNumber == 0) {
                    processMessage(super.getGeneralBuffer(), super.getByteCount());
                    isBufferAppending = false;
                    blockIndex = 0;
                }
            }
        } else {
            //Новое сообщение
            if(inverseBlockNumber == 0) {
                //Короткое сообщение
                super.updateByMessage(1);
                super.writeBlockTo(0, messageBlock, IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT, usefullByteCount);
                processMessage(super.getGeneralBuffer(), usefullByteCount);
                isBufferAppending = false;
                blockIndex = 0;
            } else {
                receivedBlockCount = inverseBlockNumber + 1;      //Находим количество блоков в сообщении
                isBufferAppending = true;    //Переключаемся в режим пополнения буфера
                super.updateByMessage(receivedBlockCount);
                super.writeBlockTo(0, messageBlock, IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT, usefullByteCount);
                blockIndex++;
            }
        }
    }

    public abstract void processMessage(byte[] message, int readedCount);

    public void incorrectBlockObtained(int blockIndex) {
        throw new IllegalArgumentException("Ожидался блок с индексом " + blockIndex);
    }
}
