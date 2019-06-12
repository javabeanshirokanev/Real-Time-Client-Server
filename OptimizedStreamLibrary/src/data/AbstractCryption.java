/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Широканев Александр
 */
public abstract class AbstractCryption {
    public abstract void crypting(byte[] bytes, int count);
    public abstract void uncrypting(byte[] bytes, int count);

    public final boolean cryptionTest(byte[] testMessage) {
        byte[] savedMessage = testMessage.clone();
        crypting(savedMessage, savedMessage.length);
        uncrypting(savedMessage, savedMessage.length);
        return Arrays.equals(testMessage, savedMessage);
    }

    public final boolean cryptionTest() {
        byte[] message = new byte[256];
        Random rnd = new Random();
        for(int i = 0; i < 12; i++) {
            rnd.nextBytes(message);
            boolean isTest = cryptionTest(message);
            if(isTest == false) return false;
        }
        return true;
    }
}
