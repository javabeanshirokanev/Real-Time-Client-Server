package data.tools;

public class ByteParser {
    private ByteParser() {

    }

    public static int bytes2int(byte[] bytes) {
        return (bytes[0] << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) <<  8) | ((bytes[3] & 0xff));
    }

    public static int bytes2int(byte[] bytes, int index) {
        return (bytes[index] << 24) | ((bytes[index + 1] & 0xff) << 16) | ((bytes[index + 2] & 0xff) <<  8) | ((bytes[index + 3] & 0xff));
    }

    public static void int2bytes(int value, byte[] outBuffer, int index) {
        outBuffer[index] = (byte)(value >> 24);
        outBuffer[index + 1] = (byte)(value >> 16);
        outBuffer[index + 2] = (byte)(value >> 8);
        outBuffer[index + 3] = (byte)(value);
    }
}
