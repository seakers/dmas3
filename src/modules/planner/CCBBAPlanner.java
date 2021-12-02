package modules.planner;

import jmetal.encodings.variable.Int;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.DMASMessage;
import modules.messages.PlannerMessage;
import modules.messages.RelayMessage;
import modules.orbitData.CLAccess;
import modules.orbitData.GndAccess;
import modules.planner.CCBBA.CommLoop;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.*;

import static modules.utils.Statistics.permutations;

public class CCBBAPlanner extends AbstractPlanner{
    /**
     * Toggle for using synchronous of asynchronous communication loops
     * True     = synchronous loops will be used
     * False    = asynchronous loops will be used
     */
    private boolean syncCommLoops;

    /**
     * Stores the communication loops for a given target satellites and a given number of satellites in the relay chain
     */
    private LinkedList<CommLoop> commsLoops;

    /**
     * Stores times when rescheduling will happen either by access to a GS or at the end of a planning horizon
     */
    private LinkedList<AbsoluteDate> rescheduleTimes;

    /**
     * Initializes CCBBA planner
     * @param planningHorizon   : planning horizon in seconds
     * @param requestThreshold  : number of new measurement requests that will trigger a new planning cycle
     * @param crossLinks        : allows for cross-links between sensing satellites
     * @param syncCommLoops     : toggle for using synchronous or asynchronous communication loops
     */
    public CCBBAPlanner(double planningHorizon, int requestThreshold, boolean crossLinks, boolean syncCommLoops) {
        super(planningHorizon, requestThreshold, crossLinks);
        this.syncCommLoops = syncCommLoops;

        this.knownRequests = new ArrayList<>();
        this.activeRequests = new ArrayList<>();
    }

