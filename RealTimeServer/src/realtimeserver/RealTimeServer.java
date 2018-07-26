/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeserver;

import data.DataReceivedListener;
import data.PartReader;
import data.PartWriter;
import data.SimpleUDPSenderReceiver;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Широканев Александр
 */
public class RealTimeServer {

    protected final int FIXED_LENGTH;
    protected final int MAX_CLIENT_COUNT;
    //protected final byte MAX_ADDRESSES;
    //protected final int MAX_MESSAGES = 1000;
    //===========================================================
    public static final byte NOTHING = 0;    //Ничего не делать
    public static final byte CONNECT = 1;    //Подключение клиента
    public static final byte DISCONNECT = 2;    //Отключение клиента
    public static final byte CLIENT_QUERY = 3;    //Срочный запрос у клиента (клиент должен ответить сразу же, не перемешивая с другими сообщениями)
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
    //Добавить команды для информации о сервере (его состояние), информации о клиентах, возможность банить
    
    public static final byte QUERY_CONNECTION_STATE = 0;
    public static final byte QUERY_CLIENT_STATE = 1;
    
    //Информация о клиентах
    //----------------------
    private int clientCount = 0;
    private InetSocketAddress[] addrs;   //Конечные точки клиентов (null - клиент не подключен к слоту)
    private int[] times;   //Времена клиентов
    //private int[] waitTime = new int[MAX_CLIENT_COUNT];   //Время ожидания каждого клиента
    private int minTime = 0;   //Минимальное время из всех клиентов
    private int maxTime = 0;   //Максимальное время из всех клиентов
    protected int accelerationTime = 60;    //Время до ускорения клиента
    protected int disconnectTime = 1000;    //Время до отключения клиента
    private final PartWriter[] clientWriters;    //Потоки-записыватели для клиентов
    private final PartReader[] clientReaders;
    private final PartReader receiver;       //Поток-считыватель
    private final PartWriter writer;         //Поток-записыватель
    private final SimpleUDPSenderReceiver udpSenderReceiver;     //Отправитель, получатель
    
    private final List<InetAddress> blackList = new ArrayList<>();
    //----------------------
    
    private final ServerState state;
    
    public int getClientCount() { return clientCount; }
    
    //Информация сервера
    //---------------------------
    private byte[] serverParams;
    //---------------------------
    
    /**
     * Параметры сервера для доступа к нему (аутентификация админа)
     * @param params Параметры
     */
    public void setParams(byte[] params) {
        this.serverParams = params;
    }
    
    //Глобальные данные для пересылки клиентам
    //Широковещательные сообщения для обновления данных
    //----------------------
    private final ArrayList<byte[]> messages = new ArrayList<>();   //Сообщения
    private final ArrayList<Integer> messageTimes = new ArrayList<>();  //Времена сообщений
    private final ArrayList<Boolean> isSended = new ArrayList<>();  //Отправленное кому-то сообщение уже нельзя отправлять кому-то еще
    //----------------------
    //Срочный запрос
    //----------------------
    //private final ArrayList<byte[]> priorityMessages = new ArrayList<>();
    //private final ArrayList<Byte> sourceClientAddressIndexes = new ArrayList<>();   //Клиент, который отправил запрос
    //private final ArrayList<Integer> finalClientIds = new ArrayList<>();         //-1 - любому клиенту
    //private final InetSocketAddress[] addresses;
    //----------------------
    
    
    private DatagramSocket serverSocket = null;
    protected boolean isCanConnect = true;       //Могут ли клиенты подключаться на данный момент?
    //protected boolean isNeedConnectionPriorityQuery = true;    //Нужны ли срочные запросы для подключения
    //protected int priorityClientID = -1;         //Клиент, которому отправляются запросы при подключении (-1 - если любой клиент)
    
    public boolean isWork() { return serverSocket != null; }
    
    //private final byte[] receiveData;
    //private final DatagramPacket receivePacket;
    //private byte[] sendData;
    
