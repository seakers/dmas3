package modules.simulation;

import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import modules.agents.CommsSatellite;
import modules.agents.GndStationAgent;
import modules.agents.SatelliteAgent;
import modules.agents.SensingSatellite;
import modules.environment.Environment;
import modules.orbitData.Attitude;
import modules.orbitData.OrbitData;
import modules.planner.*;
import org.json.simple.JSONObject;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.logging.Level;

import static constants.JSONFields.*;

/** Simulation Class
 * Class in charge of initializing and running one run of the chosen scenario and constellation.
 * It reads and loads information from various inputs and databases to construct the environment
 * and satellites that will be simulated in this particular run.
 *
 * @author a.aguilar
 */
public class Simulation extends AbstractAgent{
    /**
     * Name of the simulation
     */
    private final String name;

    /**
     * Simulation ID
     */
    private final int simID;

    /**
     * JSON input file
     */
    private final JSONObject input;

    /**
     * Coverage and access data from loaded scenario and constellation
     */
    private final OrbitData orbitData;

    /**
     * Toggle to create a gui for this simulation
     */
    private boolean createFrame;

    /**
     * Directory address of where the results of this simulation will be printed to
     */
    private final String simDirectoryAddress;

    /**
     * Logger level chosen for this simulation
     */
    private Level loggerLevel;

    /**
     * Simulation start and end dates
     */
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    /**
     * Organization groups and roles available for agents in the simulation
     */
    private SimGroups myGroups;

    /**
     * Environment to be simulated
     */
    private Environment environment;

    /**
     * List of remote sensing and communications satellites to be simulated
     */
    private ArrayList<SatelliteAgent> spaceSegment;

    /**
     * List of ground stations to be simulated
     */
    private ArrayList<GndStationAgent> gndSegment;

    /**
     * Creates an instance of a simulation object. Initializes simulation variables using
     * pre-calculated databases and inputs
     * @param input : JSON object of input file chosen for this simulation
     * @param orbitData : coverage data of all satellites and ground stations in the scenario
     * @param directoryAddress : directory address of where the results of this simulation will be printed to
     * @param simID : id of this particular simulation
     */
    public Simulation(JSONObject input, OrbitData orbitData, String directoryAddress, int simID){
        this.name = ((JSONObject) input.get(SIM)).get(SCENARIO).toString() + "-" + simID;
        this.simID = simID;
        this.input = input;
        this.orbitData = orbitData;
        this.startDate = orbitData.getStartDate();
        this.endDate = orbitData.getEndDate();
        this.simDirectoryAddress = directoryAddress + "/run_" + simID;

        if(simID == 0 && ((JSONObject) input.get(SETTINGS)).get(GUI).equals(true)) createFrame = true;

        // Create simulation directory
        createDirectory();
    }

