package modules.agents;

import madkit.kernel.Agent;
import modules.actions.AnnouncementAction;
import modules.actions.SimulationActions;
import modules.environment.Environment;
import modules.measurement.MeasurementRequest;
import modules.orbitData.GndAccess;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.util.*;

public class GndStatAgent extends Agent {
    /**
     * Ground station assigned to this agent
     */
    private GndStation gnd;

    /**
     * Coverage data
     */
    private OrbitData orbitData;

    /**
     * Names of groups and roles within simulation community
     */
    private SimGroups myGroups;

    /**
     * Stores the accesses of each satellite with this ground station
     */
    private HashMap<Satellite, TimeIntervalArray> satAccesses;

    /**
     * current plan to be performed
     */
    private LinkedList<SimulationActions> plan;

    /**
     * Environment in which this agent exists in.
     */
    private Environment environment;


    public GndStatAgent(GndStation gnd, OrbitData orbitData, SimGroups myGroups){
        this.gnd = gnd;
        this.orbitData = orbitData;
        this.myGroups = myGroups;

        this.satAccesses = new HashMap<>();
        for(Satellite sat : orbitData.getAccessesGS().keySet()){
            TimeIntervalArray arr = orbitData.getAccessesGS().get(sat).get(gnd);
            this.satAccesses.put(sat, arr);
        }
    }

    @Override
    protected void activate(){
        requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT);
        this.plan = this.initPlan();
    }

    /**
     * Reads messages from other satellites. Registers when these measurements where done and when they were received
     */
    public void sense() {

    }

    /**
     * Does nothing. Initial plan does not change.
     */
    public void think(){

    }

    /**
     * Sends measurement request announcements according to initial plan.
     */
    public void execute(){
        do{
            // if the scheduled time is reached for next actions in plan, perform actions
            if(plan.getFirst().getStartDate()
                    .compareTo(environment.getCurrentDate()) >= 0){

                // get all available tasks that can be announced
                LinkedList<MeasurementRequest> availableRequests = environment.getAvailableRequests();

                // make a message containing available requests


                // send it to the target agent


                int x = 1;
            }
            else break;
        }while(!plan.isEmpty());
    }

    /**
     * Initiates final plan for ground station, which consists of sending messages to accessing satellites
     * announcing urgent measurement requests.
     * @return List of actions to be performed during execute phase
     */
    private LinkedList<SimulationActions> initPlan(){
        LinkedList<SimulationActions> plan = new LinkedList<>();
        ArrayList<GndAccess> orderedAccesses = this.orderSatAccesses();

        for(GndAccess acc : orderedAccesses){
            AnnouncementAction req = new AnnouncementAction(this, acc.getSat(), acc.getStartDate(),
                    acc.getStartDate().shiftedBy( 5.0 * environment.getDt() ));
            plan.add(req);
        }

        return plan;
    }

    private ArrayList<GndAccess> orderSatAccesses(){
        ArrayList<GndAccess> ordered = new ArrayList();
        HashMap<Satellite, ArrayList<GndAccess>> unordered = new HashMap<>();

        for(Satellite sat : satAccesses.keySet()){
            TimeIntervalArray arr = satAccesses.get(sat);
            double t_0 = 0.0;
            double t_f = 0.0;

            ArrayList<GndAccess> accesses = new ArrayList<>();
            for(int i = 0 ; i < arr.getRiseSetTimes().size(); i++){
                RiseSetTime setTime = arr.getRiseSetTimes().get(i);

                if(setTime.isRise()) {
                    t_0 = setTime.getTime();
                }
                else {
                    t_f = setTime.getTime();

                    AbsoluteDate startDate = orbitData.getStartDate().shiftedBy(t_0);
                    AbsoluteDate endDate = orbitData.getStartDate().shiftedBy(t_f);

                    GndAccess access = new GndAccess(sat, this.gnd, startDate, endDate);
                    accesses.add(access);
                }
            }

            unordered.put(sat, accesses);
        }


        for(Satellite sat : unordered.keySet()) {
            for(GndAccess acc : unordered.get(sat)){
                if(ordered.size() == 0){
                    ordered.add(acc);
                    continue;
                }

                int i = 0;
                for(GndAccess accOrd : ordered){
                    if(acc.getStartDate().compareTo(accOrd.getStartDate()) <= 0) break;
                    i++;
                }
                ordered.add(i,acc);
            }
        }

        return ordered;
    }


    @Override
    protected void live(){ }
}