    public RealTimeServer(int port, ServerState state) throws SocketException
    {
        this(port, 1000, 1024, state);
    }
    
    public RealTimeServer(int port, 
            int MAX_CLIENT_COUNT, int FIXED_LENGTH, ServerState state) throws SocketException
    {
        serverSocket = new DatagramSocket(port);
        udpSenderReceiver = new SimpleUDPSenderReceiver(FIXED_LENGTH, serverSocket);
       // this.isNeedConnectionPriorityQuery = isNeedConnectionPriorityQuery;
        
        this.MAX_CLIENT_COUNT = MAX_CLIENT_COUNT;
        //this.MAX_ADDRESSES = MAX_ADDRESSES;
        this.FIXED_LENGTH = FIXED_LENGTH;
        
        addrs = new InetSocketAddress[MAX_CLIENT_COUNT];
        times = new int[MAX_CLIENT_COUNT];
        //addresses = new InetSocketAddress[MAX_ADDRESSES];
        
        receiver = new PartReader(FIXED_LENGTH);
        writer = new PartWriter(FIXED_LENGTH);
        receiver.setStaticSenderReceiver(udpSenderReceiver);
        writer.setStaticSenderReceiver(udpSenderReceiver);
        clientWriters = new PartWriter[MAX_CLIENT_COUNT];
        clientReaders = new PartReader[MAX_CLIENT_COUNT];
        for(int i = 0; i < MAX_CLIENT_COUNT; i++) {
            clientWriters[i] = new PartWriter(FIXED_LENGTH);
            clientReaders[i] = new PartReader(FIXED_LENGTH);
            clientWriters[i].setStaticSenderReceiver(udpSenderReceiver);
            clientReaders[i].setStaticSenderReceiver(udpSenderReceiver);
        }
        this.state = state;
        
        //dbInfo.connectDB("localhost:1527");    //Подключаемся к БД
        //dbInfo.disconnectDB();
    }
    
//    public void runAgain(int port) throws SocketException {
//        if(serverSocket.isClosed() == false) serverSocket.close();
//        serverSocket = new  DatagramSocket(port);
//    }
    
    public void update()
    {
        receiver.readShortPart();    //Ожидаем кусок
        byte[] buf = receiver.getRecvBuffer();
        int clientID = PartReader.readInt(buf, 4);   //Первые 4 байта - это номер блока
        InetAddress ip = udpSenderReceiver.getLastIp();
        int port = udpSenderReceiver.getLastPort();
        if(clientID == -1) {
            try {
                connect(ip, port);
            } catch(IOException e) {
                
            }
        } else {
            PartReader currentReader = clientReaders[clientID];
            currentReader.appendBufferFromPartReader(receiver);    //Добавляем сообщение (при полном считывании произойдёт событие)
            if(currentReader.isMessageReaded()) {                
                try(ByteArrayInputStream stream = new ByteArrayInputStream(currentReader.getBufferingMessage(), 8, currentReader.getMessageLength())) {
                    try(DataInputStream in = new DataInputStream(stream)) {
                        processQuery(clientID, in, ip, port);
                    }
                } catch(IOException e) {

                }
            } else {
                currentReader.writeOK();
            }
        }
    }
    
    private void sendToClient(int clientID, byte[] message) throws IOException {
        InetSocketAddress address = addrs[clientID];
        udpSenderReceiver.setEndPoint(address.getAddress(), address.getPort());   //Такое не позволит многопоточности работать!!! Надо для каждого клиента свой updSenderReceiver
        clientWriters[clientID].readyMessage(message);
        clientWriters[clientID].writeShortPart();
    }
    private void sendToClient(InetAddress ip, int port, byte[] message) throws IOException {
        udpSenderReceiver.setEndPoint(ip, port);
        writer.readyMessage(message);
        writer.writeShortPart();
    }
    private void sendToClient(InetSocketAddress address, byte[] message) throws IOException {
        udpSenderReceiver.setEndPoint(address.getAddress(), address.getPort());
        writer.readyMessage(message);
        writer.writeShortPart();
    }
    