    /**
     * Initializes plan
     * @return
     */
    @Override
    public LinkedList<SimulationAction> initPlan() {
        try {
            // calculate all communication loop time windows for all satellites chronologically
            commsLoops = orderCommsLoops();

            // calculate all predetermined times for rescheduling
            rescheduleTimes = calculateReschedulingTimes();

//            this.plan = nominalPlanner();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<DMASMessage>> messageMap, SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {

        // for all received relay requests, schedule message to the next target at the next available access
        ArrayList<RelayMessage> relayReqs = readRelayMessages(messageMap);

        for(Message message : relayReqs){
            RelayMessage relayMessage = (RelayMessage) message;

            DMASMessage messageToSend = relayMessage.getMessageToRelay();
            AgentAddress targetAddress = relayMessage.getNextTarget();

            if(targetAddress == agent.getMyAddress()) throw new Exception("Relay Error. Check intended receiver");

            ArrayList<AbsoluteDate> nextAccess = agent.getNextAccess(targetAddress);
            AbsoluteDate startDate = nextAccess.get(0);
            AbsoluteDate endDate = nextAccess.get(1);

            MessageAction action = new MessageAction(agent,targetAddress, messageToSend, startDate, endDate);
            addToPlan(action);
        }

        // read incoming measurement requests and messages from other planners
        ArrayList<MeasurementRequest> receivedReqs = readRequestMessages(messageMap);
        ArrayList<PlannerMessage> plannerMessages = readPlannerMessages(messageMap);

        // -check for new measurement requests
        for(MeasurementRequest newReq : receivedReqs){
            if(!knownRequests.contains(newReq)){
                knownRequests.add(newReq);
            }
        }

        // -read incoming planner messages and update
        for(PlannerMessage plannerMessage : plannerMessages){

        }

        // check if rescheduling is needed
        boolean thresholdMet = this.requestThreshold <= knownRequests.size();
        boolean inCommsLoop = commsLoops.getFirst().getStartDate().compareTo(currentDate) <= 0 &&
                                commsLoops.getFirst().getEndDate().compareTo(currentDate) >= 0;
        boolean inRescheduleTime = rescheduleTimes.getFirst().compareTo(currentDate) <= 0 &&
                                rescheduleTimes.getFirst().compareTo(
                                        currentDate.shiftedBy( parentAgent.getTimeStep())
                                        ) <= 0;
        // reschedule measurement requests
        if( (thresholdMet && inCommsLoop) || inRescheduleTime){
            // create schedule

        }



        // -return next available actions in plan
        return null;
    }

    private void addToPlan(SimulationAction newAction){
        int i = 0;
        for(SimulationAction action : plan){
            if(newAction.getStartDate().compareTo(action.getStartDate()) > 0){
                break;
            }
            i++;
        }
        plan.add(i,newAction);
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        if(request == null) return 3;
        return 10;
    }

    /**
     * Calculates the times when the satellite is forced to reconsider its bids. Does this only when the satellite accesses a ground station or reaches the end of a planning horizon interval
     * @return array containing all rescheduling times in chronological order
     */
    private LinkedList<AbsoluteDate> calculateReschedulingTimes(){
        // -based on planning horizon
        ArrayList<AbsoluteDate> rescheduleTimes = new ArrayList<>();
        AbsoluteDate startDate = parentAgent.getStartDate();
        AbsoluteDate endDate = parentAgent.getEndDate();

        double t = 0.0;
        while(endDate.compareTo(startDate.shiftedBy(t)) > 0){
            AbsoluteDate planDate = startDate.shiftedBy(t);
            rescheduleTimes.add(planDate);

            t += planningHorizon;
        }

        // -based on ground station access
        ArrayList<GndAccess> orderedGndAccess = parentAgent.getOrderedGndAccesses();
        for(GndAccess access : orderedGndAccess){
            AbsoluteDate planDate = access.getStartDate();

            int i = 0;
            for(AbsoluteDate plannedDate : rescheduleTimes){
                if(planDate.compareTo(plannedDate) < 0){
                    break;
                }
                i++;
            }
            rescheduleTimes.add(i,planDate);
        }

        return new LinkedList<>(rescheduleTimes);
    }

    /**
     * Pre calculates all possible communication loops with all the satellites in the constellation
     * @param crossLinks : toggle that allows for sensing satellites to talk to each other
     * @param syncCommLoops : toggle that allows for synchronous or asynchronous comm looks (See
     *                      "Online scheduling of distributed Earth observation satellite system
     *                      under rigid communication constraints" by Guoliang Li)
     * @return
     */
    private  HashMap<Satellite, HashMap<Integer, ArrayList<CommLoop>>> generateCommLoops(boolean crossLinks, boolean syncCommLoops) throws Exception {
        HashMap<Satellite, HashMap<Integer, ArrayList<CommLoop>>> loops = new HashMap<>();

        for(Satellite targetSat : parentAgent.getSatAddresses().keySet()){
            if(targetSat.equals(parentAgent.getSat())
                    && targetSat.getName().equals(parentAgent.getSat().getName())) {
                continue;
            }
            if(parentAgent.isCommsSat(targetSat)) {
                continue;
            }

            loops.put(targetSat, new HashMap<>());
            for(int pathLength = 2; pathLength <= parentAgent.getSatAddresses().keySet().size(); pathLength++){
                if(!crossLinks && pathLength != 3){
                    continue;
                }

                ArrayList<CommLoop> availableLoops = null;
                ArrayList<ArrayList<Satellite>> paths = generatePaths(targetSat, pathLength, crossLinks);

                for(ArrayList<Satellite> path : paths){
                    availableLoops = generateLoopsForPath(path, syncCommLoops);
                }

                if(availableLoops != null && availableLoops.size() > 0) {
                    loops.get(targetSat).put(pathLength, availableLoops);
                }
            }
        }

        return loops;
    }

    /**
     * Given a path to be analysed, generates an array of communication loops that are available for that path
     * @param path : desired path to be estimated
     * @param syncCommLoops : if true, requires synchronized comm loops, else it allows for asynchronous comm intervals
     * @return List of all possible comm loops for a given paths at different times in the simulation
     * @throws Exception throws an exception if Cross Link Accesses function fails
     */
    private ArrayList<CommLoop> generateLoopsForPath(ArrayList<Satellite> path, boolean syncCommLoops) throws Exception {
        ArrayList<CommLoop> loops = new ArrayList<>();
        // for every edge in the graph, find an access interval
        ArrayList<ArrayList<CLAccess>> accesses = new ArrayList<>();
        for(int i = 0; i < path.size()-1; i++){
            Satellite sender = path.get(i);
            Satellite receiver = path.get(i+1);
            accesses.add(parentAgent.orderCLAccesses(sender,receiver));
        }

        if(accesses.isEmpty()) return loops;

        for(CLAccess firstAccess : accesses.get(0)){
            ArrayList<ArrayList<AbsoluteDate>> accessDates = new ArrayList<>();

            ArrayList<AbsoluteDate> accessInterval = new ArrayList<>();
            accessInterval.add(firstAccess.getStartDate()); accessInterval.add(firstAccess.getEndDate());

            accessDates.add(accessInterval);
            for(ArrayList<CLAccess> edgeAccess : accesses){
                if(edgeAccess == accesses.get(0)) continue;

                for(CLAccess iAccess : edgeAccess){
                    ArrayList<AbsoluteDate> iInterval = new ArrayList<>();
                    iInterval.add(iAccess.getStartDate()); iInterval.add(iAccess.getEndDate());

                    if(syncCommLoops){
                        // only allows intervals that overlap
                        if(iAccess.getStartDate().compareTo(accessInterval.get(1)) <= 0
                        && iAccess.getEndDate().compareTo(accessInterval.get(0)) >= 0){

                            // updates overall access interval dates
                            if(iAccess.getStartDate().compareTo(accessInterval.get(0)) <= 0){
                                if(iAccess.getEndDate().compareTo(accessInterval.get(1)) <= 0){
                                    accessInterval.set(1,iAccess.getEndDate());
                                }
                            }
                            else if(iAccess.getEndDate().compareTo(accessInterval.get(1)) <= 0){
                                accessInterval.set(0,iAccess.getStartDate());
                                accessInterval.set(1,iAccess.getEndDate());
                            }
                            else {
                                accessInterval.set(0,iAccess.getStartDate());
                            }

                            // adds access interval to access dates
                            accessDates.add(iInterval);
                            break;
                        }
                    }
                    else{
                        if(iAccess.getStartDate().compareTo(accessInterval.get(0)) <= 0){
                            // adds access interval to access dates
                            accessDates.add(iInterval);
                            break;
                        }
                    }
                }
            }

            if(accessDates.size() == path.size()-1){
                Satellite sender = path.get(0);
                Satellite receiver = path.get(1);
                AbsoluteDate startDate = accessInterval.get(0);
                AbsoluteDate endDate = accessInterval.get(1);

                loops.add(new CommLoop(sender, receiver, path, startDate, endDate, accessDates));
            }
        }

        return loops;
    }

    /**
     * Returns all possible paths of size n to a desired target satellite
     * @param target : target satellite
     * @param n : number of sats in path including start and end points
     * @return
     */
    private ArrayList<ArrayList<Satellite>> generatePaths(Satellite target, int n, boolean crossLinks){
        int numPaths = countPaths(n);
        ArrayList<ArrayList<Satellite>> paths = new ArrayList<>(numPaths);
        ArrayList<ArrayList<Integer>> pathsInt = new ArrayList<>(numPaths);
        ArrayList<Satellite> satList = new ArrayList<>(parentAgent.getSatAddresses().keySet());
        ArrayList<String> satNames = new ArrayList<>();

        int[] indeces = new int[satList.size()];
        for(int i = 0; i < satList.size(); i++) {
            indeces[i] = i;
            satNames.add(satList.get(i).getName());
        }

        ArrayList<int[]> permutIndeces = new ArrayList<>();
        getPermutations(satList.size(), indeces, permutIndeces);

        int i_sender = satNames.indexOf(parentAgent.getSat().getName());
        int i_receiver = satNames.indexOf(target.getName());

        for(int i = 0; i < permutIndeces.size(); i++){
            int i_first = permutIndeces.get(i)[0];
            int i_end = permutIndeces.get(i)[n-1];

            if(i_first == i_sender
                    && i_end == i_receiver){

                ArrayList<Satellite> path = new ArrayList<>();
                ArrayList<Integer> pathInt = new ArrayList<>();
                for(int j = 0; j <= n-1; j++){
                    path.add(satList.get(permutIndeces.get(i)[j]));
                    pathInt.add(permutIndeces.get(i)[j]);
                }

                // check for duplicates
                boolean duplicate = false;
                for(int j = 0; j < paths.size(); j++){
                    int dupCounter = 0;

                    for(int k = 0; k < n; k++){
                        if(pathsInt.get(j).get(k).equals(pathInt.get(k))){
                            dupCounter++;
                        }
                    }

                    if(dupCounter == n){
                        duplicate = true;
                        break;
                    }
                }



                // check if crossLinks are required in path
                boolean links = false;
                for(int j = 0; j < path.size()-1; j++){
                    Satellite sat_j = path.get(j);
                    Satellite sat_jp = path.get(j+1);

                    if(!parentAgent.isCommsSat(sat_j) && !parentAgent.isCommsSat(sat_j)){
                        links = true;
                    }
                }

                // check if crossLinks are allowed
                boolean allowed = true;
                if(!crossLinks && links) allowed = false; // if cross links exist and are not allowed, do not allow path

                if(!duplicate && allowed) {
                    paths.add(path);
                    pathsInt.add(pathInt);
                }

            }
        }

        return paths;
    }

    public static void getPermutations(int n, int[] elements, ArrayList<int[]> permutations) {

        if(n == 1) {
            int[] newElements = new int[elements.length];
            for(int i = 0; i < elements.length; i++){
                newElements[i] = elements[i];
            }
            permutations.add(newElements);
        } else {
            for(int i = 0; i < n-1; i++) {
                getPermutations(n - 1, elements, permutations);
                if(n % 2 == 0) {
                    swap(elements, i, n-1);
                } else {
                    swap(elements, 0, n-1);
                }
            }
            getPermutations(n - 1, elements, permutations);
        }
    }

    private static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }

