package data.tools;

public class BlockBufferWriter extends BlockBuffer {
    public final int IDENTIFICATION_BLOCK_BYTE_COUNT;    //Количество байт, выделенных под идентификацию номера блока
    public final int IDENTIFICATION_SENDER_BYTE_COUNT;   //Количество байт, выделенных под идентификацию отправителя сообщения

    private final int BS;      //Размер буфера сообщения
    private boolean isSending = false;   //Накапливается ли сейчас буфер
    private int blockIndex = 0;      //Индекс текущего блока

    private final byte[] id;

    public BlockBufferWriter(int blockSize, int identificationBlockByteCount, int identificationSenderByteCount, int longMessageSize, int callCount) {
        super(blockSize, longMessageSize, callCount);
        IDENTIFICATION_BLOCK_BYTE_COUNT = identificationBlockByteCount;
        IDENTIFICATION_SENDER_BYTE_COUNT = identificationSenderByteCount;
        BS = blockSize + IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT;
        if(BS < 16) {
            throw new IllegalArgumentException("Размер буфера не может быть меньше 16");
        }
        id = new byte[IDENTIFICATION_SENDER_BYTE_COUNT];
    }

    protected void setId(byte[] id) {
        System.arraycopy(id, 0,
                this.id, 0, this.id.length);
    }

    public BlockBufferWriter() {
        this(1016, 4, 4, 16, 20);
    }

    protected final void readyForSend(int byteCount) {
        super.updateBlockCountBySize(byteCount);
        super.updateByMessage(super.getBlockCount());
        blockIndex = 0;
    }

    protected final int updateForSend(byte[] outBlock) {
        ByteParser.int2bytes(blockIndex, outBlock, 0);
        System.arraycopy(id, 0,
                outBlock, IDENTIFICATION_BLOCK_BYTE_COUNT, IDENTIFICATION_SENDER_BYTE_COUNT);
        int count = super.readBlockAt(blockIndex, outBlock, IDENTIFICATION_BLOCK_BYTE_COUNT + IDENTIFICATION_SENDER_BYTE_COUNT);
        blockIndex++;
        return count;
    }
}
