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
public abstract class AbstractCryption {
    public abstract void crypting(byte[] bytes, int count);
    public abstract void uncrypting(byte[] bytes, int count);
}
