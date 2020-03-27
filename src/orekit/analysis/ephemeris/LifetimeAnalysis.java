/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import org.orekit.bodies.BodyShape;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * Lifetime Analysis using master mode and an altitude detector. It records the orbital elements at every
 * analysis time step.
 * @author paugarciabuzzi
 */
public class LifetimeAnalysis extends OrbitalElementsAnalysis{
   
    private static final double thresholdAltitude=150000;
    
    private final BodyShape bodyshape;
    
    private double lifetime;

    public LifetimeAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PositionAngle type, PropagatorFactory propagatorFactory, BodyShape bodyshape) {
        super(startDate, endDate, timeStep, sat, type,propagatorFactory);
        this.bodyshape=bodyshape;
        this.lifetime=0;
    }

    @Override
    public String getName() {
        return String.format("lifetime_%s_%s","eph",getSatellite().getName());
    }
    
    @Override
    public OrbitalElementsAnalysis call() throws Exception {
        Propagator prop = propagatorFactory.createPropagator(this.getSatellite().getOrbit(), this.getSatellite().getGrossMass());
        final EventHandler<? super AltitudeDetector> handler=new StopOnEvent<>();
        AltitudeDetector detector= new AltitudeDetector(this.getTimeStep(),thresholdAltitude,bodyshape)
                                                .withHandler(handler);

        prop.addEventDetector(detector);
        prop.setMasterMode(this.getTimeStep(), this);
        SpacecraftState s=prop.propagate(getStartDate(), getEndDate());
        this.lifetime=s.getDate().durationFrom(getStartDate())/60/60/24/365.25;
        return this;
    }
    
    public double getLifetime(){
        return this.lifetime;
    }
}
