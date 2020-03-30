/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.communications;

import java.util.Set;
import seakers.orekit.object.CommunicationBand;

/**
 *
 * @author nhitomi
 */
public interface Antenna {

    /**
     * Gets the operating communication bands of this antenna
     *
     * @return the operating communication bands of this antenna
     */
    public Set<CommunicationBand> getBands();
}
