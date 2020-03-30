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
 * An abstract transmitter to send signals to receivers
 *
 * @author nhitomi
 */
public class AbstractTransmitter implements Transmitter {

    /**
     * the gain for transmitting signals
     */
    private final double gainR;

    /**
     * the bands supported by this transmitter
     */
    private final Set<CommunicationBand> bands;

    /**
     * Constructs a new transmitter
     *
     * @param gainR the gain for transmitting signals
     * @param bands the bands supported by this transmitter
     */
    public AbstractTransmitter(double gainR, Set<CommunicationBand> bands) {
        this.gainR = gainR;
        this.bands = bands;
    }

    @Override
    public double getGainT() {

        return gainR;
    }

    @Override
    public boolean compatible(Receiver receiver) {
        for (CommunicationBand band : getBands()) {
            if (receiver.getBands().contains(band)) {
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
        int hash = 5;
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.gainR) ^ (Double.doubleToLongBits(this.gainR) >>> 32));
        hash = 53 * hash + Objects.hashCode(this.bands);
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
        final AbstractTransmitter other = (AbstractTransmitter) obj;
        if (Double.doubleToLongBits(this.gainR) != Double.doubleToLongBits(other.gainR)) {
            return false;
        }
        if (!Objects.equals(this.bands, other.bands)) {
            return false;
        }
        return true;
    }
    
    

}
