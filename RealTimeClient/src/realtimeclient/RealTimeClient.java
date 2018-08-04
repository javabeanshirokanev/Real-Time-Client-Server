/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeclient;

import data.DataBlock;
import data.PartReader;
import data.PartWriter;
import data.SimpleUDPSenderReceiver;
import data.TimeOutUDPSenderReceiver;
import data.WriterReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Широканев Александр
 */
public class RealTimeClient {
    
    public static final int SEND_CONNECTION_FAILED = 0;
    public static final int RECEIVE_CONNECTION_FAILED = 1;
    public static final int MAX_CLIENTS_CONNECTION_FAILED = 2;
    public static final int NOT_QUERY_CONNECTION_FAILED = 3;
    public static final int CANNOT_CONNECT_FAILED = 4;
    public static final int INVALID_PARAMS_FAILED = 5;
    
    public static final int NON_CLIENTS_QUERY = 0;
    public static final int CANNOT_SEND_QUERY = 1;
    public static final int CLIENT_NOT_CONNECTED = 2;
    public static final int CLIENT_BANNED = 3;
    
    //protected final int MAX_MESSAGES = 1000;
    //===========================================================
    public static final byte NOTHING = 0;    //Ничего не делать
    public static final byte CONNECT = 1;    //Подключение клиента
    public static final byte DISCONNECT = 2;    //Отключение клиента
    public static final byte CLIENT_QUERY = 3;    //Срочный запрос у другого клиента (клиент должен ответить сразу же, не перемешивая с другими сообщениями)
    public static final byte UPDATE_MESSAGE = 4;    //Широковещательное обновляемое сообщение
    public static final byte QUERY = 5;        //Запрос серверу
    public static final byte UPDATE = 6;       //Обычный update (пустое сообщение)
    public static final byte CONNECT_ERROR = 7;    //Ошибка при подключении
    public static final byte CLIENT_QUERY_UNSWER = 8;     //Ответ на срочный запрос
    public static final byte CANNOT_CREATE_QUERY_ERROR = 9;   //Нельзя создать срочный запрос
    public static final byte CANNOT_CONNECT_ERROR = 10;      //Сервер не позволяет подключаться на данный момент
    public static final byte MAX_CLIENTS_ERROR = 11;      //Переполнение клиентов
    public static final byte NON_CLIENTS = 12;       //Сообщение об отсутствии клиентов
    public static final byte FINAL_CONNECT = 13;     //Сообщение о готовности к подключению
    public static final byte QUERY_FOR_CONNECT_ERROR = 14;  //Сообщение об ошибке отправки запроса клиенту
    public static final byte ACCELERATE_CLIENT = 15;     //Ускорение клиента
    public static final byte SERVER_CLOSE = 16;       //Сообщение об остановке сервера
    public static final byte RECONNECT = 17;      //Сообщение отправляется клиенту при необходимости клиенту переподключиться
    public static final byte ADMIN_AUTHENTIFICATION_ERROR = 18;   //Админ не прошел аутентификацию
    public static final byte CLIENT_NOT_CONNECTED_ERROR = 19;     //При отправке запроса было обнаружено, что клиент не подключен
    public static final byte BAN_ERROR = 20;     //Подключаемый клиент в чёрном списке
    public static final byte AGAIN = 21;         //Timeout отправленного сообщения, нужно отправить снова
    public static final byte NEXT_PART = 22;         //Timeout отправленного сообщения, нужно отправить снова
    
    public static final byte QUERY_CONNECTION_STATE = 0;
    public static final byte QUERY_CLIENT_STATE = 1;
    
    private int clientID = -1;     //Индекс клиента
    
    private final DatagramSocket socket;
    private InetSocketAddress mainAddress = null;
    
    private TimeOutUDPSenderReceiver senderReceiver;
    
    //private final byte[] sendData;
    //private int writedIndex = 0;          //Индекс, на котором остановились при отправке
    //private final byte[] receiveData;
    //private final DatagramPacket receivePacket;
    
    //private byte[] clientQueryMessage = null;
    //private byte[] queryMessage = null;
    //private byte[] updateMessage = null;

    byte[] message = null;
    private int partSize;
    
    private final int delay;
    private int iteration = 0;
    
    private PartReader reader;
    private PartWriter writer;
    
