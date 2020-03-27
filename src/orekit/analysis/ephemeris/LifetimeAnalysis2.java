/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * * Lifetime Analysis using slave mode and without altitude detectors. It does not record the orbital
 * elements and it is only useful to get the lifetime value.
 * @author paugarciabuzzi
 */
public class LifetimeAnalysis2 extends OrbitalElementsAnalysis{
   
    private static final double thresholdAltitude=150000;
    
    private final BodyShape bodyShape;
    
    private double lifetime;

    public LifetimeAnalysis2(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PositionAngle type, PropagatorFactory propagatorFactory, BodyShape bodyshape) {
        super(startDate, endDate, timeStep, sat, type,propagatorFactory);
        this.bodyShape=bodyshape;
        this.lifetime=0;
    }

    @Override
    public String getName() {
        return String.format("lifetime_%s_%s","eph",getSatellite().getName());
    }
    
    @Override
    public OrbitalElementsAnalysis call() throws Exception {
        Propagator prop = propagatorFactory.createPropagator(this.getSatellite().getOrbit(), this.getSatellite().getGrossMass());
        prop.setSlaveMode();
        AbsoluteDate targetDate=getStartDate();
        SpacecraftState s=prop.getInitialState();
        while (getEndDate().durationFrom(targetDate)>0){
            targetDate=targetDate.shiftedBy(getTimeStep());
            s=prop.propagate(targetDate);
            prop.resetInitialState(s);
            Frame bodyFrame      = bodyShape.getBodyFrame();
            PVCoordinates pvBody = s.getPVCoordinates(bodyFrame);
            GeodeticPoint point  = bodyShape.transform(pvBody.getPosition(),
                                                         bodyFrame, s.getDate());
            if(point.getAltitude()<thresholdAltitude){
                break;
            }
        }
        this.lifetime=s.getDate().durationFrom(getStartDate())/60/60/24/365.25;
        return this;
    }
    
    public double getLifetime(){
        return this.lifetime;
    }
}
