package modules.agents.orbits;

public class OrbitParams {
    /**
     * Stores Orbital Parameters for a given orbit
     */
    private final String name;              // Orbit name
    private final double SMA;               // Semi-major axis [km]
    private final double ECC;               // Eccentricity [km]
    private final double INC;               // Inclination [deg]
    private final double RAAN;              // Longitude of the Right Ascending Node
    private final double APRG;              // Argument of Periapsis [deg]
    private double ANOM;                    // True Anomaly
    private final double Re = 6378.137;     // Radius of the Earth [km]
    private final double mu = 0.39860e6;    // Gravitational parameter of Earth [km^3/s^2]

    public OrbitParams(String name, double alt, double ECC, double INC, double RAAN, double APRG, double ANOM) {
        this.name = name;
        this.SMA = alt + Re;
        this.ECC = ECC;
        this.INC = INC;
        this.RAAN = RAAN;
        this.APRG = APRG;
        this.ANOM = ANOM;
    }

    public OrbitParams(String name, double alt, String inc, String time) throws Exception {
        this.name = name;
        this.SMA = alt + Re;
        this.ECC = 0.0;
        switch(inc) {
            case "SSO":
                this.INC = calcSSOinc(alt);
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

    private double rad2deg(double th){
        return th * 180/Math.PI;
    }
    private double deg2rad(double th){
        return th * Math.PI/180;
    }
}
