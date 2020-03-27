/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractAnalysis;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.analysis.Record;
import seakers.orekit.maneuvers.OrbitTransfer;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 *
 * @author paugarciabuzzi
 */
public class HohmannTransferAnalysis extends AbstractSpacecraftAnalysis<OrbitalElements> {
    
    /** Start date of the Hohmann Orbit Transfer. */
    private final AbsoluteDate dateTransfer;
    
    /** Altitude increment. */
    private final double inc;
    
    /** Specific Impulse. */
    private final double isp;
    
    public HohmannTransferAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, 
                   Satellite sat, PropagatorFactory propagatorFactory, AbsoluteDate dateTransfer, 
                   double inc, double isp) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.dateTransfer=dateTransfer;
        this.inc=inc;
        this.isp=isp;
    }

    @Override
    public String getHeader() {
        return super.getHeader()+",semimajor axis[deg],ecc.,inc.[deg],raan[deg],arg. per.[deg],mean anom.[deg]"; 
    }

    @Override
    public String getExtension() {
        return "eph";
    }

    /**
     * At each step, the ephemeris of the satellite is recorded
     *
     * @param currentState
     */
    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) {
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
        Record<OrbitalElements> e = new Record<>(currentState.getDate(), new OrbitalElements(o.getA(), o.getE(), o.getI(),
                o.getRightAscensionOfAscendingNode(), o.getPerigeeArgument(),
                o.getMeanAnomaly()));
        addRecord(e);
    }

    @Override
    public String getName() {
        return String.format("%s_%s","eph",getSatellite().getName());
    }
    
    @Override
    public AbstractAnalysis<OrbitalElements> call() throws Exception {
        Propagator prop = propagatorFactory.createPropagator(getSatellite().getOrbit(), getSatellite().getGrossMass());
        prop.setSlaveMode();
        OrbitTransfer ot= new OrbitTransfer(prop, getStartDate(), getEndDate(),isp);
        prop=ot.HohmannTransfer(inc, dateTransfer);

        for (AbsoluteDate extrapDate = getStartDate();
                extrapDate.compareTo(getEndDate()) <= 0;
                extrapDate = extrapDate.shiftedBy(getTimeStep())) {
            handleStep(prop.propagate(extrapDate),true);
        }
        
        return this;
    }
}