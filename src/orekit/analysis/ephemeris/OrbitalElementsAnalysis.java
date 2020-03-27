/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import org.orekit.errors.OrekitException;
import seakers.orekit.analysis.Record;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This class will record the orbital parameters of a satellite over the course
 * of the whole scenario
 *
 * @author nozomihitomi
 */
public class OrbitalElementsAnalysis extends AbstractSpacecraftAnalysis<OrbitalElements> {
    
    private final PositionAngle type;
   
    /**
     * 
     * @param startDate start date of the analysis
     * @param endDate end date of the analysis
     * @param timeStep the time step at which to record the orbital elements
     * @param sat the satellite of interest
     * @param type the type of anomaly to record (True or Mean)
     * @param propagatorFactory the propagator factory that will create the appropriate propagator for the satellite of interest
     */
    public OrbitalElementsAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PositionAngle type, PropagatorFactory propagatorFactory) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.type = type;
    }

    @Override
    public String getHeader() {
        return super.getHeader()+",semimajor axis[deg],ecc.,inc.[deg],raan[deg],arg. per.[deg]," + type.toString() + " anom.[deg]"; 
    }

    @Override
    public String getExtension() {
        return "eph";
    }

    @Override
    public String getName() {
        return String.format("%s_%s","eph",getSatellite().getName());
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
        Record<OrbitalElements> e = new Record<>(currentState.getDate(), new OrbitalElements(o.getA(), o.getE(), o.getI(),
                o.getRightAscensionOfAscendingNode(), o.getPerigeeArgument(),
                o.getAnomaly(type)));
        addRecord(e);
    }

}
