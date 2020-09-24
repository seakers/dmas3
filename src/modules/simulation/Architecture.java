package modules.simulation;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import madkit.kernel.AbstractAgent;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.InstrumentAntenna;
import modules.spacecraft.instrument.Radiometer;
import modules.spacecraft.instrument.SAR;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.orbits.OrbitParams;
import modules.spacecraft.instrument.measurements.Measurement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Architecture extends AbstractAgent{
    private String problemStatement;
    private String problemStatementDir;
    private String inputFileName;
    private String inputFileDir;
    private JSONArray inputDataSpace;
    private JSONArray inputDataGround;
    private JSONArray inputOrbitData;
    private ArrayList<Spacecraft> spaceSegment;
    private HashMap<String, AbstractAgent> groundSegment;

    public Architecture(String inputFile, String problemStatement) throws Exception {
        this.problemStatement = problemStatement;
        this.problemStatementDir = "./src/scenarios/" + problemStatement;
        this.inputFileName = inputFile + ".json";
        this.inputFileDir = "./src/inputs/" + inputFile + ".json";

        this.inputDataSpace = (JSONArray) readJSON().get("spaceSegment");
        this.inputDataGround = (JSONArray) readJSON().get("groundSegment");
        this.inputOrbitData = (JSONArray) readJSON().get("orbits");
        this.spaceSegment = new ArrayList<>(); this.spaceSegment.addAll( initiateSpaceSegment() );
        this.groundSegment = null;
    }

    private JSONObject readJSON(){
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.inputFileDir));
            return (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<Spacecraft> initiateSpaceSegment() throws Exception {
        // Read Instrument excel data and generate instruments and antennas
        Workbook instrumentDataXls = Workbook.getWorkbook(new File(problemStatementDir + "/Instrument Capabilities.xls"));

        // -create antennas
        Sheet antennas = instrumentDataXls.getSheet("Antennas");
        int nRowsAnts = antennas.getRows();
        HashMap<String, InstrumentAntenna> antennaList = new HashMap<>();
        for(int i = 1; i < nRowsAnts; i++){
            Cell[] row = antennas.getRow(i);
            String name = row[0].getContents();
            double dimAz = Double.parseDouble(row[1].getContents());
            double dimEl = Double.parseDouble(row[2].getContents());
            double mass = Double.parseDouble(row[3].getContents());
            String type = row[4].getContents();
            double eff = Double.parseDouble(row[5].getContents());
            InstrumentAntenna ant_i = new InstrumentAntenna(name, dimAz, dimEl, mass, type, eff);
            antennaList.put(name, ant_i);
        }

        // -create instruments
        Sheet instruments = instrumentDataXls.getSheet("Instruments");
        int nRowsIns = instruments.getRows();
        HashMap<String, Instrument> instrumentList = new HashMap<>();
        for (int i = 1; i < nRowsIns; i++) {
            // unpack data from spreadsheets
            Cell[] row = instruments.getRow(i);
            String name = row[0].getContents();
            double dataRate = Double.parseDouble(row[1].getContents());
            double pPeak = Double.parseDouble(row[2].getContents());
            double prf = Double.parseDouble(row[3].getContents());
            double pulseWidth = Double.parseDouble(row[4].getContents());
            double bandwidth = Double.parseDouble(row[5].getContents());
            double f = Double.parseDouble(row[6].getContents());
            Measurement freq = new Measurement(f);
            double mass = Double.parseDouble(row[7].getContents());
            double nLooks = Double.parseDouble(row[8].getContents());
            double offAxisAngle = Double.parseDouble(row[9].getContents());
            String scanningType = row[10].getContents();
            double scanningAngle = Double.parseDouble(row[11].getContents());
            String sensorType = row[12].getContents();
            String antennaName = row[13].getContents();

            InstrumentAntenna ant = antennaList.get(antennaName);
            Instrument ins;
            switch (sensorType){
                case "SAR":
                    ins = new SAR(name, dataRate, pPeak, prf, pulseWidth, freq, bandwidth, offAxisAngle, mass, scanningType, scanningAngle/2.0, scanningAngle/2.0, sensorType, ant, nLooks);
                    break;
                case "RAD":
                    ins = new Radiometer(name, dataRate, pPeak, pulseWidth, freq, bandwidth, offAxisAngle, mass, scanningType, scanningAngle/2.0, scanningAngle/2.0, sensorType, ant, nLooks);
                    break;
                default:
                    throw new Exception("Sensor type not yet supported");
            }
            instrumentList.put(name, ins);
        }

        // Load Orbital Parameters
        HashMap<String, OrbitParams> orbitList = new HashMap<>();
        for(int i = 0; i < inputOrbitData.size(); i++){
            JSONObject orbit_i = (JSONObject) inputOrbitData.get(i);
            String type = orbit_i.get("@type").toString();

            String name;
            double alt;
            double ecc;
            double inc;
            double parg;
            double raan;
            double anom;
            String incName;
            String date;
            OrbitParams orbit;

            switch (type){
                case "WalkerConstellation":
                    throw new Exception("Constellation orbit input not yet supported");
                case "OrbitalElements":
                    name = orbit_i.get("name").toString();
                    alt = Double.parseDouble(orbit_i.get("altitude").toString());
                    ecc = Double.parseDouble(orbit_i.get("eccentricity").toString());
                    inc = Double.parseDouble(orbit_i.get("inclination").toString());
                    parg =Double.parseDouble(orbit_i.get("argumentOfPerigee").toString());
                    raan =Double.parseDouble(orbit_i.get("raan").toString());
                    anom =Double.parseDouble(orbit_i.get("anomaly").toString());

                    orbit = new OrbitParams(name, alt, ecc, inc, parg, raan, anom);
                    orbitList.put(name, orbit);
                    break;
                case "OrbitName":
                    name = orbit_i.get("name").toString();
                    alt = Double.parseDouble(orbit_i.get("altitude").toString());
                    incName = orbit_i.get("inclination").toString();
                    date = orbit_i.get("date").toString();

                    orbit = new OrbitParams(name, alt, incName, date);
                    orbitList.put(name, orbit);
                    break;
                default:
                    throw new Exception("Orbit type not yet supported");
            }
        }

        // Assign payloads and orbits to each spacecraft
        ArrayList<Spacecraft> spaceSegment = new ArrayList<>();
        for(int i = 0; i < inputDataSpace.size(); i++){
            // Unpack JSON data
            JSONObject sat_i = (JSONObject) inputDataSpace.get(i);
            String name = sat_i.get("name").toString();
            String orbitName = sat_i.get("orbit").toString();
            String plannerName = sat_i.get("planner").toString();
            JSONArray payloadData = (JSONArray) sat_i.get("payload");

            ArrayList<Instrument> payload  = new ArrayList<>();
            for(int j = 0; j < payloadData.size(); j++){
                String instrumentName = payloadData.get(j).toString();
                if(!instrumentList.containsKey(instrumentName)){
                    throw new Exception("INPUT ERROR. " + instrumentName + " not found in problem statement folder");
                }
                Instrument instrument = instrumentList.get(instrumentName);
                payload.add(instrument);
            }
            OrbitParams orbit = orbitList.get(orbitName);

            // Create spacecraft agent
            Spacecraft spacecraft = new Spacecraft(name, payload, orbit, plannerName);
            spaceSegment.add(spacecraft);
        }
        if(spaceSegment.size() == 0) throw new Exception("No spacecraft loaded onto simulation");
        else return spaceSegment;
    }

    public ArrayList<Spacecraft> getSpaceSegment(){return this.spaceSegment;}
}
