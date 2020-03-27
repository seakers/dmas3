/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Finder for apogee crossing events for a HohmannTransfer Orbit.
 * This class finds apside crossing events (i.e. apogee or perigee crossing).
 * The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at perigee crossing and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at apogee crossing. However, we will only stop IF the apogee detection is AFTER 
 * the date of the first firing.
 * Most of the code is a replique of the ApsideDetector of the orekit source code.
 * @author paugarciabuzzi
 */

public class ApogeeDetectorHohmannTransfer extends AbstractDetector<ApogeeDetectorHohmannTransfer> {

    /** Serializable UID. */
    private static final long serialVersionUID = 4267430340250440792L;
    
    /** Date of the first impulse maneuver. */
    private AbsoluteDate date;
    
    /** Counter of apogee crossing events after the date of the first impulse maneuver. */
    private int counter;


    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3 and to set the convergence
     * threshold according to orbit size</p>
     * @param orbit initial orbit
     */
    public ApogeeDetectorHohmannTransfer(final Orbit orbit, AbsoluteDate date) {
        this(1.0e-13 * orbit.getKeplerianPeriod(), orbit);
        this.date=date;
        this.counter=0;
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     */
    public ApogeeDetectorHohmannTransfer(final double threshold, final Orbit orbit) {
        super(orbit.getKeplerianPeriod() / 3, threshold,
              DEFAULT_MAX_ITER, new StopOnDecreasing<ApogeeDetectorHohmannTransfer>());
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @since 6.1
     */
    private ApogeeDetectorHohmannTransfer(final double maxCheck, final double threshold,
                           final int maxIter, final EventHandler<? super ApogeeDetectorHohmannTransfer> handler) {
        super(maxCheck, threshold, maxIter, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected ApogeeDetectorHohmannTransfer create(final double newMaxCheck, final double newThreshold,
                                    final int newMaxIter, final EventHandler<? super ApogeeDetectorHohmannTransfer> newHandler) {
        return new ApogeeDetectorHohmannTransfer(newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** Compute the value of the switching function.
     * This function computes the dot product of the 2 vectors : position.velocity.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final PVCoordinates pv = s.getPVCoordinates();
        return Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }
    
    
    /** Only method that changes from the regular ApsideDetector. Propagation only can stop after the date
     * of the first firing so that the propagation does not stop when we detect an apside in the initial orbit.
     * We only want to detect the apside when we are in the transfer eliptic orbit. Also, after detecting the 
     * apogee (where we fire the second burst), we are no longer detecting more apside's crossings.
     * @param s
     * @param increasing
     * @return
     * @throws OrekitException 
     */
    @Override
    public EventHandler.Action eventOccurred(final SpacecraftState s, final boolean increasing) throws OrekitException {
        @SuppressWarnings("unchecked")
        final EventHandler.Action whatNext;
        if(s.getDate().compareTo(date)>0 && counter==0){
            whatNext = getHandler().eventOccurred(s, this, increasing);
            counter=counter+1;
        }else{
            whatNext = Action.CONTINUE;
        }
        
        return whatNext;
    }

}