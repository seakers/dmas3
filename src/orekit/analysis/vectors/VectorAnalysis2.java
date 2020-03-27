/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.vectors;

import java.util.logging.Level;
import java.util.logging.Logger;
import seakers.orekit.analysis.Record;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This analysis will record how a vector changes in the given frame of reference. 
 * The vector must be accessible from the spacecraft state or this CelestialBody.
 * This class is very similar to VectorAnalysis but we can have access to a 
 * CelestialBody such as sun or moon.
 *
 * @author paugarciabuzzi
 */
public abstract class VectorAnalysis2 extends AbstractSpacecraftAnalysis<String> {

    /**
     * The desired frame to use. Frame used to transform vectors into the same
     * frame.
     */
    private final Frame frame;
    private final CelestialBody body;

    /**
     * Constructor to obtain a 3D vector in the specified frame at the specified time step.
     *
     * @param startDate
     * @param endDate
     * @param timeStep
     * @param sat
     * @param propagatorFactory
     * @param frame
     */
    public VectorAnalysis2(AbsoluteDate startDate, AbsoluteDate endDate,
            double timeStep, Satellite sat, PropagatorFactory propagatorFactory, Frame frame, CelestialBody body) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.frame = frame;
        this.body = body;
    }
    
    public abstract Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException;

    @Override
    public String getHeader() {
        return super.getHeader() + ",x,y,z"; //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getExtension() {
        return  "vec";
    } 

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException{
        Vector3D pos = null;
        try {
            pos = getVector(currentState, frame);
        } catch (OrekitException ex) {
            Logger.getLogger(VectorAnalysis2.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(pos == null){
            throw new IllegalStateException("Could not complete Vector Analysis");
        }
        String str = String.format("%f,%f,%f", pos.getX(),pos.getY(),pos.getZ());
        addRecord(new Record<>(currentState.getDate(),str));
    }
}
