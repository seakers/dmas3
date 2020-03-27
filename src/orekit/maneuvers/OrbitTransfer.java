/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.maneuvers;

import java.io.Serializable;
import java.util.Date;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.event.detector.ApogeeDetectorHohmannTransfer;

/**
 *
 * @author paugarciabuzzi
 */
public class OrbitTransfer implements Serializable{

    private static final long serialVersionUID = 1655810305698925450L;
    
    private final Propagator prop;
    
    private final AbsoluteDate startDate;
    
    private final AbsoluteDate endDate;
    
    private final double isp;

    public OrbitTransfer(Propagator prop, AbsoluteDate startDate, AbsoluteDate endDate, double isp) {
        this.prop = prop;
        this.startDate=startDate;
        this.endDate=endDate;
        this.isp=isp;
    }

    public Propagator getProp() {
        return prop;
    }
    
    /**
     *
     * @param prop
     * @param date date in which the orbit transfer starts
     * @param inc increment of altitude
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    public Propagator HohmannTransfer(double inc, AbsoluteDate date) throws OrekitException{
        if (date.compareTo(startDate)<0){
            throw new IllegalArgumentException("Initial date of orbit transfer is earlier than the scenario start date");
        }else if(date.compareTo(endDate)>0){
            throw new IllegalArgumentException("Initial date of orbit transfer is later than the scenario end date");
        }
        DateDetector trigger1 = new DateDetector(date);
        double aL=prop.getInitialState().getA();
        double aH=aL+inc;
        double aT=0.5*(aL+aH);
        double deltaV1=Math.sqrt(Constants.WGS84_EARTH_MU)*(Math.sqrt(2/aL-1/aT)-Math.sqrt(1/aL));
        double deltaV2=Math.sqrt(Constants.WGS84_EARTH_MU)*(Math.sqrt(1/aH)-Math.sqrt(2/aH-1/aT));
        Vector3D deltaVVector1=new Vector3D(deltaV1,0,0);
        Vector3D deltaVVector2=new Vector3D(deltaV2,0,0);
        ApogeeDetectorHohmannTransfer trigger2=new ApogeeDetectorHohmannTransfer(prop.getInitialState().getOrbit(),date);
        AttitudeProvider attitudeOverride = new LofOffset(prop.getFrame(), LOFType.TNW);
        ImpulseManeuver<DateDetector> maneuver1 = new ImpulseManeuver<>(trigger1,attitudeOverride,deltaVVector1,isp);
        
        ImpulseManeuver<ApogeeDetectorHohmannTransfer> maneuver2 = new ImpulseManeuver<>(trigger2,attitudeOverride,deltaVVector2,isp);
        
        prop.addEventDetector(maneuver1);
        prop.addEventDetector(maneuver2);
        return prop;
    }
}
