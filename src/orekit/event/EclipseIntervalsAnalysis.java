/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EclipseDetector;
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

/**Analysis that allows us to obtain the eclipse intervals of a given satellite.
 *
 * @author paugarciabuzzi
 */
public class EclipseIntervalsAnalysis extends AbstractEventAnalysis{
    
    private final TimeIntervalArray timearray;
    
    private final Satellite sat;
    
    protected final PropagatorFactory pf;

    public EclipseIntervalsAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, Frame inertialFrame, Satellite sat, PropagatorFactory pf) {
        super(startDate, endDate, inertialFrame);
        this.sat=sat;
        this.pf=pf;
        this.timearray=this.getEmptyTimeArray();
    }

    @Override
    public EventAnalysis call() throws Exception {
        Propagator prop = pf.createPropagator(sat.getOrbit(), sat.getGrossMass());
        double detStepSize = sat.getOrbit().getKeplerianPeriod() / 1000.;
        double threshold = 1e-3;

        //need to reset initial state of the propagators or will progate from the last stop time
        prop.resetInitialState(prop.getInitialState());
        prop.clearEventsDetectors();
        
        final PVCoordinatesProvider occulted=CelestialBodyFactory.getSun();
        double occultedRadius=Constants.SUN_RADIUS;
        final PVCoordinatesProvider occulting=CelestialBodyFactory.getEarth();
        double occultingRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final EventHandler<? super EclipseDetector> handler=new StopOnEvent<>();
        EclipseDetector detector= new EclipseDetector(detStepSize,threshold,
                                                occulted,occultedRadius,
                                                occulting,occultingRadius)
                                                .withHandler(handler);
        prop.addEventDetector(detector);
        
        boolean end=false;
        SpacecraftState s=prop.getInitialState();
        while (!end){
            prop.resetInitialState(s);
            if (detector.g(prop.getInitialState())<0){
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
        sb.append("Eclipse Event Analysis\n");
        sb.append(String.format("\t\tSatellite %s\n", sat.toString()));
        return sb.toString();
    }
    
}
