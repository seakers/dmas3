/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.vectors;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.DTM2000InputParameters;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.MarshallSolarActivityFutureEstimation;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.ICGEM_FILENAME;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * An analysis for recording the position of a satellite that changes its
 * drag area while in eclipse or in sunlight.
 * 
 * @author paugarciabuzzi
 */
public class VectorAnalisysEclipseSunlightDiffDrag extends VectorAnalysis{
   
    private final double eclipseDragArea;
    
    private final double sunlightDragArea;
    
    private final double solarArea;
    
    private final double mass;

    public VectorAnalisysEclipseSunlightDiffDrag(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PropagatorFactory propagatorFactory, Frame frame,
                                                double eclipseDragArea,double sunlightDragArea,double solarArea, double mass) {
        super(startDate, endDate, timeStep, sat, propagatorFactory, frame);
        this.eclipseDragArea=eclipseDragArea;
        this.sunlightDragArea=sunlightDragArea;
        this.solarArea=solarArea;
        this.mass=mass;
    }

    @Override
    public Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException {
        return currentState.getPVCoordinates(frame).getPosition();
    }

    @Override
    public String getName() {
        return String.format("%s_%s","DiffEclipseSunlightVec",getSatellite().getName());
    }
    
    @Override
    public VectorAnalisysEclipseSunlightDiffDrag call() throws Exception {
        Propagator prop = propagatorFactory.createPropagator(super.getSatellite().getOrbit(), mass);
        prop.setSlaveMode();
        
        //Set stepsizes and threshold for detectors
        double StepSize = super.getSatellite().getOrbit().getKeplerianPeriod()/1000;
        double threshold = 1e-3;
        final PVCoordinatesProvider occulted=CelestialBodyFactory.getSun();
        double occultedRadius=Constants.SUN_RADIUS;
        final PVCoordinatesProvider occulting=CelestialBodyFactory.getEarth();
        double occultingRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final EventHandler<? super EclipseDetector> handler=new StopOnEvent<>();
        EclipseDetector detector= new EclipseDetector(StepSize,threshold,
                                                occulted,occultedRadius,
                                                occulting,occultingRadius)
                                                .withHandler(handler);

        prop.addEventDetector(detector);
        boolean end=false;
        SpacecraftState s=prop.getInitialState();
        handleStep(s, false);
        /*Flag that captures when the satellite changes from sunlight to eclipse
        and viceversa*/
        boolean flag=true;
        while (!end){
            prop.resetInitialState(s);
            ((NumericalPropagator) prop).removeForceModels();
            if (flag){
                if (detector.g(prop.getInitialState())<0){
                    prop=setNumericalPropagator(prop, eclipseDragArea, solarArea);
                    flag=false;
                }else{
                    prop=setNumericalPropagator(prop, sunlightDragArea, solarArea);
                    flag=false;
                }
            }
            AbsoluteDate date0=s.getDate();
            s =prop.propagate(s.getDate(), s.getDate().shiftedBy(getTimeStep()));
            handleStep(s, false);
            if (s.getDate().durationFrom(date0)<getTimeStep()){
                flag=true;
            }
            if(s.getDate().compareTo(getEndDate())>0){
                end=true;
            }
        }
        return this;
    }
    
    public Propagator setNumericalPropagator(Propagator prop, double dragArea, double solarArea) throws OrekitException{
        //Frames and Bodies creation (must use IERS_2003 and EME2000 frames to be consistent with STK)
        final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Add the gravity model with Harmonics
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        ICGEMFormatReader reader = new ICGEMFormatReader(ICGEM_FILENAME, false);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        final NormalizedSphericalHarmonicsProvider harmonicsProvider = GravityFieldFactory.getNormalizedProvider(21, 21);
        ((NumericalPropagator)prop).addForceModel(new HolmesFeatherstoneAttractionModel(earthFrame, harmonicsProvider));
        
        //Add the drag model (DTM2000 model)
        double dragCoeff = 2.2;

        String supportedNames = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)";
        MarshallSolarActivityFutureEstimation.StrengthLevel strengthlevel = MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE;
        DTM2000InputParameters parameters = new MarshallSolarActivityFutureEstimation(supportedNames, strengthlevel);
        Atmosphere atmosphere = new DTM2000(parameters, CelestialBodyFactory.getSun(), earth);
        DragSensitive spacecraft = new IsotropicDrag(dragArea, dragCoeff);
        ((NumericalPropagator)prop).addForceModel(new DragForce(atmosphere, spacecraft));
        
        ((NumericalPropagator)prop).addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        
        ((NumericalPropagator)prop).addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));


        //Add the solar radiation pressure model
        double equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double cr = 1;
        RadiationSensitive spacecraft1 = new IsotropicRadiationSingleCoefficient(solarArea, cr);
        ((NumericalPropagator)prop).addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), equatorialRadius, spacecraft1));
            
        return prop;
    }
    
    
}
