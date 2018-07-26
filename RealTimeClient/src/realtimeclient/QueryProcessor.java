/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeclient;

/**
 *
 * @author Широканев Александр
 */
public interface QueryProcessor {
    byte[] queryConnectionUnswerSending();
    byte[] queryUnswerSending(int queryType);
}
