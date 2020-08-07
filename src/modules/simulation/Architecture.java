package modules.simulation;

import madkit.kernel.AbstractAgent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.HashMap;

public class Architecture {
    private String inputFileName;
    private JSONObject inputDataSpace;
    private JSONObject inputDataGround;
    private HashMap<String, AbstractAgent> spaceSegment;
    private HashMap<String, AbstractAgent> groundSegment;

    public Architecture(String inputFileName){
        this.inputFileName = inputFileName;
        this.inputDataSpace = (JSONObject) readJSON(inputFileName).get("spaceSegment");
        this.inputDataGround = (JSONObject) readJSON(inputFileName).get("groundSegment");
        this.spaceSegment = initiateSpaceSegment();
        this.groundSegment = initiateGroundSegment();
    }

    private JSONObject readJSON(String inputFileName){
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(
                    "src/inputs/" + inputFileName));
            return (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, AbstractAgent> initiateSpaceSegment(){
        /* for every spacecraft:
            Design Spacecraft
                EPS design
                Payload capabilities
            Propagate orbit
                Calculate location vs time
                Access times to each task
                Calculate eclipse
            Initiate planner

        */
        return null;
    }

    private HashMap<String, AbstractAgent> initiateGroundSegment(){
        return null;
    }
}
