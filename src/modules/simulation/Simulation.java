package modules.simulation;

import constants.JSONFields;
import jxl.read.biff.BiffException;
import madkit.kernel.Agent;
import modules.agents.CommsSatellite;
import modules.agents.GndStatAgent;
import modules.agents.SensingSatellite;
import modules.environment.Environment;
import modules.planner.*;
import org.json.simple.JSONObject;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;

import static constants.JSONFields.*;

public class Simulation extends Agent{
    private int simID;
    private JSONObject input;
    private OrbitData orbitData;
    private boolean createFrame = true;
    private String directoryAddress;
    private String simDirectoryAddress;

    private SimGroups myGroups;
    private Environment environment;

    private ArrayList<Agent> spaceSegment;
    private ArrayList<Agent> gndSegment;

    public Simulation(JSONObject input, OrbitData orbitData, String directoryAddress, int simID){
        this.simID = simID;
        this.input = input;
        this.orbitData = orbitData;
        this.directoryAddress = directoryAddress;
        this.simDirectoryAddress = directoryAddress + "/run_" + simID;

        if(simID == 0 && ((JSONObject) input.get(SETTINGS)).get(JSONFields.GUI).equals(true)) createFrame = true;

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

            // 2- launch simulation environment
            environment = new Environment(input, orbitData, this);
            launchAgent(this.environment, createFrame);

            // 3- launch satellite agents
            spaceSegment = generateSpaceSegment();
            for(Agent satAgent : spaceSegment){
                launchAgent(satAgent, createFrame);
            }

            // 4 - launch ground station agents
            gndSegment = generateGroundSegment();
            for(Agent satAgent : gndSegment){
                launchAgent(satAgent, false);
            }

            // 5- launch simulation scheduler
            launchAgent(new SimScheduler(myGroups), false);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void live(){}

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

    private ArrayList<Agent> generateSpaceSegment(){
        ArrayList<Agent> spaceSegment = new ArrayList<>();

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

    private ArrayList<Agent> generateGroundSegment(){
        ArrayList<Agent> gndSegment = new ArrayList<>();

        for(GndStation gnd : orbitData.getUniqueGndStations()){
            GndStatAgent gndStatAgent = new GndStatAgent(orbitData, myGroups);
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
            case AbstractPlanner.TIME:
                planner = new TimePriorityPlanner();
                break;
            case AbstractPlanner.CCBBA:
                planner = new CCBBAPlanner();
                break;
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