    private byte[] queryForNextPart = new byte[] {
        0, 0, 0, 1, 0, 0, 0, 0, NEXT_PART
    };   //Сообщение-запрос на следуюшую часть
    private byte[] queryForAgainMessage = new byte[] {
        0, 0, 0, 1, 0, 0, 0, 0, AGAIN
    };   //Сообщение-запрос на повторную отправку
    
    private QueryProcessor queryProcessor = null;
    
    public void setQueryProcessot(QueryProcessor processor) {
        this.queryProcessor = processor;
    }
    
    private List<ConnectedListener> conListeners = new ArrayList<>();
    private List<DisconnectedListener> disconListeners = new ArrayList<>();
    private List<UpdatedListener> updateListeners = new ArrayList<>();
    private List<FailedListener> failedListeners = new ArrayList<>();
    private List<StateGettedListener> stateGettedListeners = new ArrayList<>();
    private List<QueryReceivedListener> queryReceivedListeners = new ArrayList<>();
    
    public void addConnectedListener(ConnectedListener listener) { conListeners.add(listener); }
    public void removeConnectedListener(ConnectedListener listener) { conListeners.remove(listener); }
    public void clearConnectedListeners() { conListeners.clear(); }
    
    public void addDisconnectedListener(DisconnectedListener listener) { disconListeners.add(listener); }
    public void removeDisconnectedListener(DisconnectedListener listener) { disconListeners.remove(listener); }
    public void clearDisconnectedListeners() { disconListeners.clear(); }
    
    public void addUpdatedListener(UpdatedListener listener) { updateListeners.add(listener); }
    public void removeUpdatedListener(UpdatedListener listener) { updateListeners.remove(listener); }
    public void clearUpdatedListeners() { updateListeners.clear(); }
    
    public void addFailedListener(FailedListener listener) { failedListeners.add(listener); }
    public void removeFailedListener(FailedListener listener) { failedListeners.remove(listener); }
    public void clearFailedListeners() { failedListeners.clear(); }
    
    public void addStateGettedListener(StateGettedListener listener) { stateGettedListeners.add(listener); }
    public void removeStateGettedListener(StateGettedListener listener) { stateGettedListeners.remove(listener); }
    public void clearStateGettedListener() { stateGettedListeners.clear(); }
    
    public void addQueryReceivedListener(QueryReceivedListener listener) { queryReceivedListeners.add(listener); }
    public void removeQueryReceivedListener(QueryReceivedListener listener) { queryReceivedListeners.remove(listener); }
    public void clearQueryReceivedListener() { queryReceivedListeners.clear(); }
    
    public RealTimeClient(int FIXED_LENGTH, int timeout, int delay) throws SocketException {
        socket = new DatagramSocket();
        //receiveData = new byte[FIXED_LENGTH];
        //sendData = new byte[FIXED_LENGTH];
        //receivePacket = new DatagramPacket(receiveData, receiveData.length);
        partSize = FIXED_LENGTH;
        //socket.setSoTimeout(timeout);
        this.delay = delay;
        writer = new PartWriter(FIXED_LENGTH);
        reader = new PartReader(FIXED_LENGTH);
        senderReceiver = new TimeOutUDPSenderReceiver(FIXED_LENGTH, socket, timeout);
    }
    
    public int getClientID() { return clientID; }
    public boolean isConnected() { return mainAddress != null; }
    
    public void close() {
        socket.close();
    }
    
