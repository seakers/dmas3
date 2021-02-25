package modules.simulation;

import static constants.JSONFields.*;

import constants.JSONFields;
import madkit.kernel.Agent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;


public class Dmas extends Agent {
    public static String inputFile = null;
    private static final String inputDir = "./inputs/";
    private static final String resultsDir = "./results/";
    private static final String coverageDir = "./data/coverage/";
    private static final String constellationsDir = "./data/constellations/";
    private static final String databaseDir = "./data/databases/";
    private static final String scenarioDir = "./data/scenarios/";
    private static final String orekitDataDir = "./src/orekit-data";
    private static String directoryAddress;

    private OrbitData orbitData;

    @Override
    public void activate(){
        try {
            // 0- Print welcome message
            logWelcome();

            // 1- Check input format
            getLogger().info("Loading " + inputFile + " input file...");
            if(inputFile == null) throw new InputMismatchException("No input file selected.");

            // 2- Read input file
            JSONObject input = parseJSON(inputFile);

            // 3- Set Logger Level
            setLogger(input);
            logInput(input);

            // 4- Create results directory
            createDirectory(input);

            // 5- Coverage and Cross Link Calculation
            getLogger().info("Loading constellation and scenario data...");
            orbitData = new OrbitData(input, orekitDataDir, databaseDir, coverageDir, constellationsDir, scenarioDir);
            getLogger().info("Calculating coverage...");
            orbitData.coverageCalc();
            getLogger().info("Propagating satellite trajectories...");
            orbitData.trajectoryCalc();
            getLogger().info("Printing coverage definition ground points...");
            orbitData.printGP();

            // 6- Generate simulation scenarios
            int n_sims = Integer.parseInt( input.get(N_SIMS).toString() );
            if(n_sims <= 0) throw new InputMismatchException(
                    "Invalid value for " + N_SIMS + ". Must run at least once.");
            else if(n_sims == 1) getLogger().info("Generating single scenario...");
            else getLogger().info("Generating " + n_sims + " scenarios...");

            ArrayList<Simulation> sims = new ArrayList<>();
            for(int i = 0; i < n_sims; i++){
                sims.add(new Simulation(input, orbitData, directoryAddress, i));
            }

            // 7- Execute simulations
            for(Simulation sim : sims){
                boolean createFrame = false;
                if(sims.indexOf(sim) == 0
                        && input.get(JSONFields.GUI).equals(true)) createFrame = true;
                if(sims.indexOf(sim) == 1) launchAgent(sim, createFrame);
                else launchAgent(sim, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void live(){}

    private void logWelcome(){
        String str = "\n    ____  __  ______   __________\n" +
                "   / __ \\/  |/  /   | / ___/__  /\n" +
                "  / / / / /|_/ / /| | \\__ \\ /_ < \n" +
                " / /_/ / /  / / ___ |___/ /__/ / \n" +
                "/_____/_/  /_/_/  |_/____/____/  \n" +
                "SEAK Lab - Texas A&M University\n";

        getLogger().severeLog(str);
    }

    /**
     * Reads JSON input file and creates a JSON object to be used across the simulation
     * @param fileName : string containing the name of the desired input file within the /inputs folder
     * @return input : JSON Object of loaded input json file
     * @throws IOException
     * @throws ParseException
     */
    private JSONObject parseJSON(String fileName) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        String fileAddress = inputDir + fileName;
        JSONObject input = (JSONObject) parser.parse(new FileReader(fileAddress));

        for(String field : FIELDS){
            if(input.get(field) == null) throw new InputMismatchException("Input file format error. "
                    + field + " field not found");
        }

        return input;
    }

    /**
     * Logs information from input file
     * @param input Loaded JSON input file
     */
    private void logInput(JSONObject input){
        getLogger().config("Simulation Inputs:\n" +
                "Constellation: \t" + input.get(CONSTELLATION).toString() + "\n" +
                "Ground Station\nNetwork: \t\t" + input.get(GROUND_STATIONS).toString() + "\n" +
                "Scenario: \t\t" + input.get(SCENARIO).toString() + "\n" +
                "Start Date: \t" + input.get(START_DATE).toString() + "\n" +
                "End Date: \t\t" + input.get(END_DATE).toString() + "\n");
    }

    /**
     * Creates a directory where all runs will save their results
     * @param input imported JSON input file
     */
    private void createDirectory(JSONObject input){
        getLogger().info("Creating simulation results directory...");

        LocalDateTime now = LocalDateTime.now();

        String simName = input.get(SIM_NAME).toString();

        directoryAddress = resultsDir + simName + "_" + now.toString();
        if(!new File( directoryAddress ).exists()) {
            new File(directoryAddress).mkdir();
            getLogger().config("Simulation results directory created at\n" + directoryAddress);
        }
        else{
            getLogger().config("Simulation results directory already exists at\n" + directoryAddress);
        }
    }

    /**
     * Sets logger level for all agents in the simulation
     * @param input imported JSON input file
     */
    private void setLogger(JSONObject input){
        String lvl = input.get(LEVEL).toString();

        switch(lvl){
            case "OFF":
                getLogger().setLevel(Level.OFF);
                break;
            case "SEVERE":
                getLogger().setLevel(Level.SEVERE);
                break;
            case "WARNING":
                getLogger().setLevel(Level.WARNING);
                break;
            case "INFO":
                getLogger().setLevel(Level.INFO);
                break;
            case "CONFIG":
                getLogger().setLevel(Level.CONFIG);
                break;
            case "FINE":
                getLogger().setLevel(Level.FINE);
                break;
            case "FINER":
                getLogger().setLevel(Level.FINER);
                break;
            case "FINEST":
                getLogger().setLevel(Level.FINEST);
                break;
            case "ALL":
                getLogger().setLevel(Level.ALL);
                break;
            default:
                throw new InputMismatchException("Input file format error. " + lvl
                        + " not currently supported for field " + LEVEL);
        }
    }

    public String getDirectoryAddress(){
        return directoryAddress;
    }
}
