package modules.simulation;

import static constants.JSONFields.*;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import modules.orbitData.OrbitData;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;

/**
 *     ____  __  ______   __________
 *    / __ \/  |/  /   | / ___/__  /
 *   / / / / /|_/ / /| | \__ \ /_ <
 *  / /_/ / /  / / ___ |___/ /__/ /
 * /_____/_/  /_/_/  |_/____/____/
 * SEAK Lab - Texas A&M University
 *
 * Distributed Multi-Agent Satellite System Simulation
 *
 * Main class for DMAS3 simulation. It simulates and evaluates the performance of an Earth
 * Observation Satellite System Architecture by considering the different levels of autonomy
 * that can be given to Earth-Observing Spacecraft.
 *
 * This class loads input files as well as coverage and instrument databases to create as
 * many simulations as desired and help evaluate the performance of a constellation under a
 * particular Earth-observation problem.
 *
 * To run, create an extended object of this class that contains a main() function. In it,
 * ensure that the name of the desired input file is assigned to the inputFile string variable.
 *
 * @author a.aguilar
 */
public class Dmas extends AbstractAgent {
    /**
     * Name of input file to be opened
     */
    public static String inputFile = null;

    /**
     * Location of input file directory
     */
    private static final String inputDir = "./inputs/";

    /**
     * Location of results directory
     */
    private static final String resultsDir = "./results/";

    /**
     * Location of coverage data directory
     */
    private static final String coverageDir = "./data/coverage/";

    /**
     * Location of constellation data directory
     */
    private static final String constellationsDir = "./data/constellations/";

    /**
     * Location of database directory
     */
    private static final String databaseDir = "./data/databases/";

    /**
     * Location of scenario directory
     */
    private static final String scenarioDir = "./data/scenarios/";

    /**
     * Location of orekit data directory
     */
    private static final String orekitDataDir = "./src/orekit-data";

    /**
     * Location of the directory where the simulation outputs will be printed to
     */
    private static String directoryAddress;

    /**
     * Coverage and access data from loaded scenario and constellation
     */
    private OrbitData orbitData;

    /**
     * Triggered when DMAS is activated. Reads input files and databases to generates different
     * simulations based on the inputs.
     */
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
            double tic = System.nanoTime();

                getLogger().info("Loading constellation and scenario data...");
                orbitData = new OrbitData(input, orekitDataDir, databaseDir, coverageDir, constellationsDir, scenarioDir);

                getLogger().info("Calculating coverage...");
                orbitData.coverageCalc();

                getLogger().info("Propagating satellite trajectories...");
                orbitData.trajectoryCalc();

                getLogger().info("Printing coverage definition ground points...");
                orbitData.printGP();

            double toc = (System.nanoTime() - tic);
            getLogger().fine("Coverage metrics loaded. Runtime of " + toc + " ns");

            // 6- Generate simulation scenarios
            int n_sims = Integer.parseInt( ((JSONObject) input.get(SIM)).get(N_SIMS).toString() );
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
                        && ((JSONObject) input.get(SETTINGS)).get(GUI).equals(true)) createFrame = true;
                if(sims.indexOf(sim) == 1) launchAgent(sim, createFrame);
                else launchAgent(sim, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints out welcome message on terminal
     */
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

        return input;
    }

    /**
     * Logs information from input file
     * @param input Loaded JSON input file
     */
    private void logInput(JSONObject input){
        getLogger().config("Simulation Inputs:\n" +
                "Constellation: \t" + ((JSONObject) input.get(SIM)).get(CONS).toString() + "\n" +
                "Gnd Stat Net: \t" + ((JSONObject) input.get(SIM)).get(GND_STATS).toString() + "\n" +
                "Scenario: \t\t" + ((JSONObject) input.get(SIM)).get(SCENARIO).toString() + "\n" +
                "Start Date: \t" + ((JSONObject) input.get(SIM)).get(START_DATE).toString() + "\n" +
                "End Date: \t\t" + ((JSONObject) input.get(SIM)).get(END_DATE).toString() + "\n");
    }

    /**
     * Creates a directory where all runs will save their results
     * @param input imported JSON input file
     */
    private void createDirectory(JSONObject input){
        getLogger().info("Creating simulation results directory...");

        LocalDateTime now = LocalDateTime.now();

        String simName = ((JSONObject) input.get(SIM)).get(SIM_NAME).toString();

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
        String lvl = ((JSONObject) input.get(SETTINGS)).get(LEVEL).toString();

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
}
