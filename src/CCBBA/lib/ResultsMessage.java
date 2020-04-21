package CCBBA.lib;

import madkit.kernel.Message;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.PVCoordinates;

public class ResultsMessage extends Message {
    private IterationResults results;
    private PVCoordinates location;

    public ResultsMessage(IterationResults results, SimulatedAgent agent){
        this.results = new IterationResults(results, agent);

        if(agent.getEnvironment().getWorldType().equals("3D_Earth")) {
            Vector3D position = new Vector3D(agent.getPositionPV().getPosition().getX(),
                    agent.getPositionPV().getPosition().getY(),
                    agent.getPositionPV().getPosition().getZ());
            Vector3D velocity = new Vector3D(agent.getPositionPV().getVelocity().getX(),
                    agent.getPositionPV().getVelocity().getY(),
                    agent.getPositionPV().getVelocity().getZ());
            Vector3D acceleration = new Vector3D(agent.getPositionPV().getAcceleration().getX(),
                    agent.getPositionPV().getAcceleration().getY(),
                    agent.getPositionPV().getAcceleration().getZ());

            this.location = new PVCoordinates(position, velocity, acceleration);
        }
    }
    public IterationResults getResults(){ return this.results; }
    public  PVCoordinates getLocation(){return this.location;}
    public String getSenderName(){ return results.getParentAgent().getName(); }
}