    private LinkedList<CommLoop> orderCommsLoops() throws Exception {
        LinkedList<CommLoop> ordered = new LinkedList();
        HashMap<Satellite, HashMap<Integer, ArrayList<CommLoop>>> commIntervals
                = generateCommLoops(crossLinks, syncCommLoops);

        while(!commIntervals.isEmpty()) {
            Satellite minSat = null;
            int minPathLength = -1;
            CommLoop minLoop = null;

            for (Satellite sat : commIntervals.keySet()) {
                for(Integer pathLength : commIntervals.get(sat).keySet()){
                    for(CommLoop loop : commIntervals.get(sat).get(pathLength)){
                        if(minLoop == null || loop.getStartDate().compareTo(minLoop.getStartDate()) < 0){
                            minSat = sat;
                            minPathLength = pathLength;
                            minLoop = loop;
                            continue;
                        }
                    }
                }
            }

            ordered.add(minLoop);

            commIntervals.get(minSat).get(minPathLength).remove(minLoop);
            if(commIntervals.get(minSat).get(minPathLength).isEmpty())
                commIntervals.get(minSat).remove(minPathLength);
            if(commIntervals.get(minSat).isEmpty())
                commIntervals.remove(minSat);
        }
        return ordered;
    }

    /**
     * Counts the total amount of acceptable paths of a given size n. Must start from the parent satellite and end in the target satellite
     * @param n : size of paths
     * @return
     */
    private int countPaths(int n){
        if(n == 2) return 1;

        int n_sats = parentAgent.getSatAddresses().keySet().size()-2;
        int k_slots = n-2;

        return permutations(n_sats,k_slots);
    }
}
