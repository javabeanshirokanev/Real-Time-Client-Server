/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.net.SocketException;
import realtimeserver.RealTimeServer;
import static realtimeserver.RealTimeServer.runServer;

/**
 *
 * @author USER
 */
public class TestServer {
    
    public static void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Неверное количество параметров! Нужно выбрать порт");
        }
        int port = Integer.parseInt(args[0]);
        RealTimeServer server = null;
        try {
            server = new RealTimeServer(port, new TestServerState());   //Параметры через аргументы
            System.out.println("server started");
            runServer(server);
        } catch(SocketException e) {
            
        }
    }
}
