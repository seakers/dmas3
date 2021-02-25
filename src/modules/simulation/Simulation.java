package modules.simulation;

import constants.JSONFields;
import jxl.read.biff.BiffException;
import madkit.kernel.Agent;
import modules.environment.Environment;
import modules.measurements.MeasurementRequest;
import org.json.simple.JSONObject;
import seakers.orekit.event.CrossLinkEventAnalysis;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static constants.JSONFields.SIM_NAME;

public class Simulation extends Agent{
    private int simID;
    private JSONObject input;
    private OrbitData orbitData;
    private boolean createFrame = true;
    private String directoryAddress;
    private String simDirectoryAddress;

    private SimGroups myGroups;
    private Environment environment;

    private ArrayList<Agent> satellites;

    public Simulation(JSONObject input, OrbitData orbitData, String directoryAddress, int simID){
        this.simID = simID;
        this.input = input;
        this.orbitData = orbitData;
        this.directoryAddress = directoryAddress;
        this.simDirectoryAddress = directoryAddress + "/run_" + simID;

        if(simID == 0 && input.get(JSONFields.GUI).equals(true)) createFrame = true;

        // Create simulation directory
        createDirectory();
    }

    @Override
    public void activate(){
        try {
            // 1- create the simulation group
            myGroups = new SimGroups(input, simID);
            createGroup(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP);

            // 2- launch simulation environment
            environment = new Environment(input, orbitData, this);
            launchAgent(this.environment, createFrame);
//
//            // 3- launch architecture agents
//            launchSpaceSegment();
//
//            // 4- launch simulation scheduler
//            launchAgent(new SimScheduler(), false);
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

    public SimGroups getSimGroups(){ return myGroups; }
    public String getSimDirectoryAddress(){ return simDirectoryAddress; }
}
