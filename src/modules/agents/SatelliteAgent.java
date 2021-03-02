package modules.agents;

import madkit.kernel.Agent;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.simulation.OrbitData;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class SatelliteAgent extends Agent {
    /**
     * orekit satellite represented by this agent
     */
    private Satellite sat;

    /**
     * ground point coverage, station overage, and cross-link access times for this satellite
     */
    private HashMap<Satellite, TimeIntervalArray> accessesCL;
    private HashMap<TopocentricFrame, TimeIntervalArray> accessGP;
    private HashMap<Instrument, HashMap<TopocentricFrame, TimeIntervalArray>> accessGPInst;
    HashMap<GndStation, TimeIntervalArray> accessGS;

    /**
     * list of measurements requests received by this satellite at the current simulation time
     */
    private ArrayList<MeasurementRequest> requestsReceived;

    /**
     * list of measurements performed by spacecraft pending to be downloaded to a the next visible ground station
     * or comms relay satellite
     */
    private ArrayList<Measurement> measurementsPendingDownload;

    /**
     * overall list of measurements performed by this spacecraft
     */
    private ArrayList<Measurement> measurementsDone;


    public SatelliteAgent(Constellation cons, Satellite sat, OrbitData orbitData){
        this.sat = sat;
        this.accessesCL = new HashMap<>( orbitData.getAccessesCL().get(cons).get(sat) );
        this.accessGP = new HashMap<>();
        this.accessGPInst = new HashMap<>();
        for(CoverageDefinition covDef : orbitData.getCovDefs()){
            accessGP.putAll( orbitData.getAccessesGP().get(covDef).get(sat) );
            for(Instrument ins : sat.getPayload()){
                accessGPInst.put(ins, orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins));
            }
        }
        this.accessGS = new HashMap<>( orbitData.getAccessesGS().get(sat) );
    }

    @Override
    abstract protected void live();

    public String getName(){
        return sat.getName();
    }
}