    /**
     * Добавить сообщение к отправке следующим обновлением
     * @param additingMessage Добавляемое сообщение
     */
    private void addMessage(byte[] additingMessage) {
        if(message == null) {
            message = additingMessage;
        } else {
            byte[] newArr = new byte[message.length];
            System.arraycopy(message, 0, newArr, 0, message.length);
            message = new byte[newArr.length + additingMessage.length];
            System.arraycopy(newArr, 0, message, 0, newArr.length);
            System.arraycopy(additingMessage, 0, message, newArr.length, additingMessage.length);
        }
    }
    private void sendQuery(byte[] message) {
        writer.writeMessage(message);
        reader.readMessage(queryForNextPart);
        byte[] buffer = reader.getBufferingMessage();
        int count = reader.getMessageLength();
        try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(buffer, 0, count))) {
            while(stream.available() > 0) {
                processQueryMessage(stream);
            }
        } catch(IOException e) {
            
        }
    }
    
    private void sendUpdatingMessage() {
        byte[] fullMessage = new byte[message.length + 5];
        fullMessage[0] = UPDATE_MESSAGE;
        PartWriter.writeInt(clientID, fullMessage, 1);
        System.arraycopy(message, 0, fullMessage, 5, message.length);
        writer.writeMessage(message);
        reader.readMessage(queryForNextPart);
        byte[] buffer = reader.getBufferingMessage();
        int count = reader.getMessageLength();
            
        try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(buffer, 0, count))) {
            while(stream.available() > 0) {
                processUpdateMessage(stream);
            }
        } catch(IOException e) {
                
        }
    }
    
    /**
     * Отправить обновляемое сообщение
     * @param block Сообщение
     */
    public void sendUpdatingMessage(DataBlock block) {
        int count = block.getByteCount();
        try(ByteArrayOutputStream bStream = new ByteArrayOutputStream(count + 5)) {
            try(DataOutputStream stream = new DataOutputStream(bStream)) {
                stream.writeByte(UPDATE_MESSAGE);
                stream.writeInt(clientID);
                block.writeData(stream);
                addMessage(bStream.toByteArray());
            }
        } catch(IOException e) {
            
        }
    }
    
    /**
     * Отправить обновляемое сообщение. Предполагается, что сообщение укладывается в размерность буфера
     * @param message Короткое сообщение
     * @throws IOException 
     */
    public void sendUpdatingMessage(byte[] message) throws IOException {
        byte[] fullMessage = new byte[message.length + 5];
        fullMessage[0] = UPDATE_MESSAGE;
        PartWriter.writeInt(clientID, fullMessage, 1);
        System.arraycopy(message, 0, fullMessage, 5, message.length);
        addMessage(fullMessage);
        
        //Старая версия
//        int diff = sendData.length - writedIndex;
//        boolean isStartIndex = writedIndex == 0;
//        int len = (isStartIndex) ? message.length + 3 : message.length;
//        if(len <= diff) {
//            if(isStartIndex) {
//                try (ByteArrayOutputStream writer = new ByteArrayOutputStream(sendData.length)) {
//                    DataOutputStream stream = new DataOutputStream(writer);
//                    stream.writeByte(UPDATE_MESSAGE);
//                    stream.writeShort(clientID);
//                    stream.write(message);
//                    byte[] arr = writer.toByteArray();
//                    System.arraycopy(arr, 0, sendData, 0, arr.length);
//                    writedIndex += len;
//                }
//            } else {
//                System.arraycopy(message, 0, sendData, writedIndex, message.length);
//                writedIndex += len;
//            }
//        } else {
//            //Отправка должна быть сложным пакетом с соответствующим параметром
//            throw new IOException("Сообщение слишком длинное");
//        }
    }
    
    /**
     * Отправить запрос у другого клиента
     * @param message Сообщение
     * @throws IOException 
     */
    public void sendClientQueryMessage(byte[] message) throws IOException {
        byte[] fullMessage = new byte[message.length + 5];
        fullMessage[0] = CLIENT_QUERY;
        PartWriter.writeInt(clientID, fullMessage, 1);
        System.arraycopy(message, 0, fullMessage, 5, message.length);
        addMessage(fullMessage);
        sendQuery(fullMessage);
    }
    
    public void sendClientStateQuery() throws IOException {
        sendClientQueryMessage(new byte[] { QUERY_CLIENT_STATE });
    }
    
    public void sendQueryMessage(byte[] message) throws IOException {
        byte[] fullMessage = new byte[message.length + 5];
        PartWriter.writeInt(clientID, fullMessage, 0);     //Нужен, чтобы по частям всё было
        fullMessage[4] = QUERY;
        System.arraycopy(message, 0, fullMessage, 5, message.length);
        addMessage(fullMessage);
        sendQuery(fullMessage);
        
//        if(queryMessage != null) {
//            //Формируем запрос
//            try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
//                DataOutputStream stream = new DataOutputStream(writer);
//                stream.writeByte(QUERY);
//                stream.write(message);
//                queryMessage = writer.toByteArray();    //Записываем сообщение
//            }
//        } else {
//            //Дополняем сообщение
//            byte[] newQuery = new byte[queryMessage.length + message.length];
//            System.arraycopy(queryMessage, 0, newQuery, 0, queryMessage.length);
//            System.arraycopy(message, 0, newQuery, queryMessage.length, message.length);
//            queryMessage = newQuery;
//        }
    }
    
    public void sendQueryMessage(DataBlock block) throws IOException {
        int count = block.getByteCount();
        try(ByteArrayOutputStream bStream = new ByteArrayOutputStream(count + 5)) {
            try(DataOutputStream stream = new DataOutputStream(bStream)) {
                stream.writeInt(clientID);
                stream.writeByte(QUERY);
                block.writeData(stream);
                addMessage(bStream.toByteArray());
            }
        } catch(IOException e) {
            
        }
    }
    
