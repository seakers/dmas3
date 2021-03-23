package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.orbitData.CLAccess;
import modules.planner.CCBBA.CommsLoop;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.*;

import static modules.utils.Statistics.permutations;

public class CCBBAPlanner extends AbstractPlanner{
    private boolean syncCommLoops;

    /**
     * Stores the communication loops for a given target satellites and a given number of satellites in the relay chain
     */
    private HashMap<Satellite, HashMap<Integer, ArrayList<CommsLoop>>> commsLoops;

    public CCBBAPlanner(double planningHorizon, int requestThreshold, boolean crossLinks, boolean syncCommLoops) {
        super(planningHorizon, requestThreshold, crossLinks);
    }

    @Override
    public LinkedList<SimulationAction> initPlan() {
        try {
            this.commsLoops = generateCommsLoops(crossLinks, syncCommLoops);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap, SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        return null;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        return 0;
    }

    /**
     * Precalculates all possible communication loops with all the satellites in the constellation
     * @param crossLinks : toggle that allows for sensing satellites to talk to each other
     * @param syncCommLoops : toggle that allows for synchronous or asynchronous comm looks (See
     *                      "Online scheduling of distributed Earth observation satellite system
     *                      under rigid communication constraints" by Guoliang Li)
     * @return
     */
    private  HashMap<Satellite, HashMap<Integer, ArrayList<CommsLoop>>> generateCommsLoops(boolean crossLinks, boolean syncCommLoops) throws Exception {
        HashMap<Satellite, HashMap<Integer, ArrayList<CommsLoop>>> loops = new HashMap<>();

        for(Satellite targetSat : parentAgent.getSatAddresses().keySet()){
            if(targetSat.equals(parentAgent.getSat())) continue;

            loops.put(targetSat, new HashMap<>());
            for(int i = 2; i <= parentAgent.getSatAddresses().keySet().size(); i++){
                if(!crossLinks && i != 3) continue;

                ArrayList<CommsLoop> availableLoops = null;
                ArrayList<ArrayList<Satellite>> paths = generatePaths(targetSat, i, crossLinks);

                for(ArrayList<Satellite> path : paths){
                    availableLoops = generateLoopsForPath(path, syncCommLoops);
                }

                if(availableLoops != null && availableLoops.size() > 0) {
                    loops.get(targetSat).put(i, availableLoops);
                }
            }
        }

        return loops;
    }

    private ArrayList<CommsLoop> generateLoopsForPath(ArrayList<Satellite> path, boolean syncCommLoops) throws Exception {
        ArrayList<CommsLoop> loops = new ArrayList<>();
        // for every edge in the graph, find an access interval
        ArrayList<ArrayList<CLAccess>> accesses = new ArrayList<>();
        for(int i = 0; i < path.size()-2; i++){
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

                loops.add(new CommsLoop(sender, receiver, path, startDate, endDate, accessDates));
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

        int[] indeces = new int[satList.size()];
        for(int i = 0; i < satList.size(); i++) {
            indeces[i] = i;
        }

        ArrayList<int[]> permutIndeces = new ArrayList<>();
        getPermutations(satList.size(), indeces, permutIndeces);

        for(int i = 0; i < permutIndeces.size(); i++){
            int i_first = permutIndeces.get(i)[0];
            int i_end = permutIndeces.get(i)[n-1];

            if(i_first == satList.indexOf(parentAgent.getSat())
                    && i_end == satList.indexOf(target)){

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