    /**
     * Triggered when simulation is launched. Creates instances of agents chosen to be simulated
     * and starts the simulation.
     */
    @Override
    protected void activate(){
        try {
            // 0- if frame created, log welcome message
            setLogger();

            // 1- create the simulation group
            myGroups = new SimGroups(input, simID);
            createGroup(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP);

            // 2- generate agents
            environment = new Environment(input, orbitData, myGroups, simDirectoryAddress);
            spaceSegment = generateSpaceSegment();
            gndSegment = generateGroundSegment();

            // 3- launch agents
            launchAgent(environment, createFrame);
            for(SatelliteAgent satAgent : spaceSegment) launchAgent(satAgent, createFrame);
            for(GndStationAgent gndAgent : gndSegment) launchAgent(gndAgent, false);

            // 4 - give agent addresses to all agents
            HashMap<Satellite, AgentAddress> satAddresses = this.getSatAddresses();
            HashMap<GndStation, AgentAddress> gndAddresses = this.getGndAddresses();
            this.registerAddresses(satAddresses, gndAddresses);

            // 5 - initialize planners
            for(SatelliteAgent satAgent : spaceSegment) satAgent.initPlanner();

            // 5- launch simulation
            launchAgent(new SimScheduler(myGroups, startDate, endDate), true);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gives all agents the list of their peer's agent addresses in a map
     * @param satAdd : map of satellite agent addresses
     * @param gndAdd : map of ground station agent addresses
     */
    private void registerAddresses(HashMap<Satellite, AgentAddress> satAdd,
                                   HashMap<GndStation, AgentAddress> gndAdd){
        for(SatelliteAgent sat : spaceSegment){
            sat.registerAddresses(satAdd, gndAdd);
        }
        for(GndStationAgent gnd : gndSegment){
            gnd.registerAddresses(satAdd, gndAdd);
        }
    }

    /**
     * Obtains the addresses of all satellite agents and creates a map linking their respective
     * orekit satellite object to their agent address
     * @return addresses : a map of addresses as a function of orekit satellite objects
     */
    private HashMap<Satellite, AgentAddress> getSatAddresses(){
        HashMap<Satellite, AgentAddress> addresses = new HashMap<>();

        for(SatelliteAgent sat : spaceSegment){
            addresses.put(sat.getSat(),
                    sat.getAgentAddressIn(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE));
        }

        return addresses;
    }

    /**
     * Obtains the addresses of all ground station agents and creates a map linking their respective
     * orekit ground station object to their agent address
     * @return addresses : a map of addresses as a function of orekit ground station objects
     */
    private HashMap<GndStation, AgentAddress> getGndAddresses(){
        HashMap<GndStation, AgentAddress> addresses = new HashMap<>();
        for(GndStationAgent gnd : gndSegment){
            addresses.put(gnd.getGnd(),
                    gnd.getAgentAddressIn(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT));
        }

        return addresses;
    }

    /**
     * Creates a folder directory where the outputs of the simulation will be printed to
     */
    private void createDirectory(){
        getLogger().info("Creating simulation results directory...");

        if(!new File( simDirectoryAddress ).exists()) {
            new File(simDirectoryAddress).mkdir();
            getLogger().config("Simulation results directory created at\n" + simDirectoryAddress);
        }
        else{
            getLogger().config("Simulation results directory already exists at\n" + simDirectoryAddress);
        }
    }

    /**
     * Reads input files and databases to create an array containing all satellite agents present
     * in this simulation
     * @return spaceSegment : array containing all satellite agents in the simulation
     */
    private ArrayList<SatelliteAgent> generateSpaceSegment(){
        ArrayList<SatelliteAgent> spaceSegment = new ArrayList<>();

        Constellation senseSats = orbitData.getSensingSats();
        Constellation commsSats = orbitData.getCommsSats();

        for(Satellite sat : senseSats.getSatellites()){
            AbstractPlanner planner = loadSensingPlanner();
            Attitude attitude = new Attitude(0.0,  Math.toRadians(90.0), Math.toRadians(1.0));
            SensingSatellite sensingSat = new SensingSatellite(senseSats, sat, orbitData, attitude, planner, myGroups, loggerLevel);
            spaceSegment.add(sensingSat);
        }
        for(Satellite sat : commsSats.getSatellites()){
            AbstractPlanner planner = loadCommsPlanner();
            Attitude attitude = new Attitude(0.0, 0.0, 0.0);
            CommsSatellite commsSat = new CommsSatellite(commsSats, sat, orbitData, attitude, planner, myGroups, loggerLevel);
            spaceSegment.add(commsSat);
        }

        return spaceSegment;
    }

    /**
     * Reads input files and databases to create an array containing all ground station agents present
     * in this simulation
     * @return gndSegment : array containing all ground station agents in the simulation
     */
    private ArrayList<GndStationAgent> generateGroundSegment(){
        ArrayList<GndStationAgent> gndSegment = new ArrayList<>();

        for(GndStation gnd : orbitData.getUniqueGndStations()){
            GndStationAgent gndStatAgent = new GndStationAgent(gnd, orbitData, myGroups, loggerLevel);
            gndSegment.add(gndStatAgent);
        }

        return gndSegment;
    }

    /**
     * Reads input files and databases to create the planners that will schedule a remote sensing satellite's
     * activities for the simulation
     * @return planner : a planner object that creates a schedule given an agent and its known information
     */
    private AbstractPlanner loadSensingPlanner(){
        AbstractPlanner planner;
        String plannerStr = ((JSONObject) input.get(PLNR)).get(PLNR_NAME).toString();
        double planningHorizon = Double.parseDouble( ((JSONObject) input.get(PLNR)).get(PLN_HRZN).toString() );
        int threshold = Integer.parseInt( ((JSONObject) input.get(PLNR)).get(PLN_THRSHLD).toString() );
        boolean crossLinks = Boolean.parseBoolean( ((JSONObject) input.get(SIM)).get(CRSSLNKS).toString() );

        switch (plannerStr){
            case AbstractPlanner.NONE:
                planner = new NominalPlanner(planningHorizon, threshold, crossLinks);
                break;
//            case AbstractPlanner.TIME:
//                planner = new TimePriorityPlanner();
//                break;
//            case AbstractPlanner.CCBBA:
//                planner = new CCBBAPlanner();
//                break;
            default:
                throw new InputMismatchException("Input error. Planner " + plannerStr + " not yet supported.");
        }

        return planner;
    }

    /**
     * Reads input files and databases to create the planners that will schedule a communications satellite's
     * activities for the simulation
     * @return planner : a planner object that creates a schedule given an agent and its known information
     */
    private AbstractPlanner loadCommsPlanner(){
        double planningHorizon = Double.parseDouble( ((JSONObject) input.get(PLNR)).get(PLN_HRZN).toString() );
        int threshold = Integer.parseInt( ((JSONObject) input.get(PLNR)).get(PLN_THRSHLD).toString() );

        return new RelayPlanner(planningHorizon, threshold);
    }

    /**
     * Sets logger level for all agents in the simulation
     */
    private void setLogger() {
        String lvl = ((JSONObject) input.get(SETTINGS)).get(LEVEL).toString();

        switch (lvl) {
            case "OFF":
                loggerLevel = Level.OFF;
                getLogger().setLevel(Level.OFF);
                break;
            case "SEVERE":
                loggerLevel = Level.SEVERE;
                getLogger().setLevel(Level.SEVERE);
                break;
            case "WARNING":
                loggerLevel = Level.WARNING;
                getLogger().setLevel(Level.WARNING);
                break;
            case "INFO":
                loggerLevel = Level.INFO;
                getLogger().setLevel(Level.INFO);
                break;
            case "CONFIG":
                loggerLevel = Level.CONFIG;
                getLogger().setLevel(Level.CONFIG);
                break;
            case "FINE":
                loggerLevel = Level.FINE;
                getLogger().setLevel(Level.FINE);
                break;
            case "FINER":
                loggerLevel = Level.FINER;
                getLogger().setLevel(Level.FINER);
                break;
            case "FINEST":
                loggerLevel = Level.FINEST;
                getLogger().setLevel(Level.FINEST);
                break;
            case "ALL":
                loggerLevel = Level.ALL;
                getLogger().setLevel(Level.ALL);
                break;
            default:
                throw new InputMismatchException("Input file format error. " + lvl
                        + " not currently supported for field " + LEVEL);
        }
    }
}