//    public byte[] getServerData(byte[] params) {
//        try (ByteArrayOutputStream writer = new ByteArrayOutputStream(1 + params.length)) {
//            DataOutputStream stream = new DataOutputStream(writer);
//            stream.writeByte(QUERY);
//            stream.write(params);
//            byte[] message = writer.toByteArray();    //Записываем сообщение
//            //Отправляем данные
//            DatagramPacket sendPacket = new DatagramPacket(message, message.length, mainAddress.getAddress(), mainAddress.getPort());
//            socket.send(sendPacket);
//            //Получаем данные
//            socket.receive(receivePacket);
//            byte[] unswer = new byte[receivePacket.getLength()];
//            System.arraycopy(receivePacket.getData(), 0, unswer, 0, unswer.length);
//            return unswer;
//        } catch(IOException e) {
//            return null;
//        }
//    }
    
    public void sendAdminCommandClose(byte[] params) throws IOException {
        try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
            DataOutputStream stream = new DataOutputStream(writer);
            stream.writeInt(clientID);
            stream.writeByte(SERVER_CLOSE);
            stream.write(params);
            //sendAdminCommand(writer.toByteArray());
            byte[] mes = writer.toByteArray();
            this.writer.writeMessage(mes);
            reader.readMessage(queryForNextPart);
            
        }
    }
    
//    private byte[] sendingOfMessage() throws IOException {
//        if(clientQueryMessage != null) {
//            byte[] mess = clientQueryMessage;
//            clientQueryMessage = null;
//            return mess;
//        } else {
//            if(queryMessage != null) {
//                byte[] mess = queryMessage;
//                queryMessage = null;
//                return mess;
//            } else {
//                if(updateMessage != null) {
//                    byte[] mess = updateMessage;
//                    updateMessage = null;
//                    return mess;
//                } else {
//                    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
//                        DataOutputStream stream = new DataOutputStream(writer);
//                        stream.writeByte(UPDATE);
//                        stream.writeShort(clientID);
//                        return writer.toByteArray();    //Пустое обновление
//                    }
//                }
//            }
//        }
//    }
    
    public void connect(InetAddress ip, int port) {
        senderReceiver.setEndPoint(ip, port);
        writer.setStaticSenderReceiver(senderReceiver);
        
        //Отправляем запрос на подключение и получаем id клиента
        //===================================================
        byte[] connectMessage = new byte[4];
        PartWriter.writeInt(-1, connectMessage, 0);    //-1 говорит о том, что клиенту нужен ID
        writer.writeMessage(connectMessage);
        reader.readMessage();     //Это сообщение короткое
        byte[] unswer = reader.getMessage();    //Не больше 5 байт
        try (ByteArrayInputStream readerStream = new ByteArrayInputStream(unswer)) {
            DataInputStream stream = new DataInputStream(readerStream);
            byte type = stream.readByte();
            if(type == CONNECT) {
                clientID = stream.readInt();   //Получили ID
                PartWriter.writeInt(clientID, queryForNextPart, 4);
            } else {
                //Ошибка
                switch(type) {
                    case MAX_CLIENTS_ERROR:
                        for(ConnectedListener listener : conListeners)
                            listener.connectionFailed(this, MAX_CLIENTS_CONNECTION_FAILED);
                        break;
                    case CANNOT_CONNECT_ERROR:
                        for(ConnectedListener listener : conListeners)
                            listener.connectionFailed(this, CANNOT_CONNECT_FAILED);
                        break;
                }
                return;
            }
        } catch(IOException e) {
            for(ConnectedListener listener : conListeners)
                listener.connectionFailed(this, RECEIVE_CONNECTION_FAILED);
            return;
        }
        //===================================================
            
        
        byte[] params = getConnectionParameters();
        byte[] finalConnectMessage = new byte[params.length + 5];
        finalConnectMessage[0] = FINAL_CONNECT;
        PartWriter.writeInt(clientID, finalConnectMessage, 1);
        System.arraycopy(params, 0, connectMessage, 5, params.length);
        
        writer.writeMessage(finalConnectMessage);
        reader.readMessage(queryForNextPart);
        byte[] finalUnswer = reader.getMessage();
        
        try (ByteArrayInputStream reader = new ByteArrayInputStream(finalUnswer)) {
            DataInputStream stream = new DataInputStream(reader);
            byte type = stream.readByte();    //Тип сообщения
            if(type == FINAL_CONNECT) {
                this.iteration = stream.readInt();
                for(StateGettedListener listener : stateGettedListeners) {
                    listener.clientStateGetted(this, stream, stream.available());
                }
                mainAddress = new InetSocketAddress(ip, port);
                //С этого момента клиент подключен...
                for(ConnectedListener listener : conListeners)
                    listener.connected(this);
            } else {
                //Ошибка
                switch(type) {
                    case INVALID_PARAMS_FAILED :
                        for(ConnectedListener listener : conListeners)
                            listener.connectionFailed(this, INVALID_PARAMS_FAILED);
                        break;
                    case QUERY_FOR_CONNECT_ERROR :
                        for(ConnectedListener listener : conListeners)
                            listener.connectionFailed(this, NOT_QUERY_CONNECTION_FAILED);
                        break;
                }
            }
        } catch(IOException e) {
            
        }
    }
    
