/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

/**
 *
 * @author Широканев Александр
 */
public interface StaticSenderReceiver {
    /**
     * Отправка мелкого пакета
     * @param buf Буфер
     * @param count Количество отправляемых байтов
     */
    void send(byte[] buf, int count);
    /**
     * Приём мелкого пакета
     * @param buf Буфер
     * @return 
     */
    int recv(byte[] buf);
}
