package CCBBA;

import CCBBA.lib.Scenario;
import CCBBA.lib.SimGroups;
import CCBBA.lib.SimulatedAgent;
import madkit.kernel.AbstractAgent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Driver extends AbstractAgent {
    /**
     * Organizational parameters
     */
    private JSONObject inputData;
    private String directoryAddress;
    private int numAgents;

    /**
     * Sim Setup
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        executeThisAgent(1, false);
    }

    @Override
    protected void activate(){
        // 1 : load sim inputs
        inputSimData("inputTest.json");

        // 2 : create results directory
        createFileDirectory();

        // 3 : create the simulation group
        createGroup(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);

        // 4 : create simulation environment
        try {
            launchAgent( new Scenario( inputData ) );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Helping functions
     */
    private void inputSimData(String fileName){
        // reads intput json file
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(
                    "src/CCBBA/inputs/" + fileName));
            this.inputData = (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFileDirectory(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();

        String simName = (String) inputData.get("SimName");
        this.directoryAddress = "src/CCBBA/results/results-" + simName + "-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }

//    private void setupAgents(){
//        ArrayList agentList = (ArrayList) inputData.get("AgentList");
//        for(int i = 0; i < agentList.size(); i++){
//            JSONObject agentSpecs = (JSONObject) agentList.get(i);
//            int instances = (int) agentSpecs.get("Instances");
//            for(int j = 0; j < instances; j++) {
//                launchAgent(new SimulatedAgent(agentSpecs));
//            }
//        }
//    }
}
