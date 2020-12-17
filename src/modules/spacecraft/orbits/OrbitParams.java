package modules.spacecraft.orbits;

import org.orekit.utils.Constants;

public class OrbitParams {
    /**
     * Stores Orbital Parameters for a given orbit
     */
    private final String name;              // Orbit name
    private final double SMA;               // Semi-major axis [m]
    private final double ECC;               // Eccentricity [-]
    private final double INC;               // Inclination [deg]
    private final double RAAN;              // Longitude of the Right Ascending Node [deg]
    private final double APRG;              // Argument of Periapsis [deg]
    private double ANOM;                    // True Anomaly [deg]
    private final double Re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
                                            // Radius of the Earth [m]
    private final double mu = 0.39860e6;    // Gravitational parameter of Earth [km^3/s^2]


    public OrbitParams(String name, double alt, double ECC, double INC, double RAAN, double APRG, double ANOM) {
        this.name = name;
        this.SMA = alt*1e3 + Re;
        this.ECC = ECC;
        this.INC = INC;
        this.RAAN = RAAN;
        this.APRG = APRG;
        this.ANOM = ANOM;
    }

    public OrbitParams(String name, double alt, String inc, String time) throws Exception {
        this.name = name;
        this.SMA = alt*1e3 + Re;
        this.ECC = 0.0;
        switch(inc) {
            case "SSO":
                this.INC = calcSSOinc(alt*1e3);
                break;
            default:
                throw new Exception("Orbit input not yet supported");
        }
        this.RAAN = 0.0;
        this.APRG = 0.0;
        this.ANOM = 0.0;
    }

    private double calcSSOinc(double alt){
        double n = Math.sqrt(this.mu/Math.pow(Re+alt,3));
        double p = (Re + alt)*(1-Math.pow(this.ECC,2));
        double i = Math.acos( 1.227e-4 * (1/n) * Math.pow(p/Re,2) );
        return rad2deg(i);
    }

    public OrbitParams copy(){
        return new OrbitParams(name, (SMA-Re)*1e-3, ECC, INC, RAAN, APRG, ANOM);
    }

    private double rad2deg(double th){
        return th * 180/Math.PI;
    }
    private double deg2rad(double th){
        return th * Math.PI/180;
    }

    public String getName() {
        return name;
    }

    public double getSMA() {
        return SMA;
    }

    public double getECC() {
        return ECC;
    }

    public double getINC() {
        return INC;
    }

    public double getRAAN() {
        return RAAN;
    }

    public double getAPRG() {
        return APRG;
    }

    public double getANOM() {
        return ANOM;
    }

    public double getRe() {
        return Re;
    }

    public double getMu() {
        return mu;
    }
}
