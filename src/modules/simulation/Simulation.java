package modules.simulation;

import constants.JSONFields;
import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import modules.agents.CommsSatellite;
import modules.agents.GndStationAgent;
import modules.agents.SatelliteAgent;
import modules.agents.SensingSatellite;
import modules.environment.Environment;
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

import static constants.JSONFields.*;

public class Simulation extends AbstractAgent{
    private String name;
    private int simID;
    private JSONObject input;
    private OrbitData orbitData;
    private boolean createFrame = true;
    private String directoryAddress;
    private String simDirectoryAddress;

    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    private SimGroups myGroups;
    private Environment environment;

    private ArrayList<SatelliteAgent> spaceSegment;
    private ArrayList<GndStationAgent> gndSegment;

    public Simulation(JSONObject input, OrbitData orbitData, String directoryAddress, int simID){
        this.name = ((JSONObject) input.get(SIM)).get(SCENARIO).toString() + "-" + simID;
        this.simID = simID;
        this.input = input;
        this.orbitData = orbitData;
        this.startDate = orbitData.getStartDate();
        this.endDate = orbitData.getEndDate();
        this.directoryAddress = directoryAddress;
        this.simDirectoryAddress = directoryAddress + "/run_" + simID;

        if(simID == 0 && ((JSONObject) input.get(SETTINGS)).get(GUI).equals(true)) createFrame = true;

        // Create simulation directory
        createDirectory();
    }

    @Override
    protected void activate(){
        try {
            // 0- if frame created, log welcome message
            if(createFrame) logWelcome();

            // 1- create the simulation group
            myGroups = new SimGroups(input, simID);
            createGroup(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP);

            // 2- generate agents
            environment = new Environment(input, orbitData, this);
            spaceSegment = generateSpaceSegment();
            gndSegment = generateGroundSegment();

            // 3- launch agents
            launchAgent(environment, createFrame);
            for(AbstractAgent satAgent : spaceSegment) launchAgent(satAgent, createFrame);
            for(AbstractAgent gndAgent : gndSegment) launchAgent(gndAgent, false);

            // 4 - give agent addresses to all agents
            HashMap<Satellite, AgentAddress> satAddresses = this.getSatAddresses();
            HashMap<GndStation, AgentAddress> gndAddresses = this.getGndAddresses();
            this.registerAddresses(satAddresses, gndAddresses);

            // 5- launch simulation
            launchAgent(new SimScheduler(myGroups, startDate, endDate), true);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    private void registerAddresses(HashMap<Satellite, AgentAddress> satAdd, HashMap<GndStation, AgentAddress> gndAdd){
        for(SatelliteAgent sat : spaceSegment){
            sat.registerAddresses(satAdd, gndAdd);
        }
        for(GndStationAgent gnd : gndSegment){
            gnd.registerAddresses(satAdd, gndAdd);
        }
    }

    private HashMap<Satellite, AgentAddress> getSatAddresses(){
        HashMap<Satellite, AgentAddress> addresses = new HashMap<>();

        for(SatelliteAgent sat : spaceSegment){
            addresses.put(sat.getSat(), sat.getAgentAddressIn(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE));
        }

        return addresses;
    }

    private HashMap<GndStation, AgentAddress> getGndAddresses(){
        HashMap<GndStation, AgentAddress> addresses = new HashMap<>();
        for(GndStationAgent gnd : gndSegment){
            AgentAddress address = gnd.getAgentAddressIn(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT);
            if(address == null) {
                int x = 1;
            }
            addresses.put(gnd.getGnd(), address);
        }

        return addresses;
    }

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

    private ArrayList<SatelliteAgent> generateSpaceSegment(){
        ArrayList<SatelliteAgent> spaceSegment = new ArrayList<>();

        Constellation senseSats = orbitData.getSensingSats();
        Constellation commsSats = orbitData.getCommsSats();

        for(Satellite sat : senseSats.getSatellites()){
            AbstractPlanner planner = loadSensingPlanner();
            SensingSatellite sensingSat = new SensingSatellite(senseSats, sat, orbitData, planner, myGroups);
            spaceSegment.add(sensingSat);
        }
        for(Satellite sat : commsSats.getSatellites()){
            AbstractPlanner planner = loadCommsPlanner();
            CommsSatellite commsSat = new CommsSatellite(commsSats, sat, orbitData, planner, myGroups);
            spaceSegment.add(commsSat);
        }

        return spaceSegment;
    }

    private ArrayList<GndStationAgent> generateGroundSegment(){
        ArrayList<GndStationAgent> gndSegment = new ArrayList<>();

        for(GndStation gnd : orbitData.getUniqueGndStations()){
            GndStationAgent gndStatAgent = new GndStationAgent(gnd, orbitData, myGroups);
            gndSegment.add(gndStatAgent);
        }

        return gndSegment;
    }

    private void logWelcome() {
        String str = "\n    ____  __  ______   __________\n" +
                "   / __ \\/  |/  /   | / ___/__  /\n" +
                "  / / / / /|_/ / /| | \\__ \\ /_ < \n" +
                " / /_/ / /  / / ___ |___/ /__/ / \n" +
                "/_____/_/  /_/_/  |_/____/____/  \n" +
                "SEAK Lab - Texas A&M University\n";

        getLogger().severeLog(str);
    }

    private AbstractPlanner loadSensingPlanner(){
        AbstractPlanner planner;
        String plannerStr = ((JSONObject) input.get(PLNR)).get(PLNR_NAME).toString();

        switch (plannerStr){
            case AbstractPlanner.NONE:
                planner = new BasicPlanner();
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

    private AbstractPlanner loadCommsPlanner(){
        return new RelayPlanner();
    }

    public SimGroups getSimGroups(){ return myGroups; }
    public String getSimDirectoryAddress(){ return simDirectoryAddress; }
}