    private void removeUnusingMessages() {
        minTime = getMinClientTime();            //КАК ОПТИМИЗИРОВАТЬ, чтобы в процессе приема выяснять минимальное время???
        //Нужно удалить все сообщения, у которых time < minTime
        while(messages.size() > 0) {
            if(messageTimes.get(0) >= minTime) break;   //Время первого сообщения. Учитываем, что сообщения отсортированы по времени
            //Если сообщение уже не будет никому отправлено
            messages.remove(0);
            messageTimes.remove(0);
            isSended.remove(0);
        }
    }
    
    private byte[] getUpdateMessageForClient(int clientID) {
        int time = times[clientID];   //Время клиента
        int messageID = messageTimes.indexOf(time);   //Ищем индекс сообщения с таким временем (Не может быть двух сообщений с одинаковым номером)
        if(messageID == -1) {
            return new byte[] { UPDATE };
        } else {
            isSended.set(messageID, Boolean.TRUE);      //Сообщение считается отправленным
            byte[] mess = messages.get(messageID);

            if(messageTimes.get(messageID) < minTime) {
                //Если сообщение уже не будет никому отправлено
                messages.remove(messageID);
                messageTimes.remove(messageID);
                isSended.remove(messageID);
            }
            return mess;
        }
    }
    
    private static final int CLIENT_ID_LENGTH = 4;
    
    //Добавление сообщения от клиента
    private void addUpdateMessageFromClient(byte[] message, int clientID) {
        int time = times[clientID];
        byte[] mainMessage = new byte[message.length - (CLIENT_ID_LENGTH + 1)];    //Сообщение без типа сообщения и номера клиента
        System.arraycopy(message, CLIENT_ID_LENGTH + 1, mainMessage, 0, mainMessage.length);
        int messageIndex = messageTimes.indexOf(time);   //Находим время сообщения, совпадающее со временем клиента
        
        boolean isContain = (messageIndex != -1);
        isContain = (isContain) ? isSended.get(messageIndex) == false : false;
        if(isContain) {
            //Если нашли сообщение, и оно еще не отправлялось никому, то объединяем
            byte[] oldMessage = messages.get(messageIndex);
            byte[] newMessage = new byte[oldMessage.length + mainMessage.length];
            System.arraycopy(oldMessage, 0, newMessage, 0, oldMessage.length);
            System.arraycopy(mainMessage, 0, newMessage, oldMessage.length, mainMessage.length);
            messages.set(messageIndex, newMessage);    //Заменяем сообщение
        } else {
            //Добавление нового сообщения
            int newMessageTime = maxTime + 1;    //Новое время
            byte[] addMessage = new byte[mainMessage.length + 1];   //Добавляемое сообщение (без номера клиента)
            addMessage[0] = UPDATE_MESSAGE;
            System.arraycopy(mainMessage, 0, addMessage, 1, mainMessage.length);
            messages.add(addMessage);
            messageTimes.add(newMessageTime);
            isSended.add(false);
        }
    }
    
//    private int addAddress(InetSocketAddress address) {
//        for(int i = 0; i < addresses.length; i++) {
//            if(addresses[i] == null) {
//                addresses[i] = address;
//                return i;
//            }
//        }
//        return -1;
//    }
    
    //Создать запрос и вернуть, был ли он создан
//    private boolean createQuery(InetAddress clientIPAddress, int clientPort, int toClientID, byte[] message) {
//        int addressIndex = addAddress(new InetSocketAddress(clientIPAddress, clientPort));   //Добавляем адрес отправителя
//        if(addressIndex == -1) return false;
//        priorityMessages.add(message);  //Добавляем срочное сообщение
//        sourceClientAddressIndexes.add((byte)addressIndex);
//        finalClientIds.add(toClientID);
//        return true;
//    }
    
