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
public class TransmitterAntenna extends AbstractTransmitter{
    
    public TransmitterAntenna(double gainR, Set<CommunicationBand> bands) {
        super(gainR, bands);
    }
    
}
