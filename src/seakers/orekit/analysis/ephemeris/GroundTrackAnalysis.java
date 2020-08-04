/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.analysis.Record;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 *
 * @author nozomihitomi
 */
public class GroundTrackAnalysis extends AbstractSpacecraftAnalysis<String> {

    /**
     * The shape of the earth
     */
    private final BodyShape body;

    /**
     *
     * @param startDate start date of the analysis
     * @param endDate end date of the analysis
     * @param timeStep the time step at which to record the orbital elements
     * @param sat the satellite of interest
     * @param body shape of the earth
     * @param propagatorFactory the propagator factory that will create the
     * appropriate propagator for the satellite of interest
     */
    public GroundTrackAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, BodyShape body, PropagatorFactory propagatorFactory) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.body = body;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + ",lat[deg],lon[deg]";
    }

    @Override
    public String getExtension() {
        return "gdt";
    }
    
    public void handleStep(SpacecraftState currentState, boolean isLast) {
        try {
            GeodeticPoint pt = body.transform(
                    currentState.getPVCoordinates().getPosition(),
                    currentState.getFrame(),
                    currentState.getDate());
            
            String str = String.format("%f,%f", 
                    FastMath.toDegrees(pt.getLatitude()),
                    FastMath.toDegrees(pt.getLongitude()));
            addRecord(new Record<>(currentState.getDate(),str));
        } catch (OrekitException ex) {
            Logger.getLogger(GroundTrackAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getName() {
        return String.format("%s_%s", "gdt", getSatellite().getName());
    }
}
