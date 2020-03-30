/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This event analysis is used to compute the intervals when a given satellite is in
 * an elevation to a ground point equal or higher than a given minimum elevation. 
 * To do: implement this class for multiple satellites and multiple groundstations.
 * @author paugarciabuzzi
 */
public class ElevationIntervalsAnalysis extends AbstractEventAnalysis{
    
    private final TimeIntervalArray timearray;
    
    private final Satellite sat;
    
    private final TopocentricFrame groundpoint;
    
    private final double minElevation;
    
    protected final PropagatorFactory pf;

    public ElevationIntervalsAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, Frame inertialFrame, 
            Satellite sat, PropagatorFactory pf, TopocentricFrame groundpoint, double minElevation) {
        super(startDate, endDate, inertialFrame);
        this.sat=sat;
        this.pf=pf;
        this.timearray=this.getEmptyTimeArray();
        this.groundpoint=groundpoint;
        this.minElevation=minElevation;
    }

    @Override
    public EventAnalysis call() throws Exception {
        Propagator prop = pf.createPropagator(sat.getOrbit(), sat.getGrossMass());
        double detStepSize = sat.getOrbit().getKeplerianPeriod() / 1000.;
        double threshold = 1e-3;

        //need to reset initial state of the propagators or will progate from the last stop time
        prop.resetInitialState(prop.getInitialState());
        prop.clearEventsDetectors();
        
        final EventHandler<? super ElevationDetector> handler=new StopOnEvent<>();
        
        ElevationDetector detector = new ElevationDetector(detStepSize, threshold,
                this.groundpoint).withConstantElevation(minElevation).withHandler(handler);
        prop.addEventDetector(detector);
         
        boolean end=false;
        SpacecraftState s=prop.getInitialState();
        while (!end){
            prop.resetInitialState(s);
            if (detector.g(prop.getInitialState())>0){
                this.timearray.addRiseTime(s.getDate());
            }else{
                this.timearray.addSetTime(s.getDate());
            }
            s =prop.propagate(s.getDate(), this.getEndDate());
            if(s.getDate().equals(this.getEndDate())){
                end=true;
            }
        }
        return this;
    }
    
    public TimeIntervalArray getTimeIntervalArray(){
        return this.timearray;
    }
    
    
    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Comms(Elevation) Event Analysis\n");
        sb.append(String.format("\t\tSatellite %s\n", sat.toString()));
        return sb.toString();
    }
    
}
