package modules.simulation;

import org.json.simple.JSONObject;

public class SimGroups {
    /**
     * Simulation community and group
     */
    public final String MY_COMMUNITY;
    public static final String SIMU_GROUP = "simu";
    public static final String SCHEDULER = "scheduler";

    /**
     * Environment roles
     */
    public static final String ENVIRONMENT = "environment";

    /**
     * Roles a Satellite can take
     */
    public static final String SATELLITE = "sat_in_simulation";
    public static final String SAT_SENSE = "sat_sense_environment";
    public static final String SAT_THINK = "sat_make_plan";
    public static final String SAT_DO = "sat_do_plan";
    public static final String SAT_DIE = "sat_die";

    /**
     * Roles a GroundStation can take
     */
    public static final String GNDSTAT = "gndStat_in_simulation";
    public static final String GS_SENSE = "gndStat_sense_environment";
    public static final String GS_THINK = "gndStat_make_plan";
    public static final String GS_DO = "gndStat_do_plan";
    public static final String GS_DIE = "gndStat_die";

    public SimGroups(JSONObject input, int i){
        MY_COMMUNITY = "simu_" + i;
    }
}
