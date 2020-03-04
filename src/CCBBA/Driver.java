package CCBBA;

import CCBBA.lib.*;
import madkit.kernel.AbstractAgent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Driver extends AbstractAgent {
    /**
     * Organizational parameters
     */

    private static JSONArray inputBatch;
    private static JSONObject inputData;
    private String directoryAddress = null;
    private static int numRuns = 1;
    private static String output;

    /**
     * Sim Setup
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        // 0: load batch file - Uncomment desired run
//        inputSimBatch("figure_3/figure3_batch.json");
//        inputSimBatch("figure_3/figure3_int.json");
//        inputSimBatch("figure_3/figure3_mod_4.json");
//        inputSimBatch("figure_3_debug/figure3_int_debug.json");
        inputSimBatch("figure_3_debug/figure3_mod_debug.json");
//        inputSimBatch("appendix_b/AppendixB_batch.json");

        if(inputBatch != null) {
            for (int i = 0; i < inputBatch.size(); i++) {
                // 1 : load sim inputs
                inputSimData(inputBatch.get(i).toString());

                for (int j = 0; j < numRuns; j++) {
                    executeThisAgent(1, false);
                }
            }
        }
        else{
            for (int j = 0; j < numRuns; j++) {
                executeThisAgent(1, false);
            }
        }
    }

    @Override
    protected void activate(){
        try {
            // 2 : create results directory
            if( !(this.output.equals("OFF") || this.output.equals("off") || this.output.equals("Off")) ){
                createFileDirectory();
            }

            // 3 : create the simulation group
            createGroup(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);

            // 4 : create simulation environment
            launchAgent( new Scenario( inputData ) );

            // 5 : launch agents
            setupAgents();

            // 6 : create task scheduler
            launchAgent(new Planner("CCBBA"), false);

            // 7 : launch results compiler
            launchAgent( new ResultsCompiler( this.directoryAddress ), false );

        } catch (Exception e) { e.printStackTrace(); }
    }


    /**
     * Helping functions
     */
    private static void inputSimBatch(String batchName){
        // reads intput json file
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(
                    "src/CCBBA/inputs/" + batchName));
            if(((JSONObject) obj).get("SimList") == null){
                if(((JSONObject) obj).get("SimName") != null){
                    inputSimData(batchName);
                }
            }
            else {
                inputBatch = (JSONArray) ((JSONObject) obj).get("SimList");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void inputSimData(String fileName){
        // reads intput json file
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(
                    "src/CCBBA/inputs/" + fileName));
            inputData = (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }

        numRuns = Integer.valueOf( inputData.get("NumRuns").toString() );
        output = inputData.get("OutputSwitch").toString();
    }

    private void createFileDirectory(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();

        String simName = (String) inputData.get("SimName");
        this.directoryAddress = "src/CCBBA/outputs/" + simName;
        new File( this.directoryAddress ).mkdir();
        this.directoryAddress += "/results-" + simName + "-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }

    private void setupAgents() throws Exception {
        JSONObject agentData = (JSONObject) inputData.get("Agents");
        JSONArray  agentList = (JSONArray) agentData.get("AgentList");

        for(int i = 0; i < agentList.size(); i++){
            JSONObject agentSpecs = (JSONObject) agentList.get(i);
            int instances = Integer.valueOf( agentSpecs.get("Instances").toString() );
            for(int j = 0; j < instances; j++) {
                launchAgent(new SimulatedAgent(agentSpecs, this.inputData, j+1));
            }
        }
//        this.numAgents = agentList.size();
    }
}