//    private void sendAdminCommand(byte[] adminCommand) {
//        if(mainAddress == null) return;
//        try
//        {
//            DatagramPacket sendPacket = new DatagramPacket(adminCommand, adminCommand.length, mainAddress);
//            socket.send(sendPacket);    //Отправляем данные
//        } catch(IOException e) {
//            //Записать в лог проблему
//            return;
//        }
//        
//        try
//        {
//            socket.receive(receivePacket);     //Приняли данные
//            byte[] receivedMessage = new byte[receivePacket.getLength()];
//            System.arraycopy(receivePacket.getData(), 0, receivedMessage, 0, receivedMessage.length);
//            adminProcessMessage(receivedMessage);
//        } catch(IOException e) {
//            //Если клиент долго не принимает данные, значит он отключается. При этом он не может отправить сообщение об отключении
//            
//        }
//    }
    
    private void flush() {
        mainAddress = null;
        message = null;
            
//        writedIndex = 0;
//        clientQueryMessage = null;
//        queryMessage = null;
    }
    
    private void adminProcessMessage(byte[] message) throws IOException {
        byte type = message[0];
        if(type == SERVER_CLOSE) {
            flush();
            for(DisconnectedListener listener : disconListeners) listener.serverClosed(this);
        }
        if(type == ADMIN_AUTHENTIFICATION_ERROR) {
            for(FailedListener listener : failedListeners)
                listener.adminAuthentificationError(this);
        }
    }
    
    private void processQueryMessage(DataInputStream stream) throws IOException {
        byte messageType = stream.readByte();
        if(messageType == CANNOT_CREATE_QUERY_ERROR) {
            for(FailedListener listener : failedListeners) {
                listener.clientQueryUnswerError(this, CANNOT_SEND_QUERY);
            }
            return;
        }
        if(messageType == QUERY) {
            for(QueryReceivedListener listener : queryReceivedListeners) {
                listener.serverQueryReceived(this, stream);
            }
        }
//        if(messageType == CLIENT_QUERY_UNSWER) {
//            byte queryType = stream.readByte();
//            if(queryType == QUERY_CONNECTION_STATE || queryType == QUERY_CLIENT_STATE) {
//                for(StateGettedListener listener : stateGettedListeners) {
//                    listener.clientStateGetted(stream, stream.available());
//                }
//            } else {
//                for(QueryReceivedListener listener : queryReceivedListeners) {
//                    listener.queryUnswerReceived(queryType, stream);
//                }
//            }
//        }
    }
    
    private void processUpdateMessage(DataInputStream stream) throws IOException {
        byte messageType = stream.readByte();
        if(messageType == ACCELERATE_CLIENT) {
            int deltaTime = stream.readInt();
            for(int i = 0; i < deltaTime; i++) {
                for(UpdatedListener listener : updateListeners)
                    listener.dataUpdated(this);
            }
            return;
        }
        if(messageType == CLIENT_NOT_CONNECTED_ERROR) {
            for(ConnectedListener listener : conListeners)
                listener.connectionFailed(this, CLIENT_NOT_CONNECTED);
            return;
        }
        if(messageType == BAN_ERROR) {
            for(ConnectedListener listener : conListeners)
                listener.connectionFailed(this, CLIENT_BANNED);
            return;
        }
        if(messageType == NON_CLIENTS) {
            for(FailedListener listener : failedListeners) {
                listener.clientQueryUnswerError(this, NON_CLIENTS_QUERY);
            }
            return;
        }
        if(messageType == CANNOT_CREATE_QUERY_ERROR) {
            for(FailedListener listener : failedListeners) {
                listener.clientQueryUnswerError(this, CANNOT_SEND_QUERY);
            }
            return;
        }
//        if(messageType == CLIENT_QUERY) {
//            byte addressIndex = stream.readByte();    //Индекс адреса. Клиента перенаправляет сообщение с этим индексом
//            byte queryType = stream.readByte();
//            byte[] unswer;
//            if(queryType == QUERY_CONNECTION_STATE) {
//                unswer = queryProcessor.queryConnectionUnswerSending();
//            } else {
//                unswer = queryProcessor.queryUnswerSending(queryType);
//            }
//            byte[] sendingMessage;
//
//            try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(3 + (Integer.SIZE >> 3) + unswer.length)) {
//                DataOutputStream writer = new DataOutputStream(byteOutput);
//                writer.writeByte(CLIENT_QUERY_UNSWER);      //Пишем message_type
//                writer.writeByte(addressIndex);
//                writer.writeInt(clientID);
//                writer.writeByte(queryType);
//                writer.write(unswer);
//                sendingMessage = byteOutput.toByteArray();
//            }
//            
//            writer.writeMessage(sendingMessage);
//            reader.readMessage();
//            //processMessage(receivedMessage);
//            return;
//        }
        if(messageType == UPDATE) {
            for(UpdatedListener listener : updateListeners)
                listener.dataUpdated(this);    //Событие обновления без полученных команд
        }
        if(messageType == UPDATE_MESSAGE) {
            for(UpdatedListener listener : updateListeners) {
                listener.updatingMessageReceived(this, stream);    //Событие обработки сообщений
                listener.dataUpdated(this);    //Событие обновления без полученных команд
            }
        }
        if(messageType == SERVER_CLOSE) {
            flush();
            for(DisconnectedListener listener : disconListeners) {
                listener.serverClosed(this);
            }
        }
        if(messageType == DISCONNECT) {
            flush();
            for(DisconnectedListener listener : disconListeners)
                listener.disconnected(this);
        }
    }
    
    public void update() {
        
        iteration++;
        
        //iteration = (iteration + 1) % delay;
        if(iteration % delay != 0) {
            //Просто обновляем данные
            for(UpdatedListener listener : updateListeners) {
                listener.dataUpdated(this);
            }
            return;
        }
        if(mainAddress == null) return;
        sendUpdatingMessage();
    }
    
    public void disconnect() {
        if(mainAddress == null) return;
        try
        {
//            byte[] message = listener.disconnectUpdatingMessageSending();    //???
//            if(message != null) {
//                sendUpdatingMessage(message);
//                do {
//                    update();
//                } while(iteration != 0);
//            }
            byte[] disconnectMessage;
            try(ByteArrayOutputStream stream = new ByteArrayOutputStream(1 + (Short.SIZE >> 3))) {
                DataOutputStream writer = new DataOutputStream(stream);
                writer.writeInt(clientID);
                writer.writeByte(DISCONNECT);
                disconnectMessage = stream.toByteArray();
            }
            
            writer.writeMessage(disconnectMessage);
            //Может быть надо получить сообщение ещё
            flush();
        } catch(IOException e) {
            //Записать в лог проблему
        }
    }
    
    protected byte[] getConnectionParameters() { return new byte[0]; }
    
}
