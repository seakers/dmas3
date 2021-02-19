package modules.simulation;

import static constants.JSONFields.*;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import madkit.kernel.Agent;
import modules.instruments.SAR;
import org.hipparchus.util.FastMath;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.event.CrossLinkEventAnalysis;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewAndGndStationEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Dmas extends Agent {
    public static String inputFile = null;
    private static final String inputDir = "./inputs/";
    private static final String resultsDir = "./results/";
    private static final String coverageDir = "./data/coverage/";
    private static final String constellationsDir = "./data/constellations/";
    private static final String databaseDir = "./data/databases/";
    private static final String scenarioDir = "./data/scenarios/";
    private static final String orekitDataDir = "./src/orekit-data";

    private OrbitData orbitData;

    @Override
    public void activate(){
        try {
            // 1- Check input format
            if(inputFile == null) throw new InputMismatchException("No input file selected.");

            // 2- Read input file
            JSONObject input = parseJSON(inputFile);

            // 3- Create results directory
//            createSimDirectory(input);

            // 4- Set Logger Level
            setLogger(input);

            // 5- Coverage and Crosslink Calculation
            orbitData = new OrbitData(input, orekitDataDir, databaseDir, coverageDir, constellationsDir, scenarioDir);
            orbitData.propagate();

            // 6- Generate simulation scenarios
            int n_sims = Integer.parseInt( input.get(N_SIMS).toString() );
            if(n_sims <= 0) throw new InputMismatchException("Invalid value for " + N_SIMS);

            ArrayList<Simulation> sims = new ArrayList<>();
            for(int i = 0; i < n_sims; i++){
                sims.add(new Simulation(input, orbitData, i));
            }

            // 7- Execute simulations
            for(Simulation sim : sims) launchAgent(sim);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void live(){}

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

    private void createSimDirectory(JSONObject input){
        LocalDateTime now = LocalDateTime.now();

        String simName = input.get(SIM_NAME).toString();
        String directoryAddress = resultsDir + simName + "_" + now.toString();
        new File( directoryAddress ).mkdir();
    }

    private void createCoverageDirectory(JSONObject input){

    }

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
}
