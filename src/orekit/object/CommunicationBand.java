/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

/**
 *
 * @author nhitomi
 */
public enum CommunicationBand {

    /**
     * Very high frequency -band
     */
    VHF(0.03, 0.3),
    
    /**
     * Ultra high frequency -band
     */
    UHF(0.3, 3.),
    /**
     * S-band
     */
    S(2., 4.),
    /**
     * X-band
     */
    X(7., 11.2),
    /**
     * Ku-band
     */
    KU(12., 18.),
    /**
     * K-band
     */
    K(18., 26.5),
    /**
     * Ka-band
     */
    KA(26.5, 40.);

    /**
     * Lower bound of band range in GHz
     */
    private final double lowerBound;

    /**
     * Upper bound of band range in GHz
     */
    private final double upperBound;

    /**
     * The range of frequencies for that band [GHz]
     *
     * @param lowerBound the lower bound of band range in GHz
     * @param upperBound the upper bound of band range in GHz
     */
    private CommunicationBand(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Gets the lower bound of band range in GHz
     *
     * @return the lower bound of band range in GHz
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Gets the upper bound of band range in GHz
     *
     * @return the upper bound of band range in GHz
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Gets the communication band object if the name of the band is given
     *
     * @param bandName the name of the communication band
     * @return the corresponding communication band object
     */
    public static CommunicationBand get(String bandName) {
        switch (bandName) {
            case "VHF":
                return VHF;
            case "UHF":
                return UHF;
            case "S":
                return S;
            case "X":
                return X;
            case "Ku":
                return KU;
            case "K":
                return K;
            case "Ka":
                return KA;
            default:
                throw new UnsupportedOperationException(
                        String.format("Band %s not recognized.", bandName));
        }
    }
}
