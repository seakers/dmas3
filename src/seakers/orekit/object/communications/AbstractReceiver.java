/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.communications;

import java.util.Objects;
import java.util.Set;
import seakers.orekit.object.CommunicationBand;

/**
 * An abstract receiver to communicate with transmitters
 *
 * @author nhitomi
 */
public class AbstractReceiver implements Receiver {

    /**
     * the gain for receiving incoming signals
     */
    private final double gainR;

    /**
     * the bands supported by this receiver
     */
    private final Set<CommunicationBand> bands;

    /**
     * Constructs a new receiver
     *
     * @param gainR the gain for receiving incoming signals
     * @param bands the bands supported by this receiver
     */
    public AbstractReceiver(double gainR, Set<CommunicationBand> bands) {
        this.gainR = gainR;
        this.bands = bands;
    }

    @Override
    public double getGainR() {

        return gainR;
    }

    @Override
    public boolean compatible(Transmitter transmitter) {
        for (CommunicationBand band : getBands()) {
            if (transmitter.getBands().contains(band)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<CommunicationBand> getBands() {
        return bands;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (int) (Double.doubleToLongBits(this.gainR) ^ (Double.doubleToLongBits(this.gainR) >>> 32));
        hash = 23 * hash + Objects.hashCode(this.bands);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractReceiver other = (AbstractReceiver) obj;
        if (Double.doubleToLongBits(this.gainR) != Double.doubleToLongBits(other.gainR)) {
            return false;
        }
        if (!Objects.equals(this.bands, other.bands)) {
            return false;
        }
        return true;
    }

    
    
}
