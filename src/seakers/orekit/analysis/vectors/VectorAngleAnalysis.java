/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.vectors;

import seakers.orekit.analysis.Record;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This analysis will record how the angle [rad] between two vector changes in
 * the given frame of reference. The vectors must be accessible from the
 * spacecraft state
 *
 * @author nozomihitomi
 */
public abstract class VectorAngleAnalysis extends AbstractSpacecraftAnalysis<Double> {

    /**
     * The desired frame to use. Frame used to transform vectors into the same
     * frame.
     */
    private final Frame frame;

    /**
     * Constructor for obtaining the angle between the two vectors defined in
     * the specified frame. Both vectors must be obtainable from the spacecraft
     * state.
     *
     * @param startDate
     * @param endDate
     * @param timeStep
     * @param sat
     * @param propagatorFactory
     * @param frame
     */
    public VectorAngleAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            double timeStep, Satellite sat, PropagatorFactory propagatorFactory, Frame frame) {
        super(startDate, endDate, timeStep,sat, propagatorFactory);
        this.frame = frame;
    }

    public abstract Vector3D getVector1(SpacecraftState currentState, Frame frame);

    public abstract Vector3D getVector2(SpacecraftState currentState, Frame frame);

    @Override
    public String getHeader() {
        return super.getHeader() + ",angle[rad]"; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getExtension() {
        return "vecang";
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException{   
        Vector3D v1 = getVector1(currentState, frame);
        Vector3D v2 = getVector2(currentState, frame);
        addRecord(new Record<>(currentState.getDate(), Vector3D.angle(v1, v2)));
    }

}