    //Отправить запрос и вернуть состояние - был ли отправлен запрос
//    private boolean trySendQuery(int clientID) throws IOException {
//        int queryIndex = finalClientIds.indexOf(clientID);   //Ищем индекс запроса с номером клиента
//        if(queryIndex == -1) {
//            queryIndex = finalClientIds.indexOf(-1);   //Ищем индекс запроса с -1
//        }
//        if(queryIndex == -1) return false;   //Срочный запрос не найден
//        //Если нашёлся срочный запрос для данного клиента, отправляем ему
//        byte addressIndex = sourceClientAddressIndexes.get(queryIndex);   //Получаем индекс адреса
//        byte[] message = priorityMessages.get(queryIndex);     //Срочное сообщение
//        byte[] resultMessage = new byte[2 + message.length];
//        resultMessage[0] = CLIENT_QUERY;
//        resultMessage[1] = addressIndex;
//        System.arraycopy(message, 0, resultMessage, 2, message.length);     // [ CLIENT_QUERY, addressIndex, Message ]
//        sendToClient(clientID, resultMessage);    //Отправляем сообщение
//        //Стираем срочный запрос, так как уже отправлен
//        priorityMessages.remove(queryIndex);
//        sourceClientAddressIndexes.remove(queryIndex);
//        finalClientIds.remove(queryIndex);
//        return true;
//    }
    
    private int addClient(InetSocketAddress address, int time) {
        for(int i = 0; i < addrs.length; i++) {
            if(addrs[i] == null) {
                //Нашли свободный слот
                addrs[i] = address;
                times[i] = time;
                clientCount++;
                return i;
            }
        }
        return -1;
    }
    
    private void removeClient(int clientID) {
        addrs[clientID] = null;
        times[clientID] = 0;
        clientCount--;
    }
    
    private int nearestMessageByTime(int time) {
        for(Integer messageTime : messageTimes) {
            if(time <= messageTime) {
                return messageTime;
            }
        }
        return time;
    }
    
    private int getMinClientTime() {
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < times.length; i++) {
            if(addrs[i] != null) {
                if(times[i] < min) min = times[i];
            }
        }
        return min;
    }
    
    private void updateClient(int clientID, DataInputStream stream) throws IOException {
        removeUnusingMessages();      //Можно поставить в то место, где вызывается реже. Нужен как периодический сборщик мусора
        int clientTime = times[clientID];
        int deltaClientTime = maxTime - clientTime;     //Время клиента
        if(deltaClientTime >= accelerationTime) {
            if(deltaClientTime >= disconnectTime) {
                sendToClient(clientID, new byte[] { DISCONNECT });
            } else {
                int deltaMessageTime = nearestMessageByTime(clientTime) - clientTime;   //Требуемое время для достижения ближайшего сообщения
                if(deltaMessageTime > 1) {
                    try (ByteArrayOutputStream writer = new ByteArrayOutputStream(1 + (Integer.SIZE >> 3))) {
                        DataOutputStream datWriter = new DataOutputStream(writer);
                        datWriter.writeByte(ACCELERATE_CLIENT);
                        datWriter.writeInt(deltaMessageTime - 1);
                        sendToClient(clientID, writer.toByteArray());
                        times[clientID] += (deltaMessageTime - 1);
                    }
                } else {
                    sendToClient(clientID, getUpdateMessageForClient(clientID));
                    times[clientID]++;
                    if(times[clientID] > maxTime) maxTime = times[clientID];
                }
            }
        } else {
            sendToClient(clientID, getUpdateMessageForClient(clientID));
            times[clientID]++;
            if(times[clientID] > maxTime) {
                maxTime = times[clientID];
                //Наступила новая итерация сервера
                if(stream != null) {
                    state.updatingMessageReceived(stream);
                }
                state.update();
            }
        }
    }
    
    private boolean isConnectedClient(InetAddress clientIPAddress, int clientPort, int clientID) {
        if(clientID < 0 || clientID >= addrs.length) return false;
        InetSocketAddress address = addrs[clientID];
        return address.getAddress().equals(clientIPAddress) && (address.getPort() == clientPort);
    }
    private void sendErrorMessageAboutNonConnected(InetAddress clientIPAddress, int clientPort) throws IOException {
        this.sendToClient(clientIPAddress, clientPort, new byte[] { CLIENT_NOT_CONNECTED_ERROR });
    }
    
    public void serverClose() throws IOException {
        //Рассылаем всем клиентам сообщение о закрытии сервера
        //Дальнейшие запросы игнорируются: наудачу посланный пакет никто не прослушает)
        for(int i = 0; i < addrs.length; i++) {
            if(addrs[i] != null) {
                sendToClient(i, new byte[] { SERVER_CLOSE });   //Отправляем клиенту сообщение о завершении работы сервера
                addrs[i] = null;
                times[i] = 0;
            }
        }
        clientCount = 0;
        
        messages.clear();
        messageTimes.clear();
        isSended.clear();
//        priorityMessages.clear();
//        sourceClientAddressIndexes.clear();
//        finalClientIds.clear();
//        for(int i = 0; i < addresses.length; i++) {
//            addresses[i] = null;
//        }
        
        serverSocket.close();
        serverSocket = null;    //Сервер не работает с этого момента
    }
    
