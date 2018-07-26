/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realtimeclient;

import java.io.IOException;

/**
 *
 * @author Широканев Александр
 */
public interface FailedListener {
    void connectionFailed(int failIndex);
    void clientQueryUnswerError(int queryError) throws IOException;
    void adminAuthentificationError();
}
