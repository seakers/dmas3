/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.communications.Receiver;
import seakers.orekit.object.communications.Transmitter;

/**
 * Detects events when satellite transmitter/receiver is compatible with ground
 * station transmitter/receiver and both can access each other
 *
 * @author nhitomi
 */
public class GroundStationDetector extends AbstractEventDetector<GroundStationDetector> {

    private static final long serialVersionUID = 8099883005724168100L;
    
    /**
     * the transmitter on board the spacecraft
     */
    private final Transmitter transmitter;
    
    /**
     * the receiver on board the spacecraft
     */
    private final Receiver receiver;
    
    /**
     * the target ground station
     */
    private final GndStation station;
    
    /**
     * Flag if the uplink from ground to spacecraft is compatible (ie. same communication band);
     */
    private final boolean uplinkCompatible;
    
    /**
     * Flag if the downlink from spacecraft to ground is compatible (ie. same communication band);
     */
    private final boolean downlinkCompatible;
    
    

    /**
     * Constructor for the detector. Must use a transmitter-receiver pair with
     * ground station. Max check for step size is set to 600.0 seconds by
     * default. Threshold for event detection is set to default 1e-6 seconds.
     * This detector by default stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param transmitter the transmitter on board the spacecraft
     * @param receiver the receiver on board the spacecraft
     * @param station the target ground station
     */
    public GroundStationDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate,
            Transmitter transmitter, Receiver receiver, GndStation station) {
        this(initialState, startDate, endDate, transmitter, receiver, station, 
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }

    /**
     * Constructor for the detector. transmitter-receiver pair with ground
     * station. Can set the resolution at which access time is computed. This
     * detector by default stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param transmitter the transmitter on board the spacecraft
     * @param receiver the receiver on board the spacecraft
     * @param station the target ground station
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public GroundStationDetector(SpacecraftState initialState, 
            AbsoluteDate startDate, AbsoluteDate endDate, 
            Transmitter transmitter, Receiver receiver, GndStation station, 
            double maxCheck, double threshold) {
        this(initialState, startDate, endDate, transmitter, receiver, station,
                maxCheck, threshold, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }

    /**
     * Constructor for the detector. transmitter-receiver pair with ground
     * station. Can set the resolution at which access time is computed
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param transmitter the transmitter on board the spacecraft
     * @param receiver the receiver on board the spacecraft
     * @param station the target ground station
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     */
    public GroundStationDetector(SpacecraftState initialState, 
            AbsoluteDate startDate, AbsoluteDate endDate, 
            Transmitter transmitter, Receiver receiver, GndStation station, 
            EventHandler.Action action, double maxCheck, double threshold) {
        this(initialState, startDate, endDate, transmitter, receiver, station, 
                maxCheck, threshold, DEFAULT_MAX_ITER, action);
    }
    
    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param pvTarget Position/velocity provider of the considered target
     * @param instrument the instrument that will observe the target
     */
    private GroundStationDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate,
             Transmitter transmitter, Receiver receiver, GndStation station, 
            final double maxCheck, final double threshold, final int maxIter, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);

        this.transmitter = transmitter;
        this.receiver = receiver;
        this.station = station;
        this.downlinkCompatible = transmitter.compatible(station.getReceiver());
        this.uplinkCompatible = receiver.compatible(station.getTransmitter());
    }

    @Override
    protected GroundStationDetector create(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, 
            EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new GroundStationDetector(initialState, startDate, endDate,
                transmitter, receiver, station, maxCheck, threshold, maxIter, action);
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        if(downlinkCompatible && uplinkCompatible){
            double elevation = station.getBaseFrame().getElevation(
                    s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
            return elevation - station.getMinEl();
        }else{
            return -1.;
    }
    }

}