//    protected abstract byte[] getErrorMessageIfInvalidParams(DataInputStream reader);
//    protected abstract byte[] getLocalStateForConnectedClient();
    protected boolean isCanExecuteServerCommand(byte[] params) {
        return params.equals(serverParams);
    }
//    
//    protected abstract boolean isClientBanned(InetAddress ip);
//    protected abstract void addBan(InetAddress ip);
//    protected abstract void removeBan(InetAddress ip);
    
    public static void runServer(RealTimeServer server) {
        while(server.isWork()) {
            server.update();
        }
    }
    
    public static void runServerInThread(RealTimeServer server) {
        //Поток
        Thread t = new Thread(new Runnable() {
            public void run() {
                runServer(server);
            }
        });
        t.start();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Неверное количество параметров! Нужно выбрать порт");
        }
        int port = Integer.getInteger(args[0]);
        RealTimeServer server = null;
        try {
            server = new RealTimeServer(port, null);   //Параметры через аргументы
            runServer(server);
        } catch(SocketException e) {
            
        }
    }
    
    private byte[] getMessage(DataInputStream in) throws IOException {
        byte[] message = new byte[in.available()];
        in.readFully(message);
        return message;
    }
    
    public void connect(InetAddress clientIPAddress, int clientPort) throws IOException {
        if(isCanConnect) {
            if(clientCount < MAX_CLIENT_COUNT) {
                //Выполнение локального запроса (отправка данных, находящихся на сервере)
                int connectedClientID = addClient(new InetSocketAddress(clientIPAddress, clientPort), maxTime); //Индекс подключаемого клиента
                byte[] bytes = new byte[5];
                bytes[0] = CONNECT;
                PartWriter.writeInt(connectedClientID, bytes, 1);
                clientWriters[connectedClientID].writeMessage(bytes);
            } else {
                sendToClient(clientIPAddress, clientPort, new byte[] { MAX_CLIENTS_ERROR });
            }
        } else {
            sendToClient(clientIPAddress, clientPort, new byte[] { CANNOT_CONNECT_ERROR });
        }
    }
    
    public void addBan(InetAddress ip) {
        blackList.add(ip);
    }
    public boolean isClientBanned(InetAddress ip) {
        return blackList.contains(ip);
    }

    public void processQuery(int clientID, DataInputStream in, InetAddress clientIPAddress, int clientPort) {
        try {
            byte messageType = (byte)in.readByte();
            byte[] message = getMessage(in);
            if(messageType == UPDATE) {
                if(isConnectedClient(clientIPAddress, clientPort, clientID)) {
                    updateClient(clientID, null);
//                    if(trySendQuery(clientID) == false) {
//                        //Если срочного запроса нет, обновляемся
//                        updateClient(clientID);
//                    }
                } else {
                    sendErrorMessageAboutNonConnected(clientIPAddress, clientPort);
                }
                return;
            }
            if(messageType == UPDATE_MESSAGE) {
                if(isConnectedClient(clientIPAddress, clientPort, clientID)) {
                    addUpdateMessageFromClient(message, clientID);   //Сообщение добавляем всегда
                    updateClient(clientID, in);
//                    if(trySendQuery(clientID) == false) {
//                        //Если срочного запроса нет, обновляемся
//                        updateClient(clientID);
//                    }
                } else {
                    sendErrorMessageAboutNonConnected(clientIPAddress, clientPort);
                }
                return;
            }
            if(messageType == QUERY) {
                byte[] query = state.getUnswerToQuery(clientIPAddress, clientPort, in);
                byte[] mess = new byte[query.length + 1];
                mess[0] = QUERY;
                System.arraycopy(query, 0, mess, 1, query.length);
                sendToClient(clientIPAddress, clientPort, mess);
                return;
            }
            if(messageType == NEXT_PART) {
                clientWriters[clientID].writeShortPart();
                return;
            }
            if(messageType == FINAL_CONNECT) {
                if(isClientBanned(clientIPAddress)) {
                    sendToClient(clientIPAddress, clientPort, new byte[] { BAN_ERROR });
                    return;
                }
                
                byte[] params = new byte[in.available()];
                in.readFully(params);
                if(state.isParamsCorrect(params)) {
                    byte[] mess = null;
                    if(state != null) {
                        mess = state.getStateForConnectedClient(clientIPAddress, clientPort, clientID);
                    }
                    if(mess != null) {
                        byte[] resultMessage = new byte[mess.length + 5];
                        resultMessage[0] = FINAL_CONNECT;
                        PartWriter.writeInt(maxTime, resultMessage, 1);
                        System.arraycopy(mess, 0, resultMessage, 5, mess.length);
                        sendToClient(clientIPAddress, clientPort, resultMessage);    //Отправляем локальное состояние клиенту
                    } else {
                        byte[] resultMessage = new byte[5];
                        resultMessage[0] = FINAL_CONNECT;
                        PartWriter.writeInt(maxTime, resultMessage, 1);
                        sendToClient(clientIPAddress, clientPort, resultMessage);
                    }
                }
                return;
            }
            if(messageType == DISCONNECT) {
                if(isConnectedClient(clientIPAddress, clientPort, clientID)) {
                    removeClient(clientID);
                } else {
                    sendErrorMessageAboutNonConnected(clientIPAddress, clientPort);
                }
                return;
            }
            if(messageType == SERVER_CLOSE) {
                byte[] params = new byte[message.length - 1];
                in.read(params);
                if(isCanExecuteServerCommand(params)) {
                    serverClose();
                } else {
                    sendToClient(clientIPAddress, clientPort, new byte[] { ADMIN_AUTHENTIFICATION_ERROR });
                }
                return;
            }
            if(messageType == AGAIN) {
                int iter = in.readInt();   //Итерация, на которой находится клиент
                if(times[clientID] == iter) {
                    clientWriters[clientID].writeShortPartAgain();
                } else {
                    byte[] amessage = new byte[] { AGAIN };
                    sendToClient(clientID, amessage);
                }
                //Если последнее полученное сообщение было более ранней итерации, то никакого сообщения от клиента не доходило вовсе.
                //  В таком случае запрашиваем у клиента снова отправить текущую часть данных AGAIN
                //Иначе сообщение дошло, но утерялось сообщение от сервера
                //  В таком случае ещё раз отправляем последнюю часть клиенту (из буфера)
                //sendToClient(clientID, getUpdateMessageForClient(clientID));   //Здесь аккуратнее. Ибо это цельная часть
                return;
            }
            if(messageType > 21 || messageType < 0) {
                addBan(clientIPAddress);      //Добавляем клиента в черный список за бредовое сообщение (Взлом)
                sendToClient(clientIPAddress, clientPort, new byte[] { BAN_ERROR });   //Отправляем клиенту сообщение об ошибке
                return;
            }
        } catch (IOException ex) {
            Logger.getLogger(RealTimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
