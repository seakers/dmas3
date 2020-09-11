package modules.spacecraft.component;

import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.maneuvers.Maneuver;
import modules.spacecraft.maneuvers.SlewingManeuver;
import modules.spacecraft.orbits.SpacecraftOrbit;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class ADCS extends Component{
    private ArrayList<Vector3D> bodyFrame;                              // list of vectors that determine the attitude frame wrt orbit frame
    private HashMap<Instrument, Vector3D> pointingVectorsBod;           // pointing vectors of each sensor wrt body frame
    private ArrayList<Maneuver> maneuverHistory;

    public ADCS(ArrayList<Instrument> payload, SpacecraftOrbit orbit) throws Exception {
        super(0,0);
        // initialize history
        maneuverHistory = new ArrayList<>();

        // calculate pointing vectors
        this.bodyFrame = initialzeBodyFrame();
        this.pointingVectorsBod = calcPointingVectorsBod(payload);

        // calculate mass and power of subsystem
        this.mass = calcADCSMass();
        this.power = calcADCSPower();
    }

    public void doManeuver(Maneuver maneuver) throws Exception {
        if(maneuver.getClass().equals(SlewingManeuver.class)){
            SlewingManeuver slew = (SlewingManeuver) maneuver;
            AbsoluteDate maneuverStart = maneuver.getStartDate();
            AbsoluteDate maneuverEnd = maneuver.getEndDate();

            double th = slew.getTh();

            Vector3D x_bod = new Vector3D(1,0,0);
            Vector3D z_bod = new Vector3D(0,Math.cos(th),Math.sin(th));
            Vector3D y_bod =new Vector3D(0,-Math.sin(th),Math.cos(th));

            ArrayList<Vector3D> bodyFrame = new ArrayList<>();
            int x = 1;
        }
        else{
            throw new Exception("Attitude Maneuver not yet supported");
        }
    }


    private ArrayList<Vector3D> initialzeBodyFrame(){
        Vector3D x_bod = new Vector3D(1,0,0);
        Vector3D y_bod = new Vector3D(0,1,0);
        Vector3D z_bod =new Vector3D(0,0,1);

        ArrayList<Vector3D> bodyFrame = new ArrayList<>();

        bodyFrame.add(x_bod);
        bodyFrame.add(y_bod);
        bodyFrame.add(z_bod);

        return bodyFrame;
    }

    public ArrayList<Vector3D> calcOrbitFrame(SpacecraftOrbit orbit, AbsoluteDate startDate) throws Exception {
        // returns 3 vectors representing the frame of the orbital direction
        // x points towards velocity, z towards the ground, and y to the right of x
        Vector3D x_bod = orbit.getPVEarth(startDate).getVelocity().normalize();
        Vector3D z_bod = orbit.getPVEarth(startDate).getPosition().scalarMultiply(-1).normalize();
        Vector3D y_bod = z_bod.crossProduct(x_bod);

        ArrayList<Vector3D> orbitFrame = new ArrayList<>();
        orbitFrame.add(x_bod);
        orbitFrame.add(y_bod);
        orbitFrame.add(z_bod);

        if(x_bod.getNorm() > 1+1e-3 || y_bod.getNorm() > 1+1e-3 || z_bod.getNorm() > 1+1e-3 ){
            throw new Exception("orbital frame calculation gives non-unit vectors");
        }

        return orbitFrame;
    }

    private HashMap<Instrument, Vector3D> calcPointingVectorsBod(ArrayList<Instrument> payload){
        HashMap<Instrument, Vector3D> pointingVectors = new HashMap<>();

        for(Instrument ins : payload){
            double lookAngle = deg2rad(-ins.getLookAngle());
            Vector3D pointVector = new Vector3D(0, -Math.sin(lookAngle), Math.cos(lookAngle));
            pointingVectors.put(ins, pointVector);
        }

        return pointingVectors;
    }

    public Vector3D getPointingVector(Instrument ins, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception{
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);
        Vector3D pointBod = pointingVectorsBod.get(ins);
        Vector3D pointOrb = transform(pointBod, this.bodyFrame);
        Vector3D pointEarth = transform(pointOrb, orbitFrame);

        return pointEarth;
    }

    public Vector3D getPointingVector(Instrument ins, ArrayList<Vector3D> bodyFrame, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception{
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);
        Vector3D pointBod = pointingVectorsBod.get(ins);
        Vector3D pointOrb = transform(pointBod, bodyFrame);
        Vector3D pointEarth = transform(pointOrb, orbitFrame);

        return pointEarth;
    }

    private Vector3D transform(Vector3D v, ArrayList<Vector3D> targetFrame){
        Vector3D x_tar = targetFrame.get(0);
        Vector3D y_tar = targetFrame.get(1);
        Vector3D z_tar = targetFrame.get(2);

        return x_tar.scalarMultiply(v.getX()).add( y_tar.scalarMultiply(v.getY()) ).add( z_tar.scalarMultiply(v.getZ()) );
    }

    public Vector3D getPointingWithSlew(double th, Instrument ins, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception{
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);

        // th in rads
        double lookAngle = deg2rad( -ins.getLookAngle() );
        Vector3D pointBod = new Vector3D(0, -Math.sin(lookAngle + th), Math.cos(lookAngle + th));
        Vector3D pointEarth = transform(pointBod, orbitFrame);

        return pointEarth;
    }

    public void updateBodyFrame(double slewAngle){
        // slew angle in rad
        ArrayList<Vector3D> bodyOld = copyFrame(this.bodyFrame);
        Vector3D x_new = bodyOld.get(0);
        Vector3D y_new = bodyOld.get(1).scalarMultiply(Math.cos(slewAngle)).add(bodyOld.get(2).scalarMultiply(Math.sin(slewAngle)));
        Vector3D z_new = x_new.crossProduct(y_new);

        this.bodyFrame.set(0,x_new);
        this.bodyFrame.set(1,y_new);
        this.bodyFrame.set(2,z_new);
    }

    private ArrayList<Vector3D> copyFrame(ArrayList<Vector3D> original){
        ArrayList<Vector3D> copy = new ArrayList<>();
        for(Vector3D v : original){
            double x_i = v.getX();
            double y_i = v.getY();
            double z_i = v.getZ();
            Vector3D a_i = new Vector3D(x_i, y_i, z_i);
            copy.add(a_i);
        }

        return copy;
    }

    public boolean isVisible(Instrument ins, Vector3D pointEarth, Vector3D objectPos, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception {
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);
        String fovType = ins.getFovType();
        if(fovType.equals("square") || fovType.equals("circular")){
            double fov = deg2rad( ins.getFOV() );
            double scanningAngleMinus = deg2rad( ins.getScanAngleMinus() );
            double scanningAnglePlus = deg2rad( ins.getScanAnglePlus() );

            double ATangleTask = getTaskATAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double CTAngleTask = getTaskCTAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double ATAnglePoint = getPointATAngle(orbitFrame, pointEarth);
            double CTAnglePoint = getPointCTAngle(orbitFrame, pointEarth);

            if(ins.getScanningType().equals("side")){
                if(Math.abs(ATangleTask - ATAnglePoint) > fov/2.0) return false;
                else return (Math.abs(CTAngleTask - CTAnglePoint) <= (fov/2.0 + scanningAnglePlus + 1e-3));
            }
            else{
                throw new Exception("Scanning type not yet supported");
            }

        }
        else{
            throw new Exception("FOV type not yet supported");
        }
    }

    public boolean isVisible(Instrument ins, ArrayList<Vector3D> bodyFrame, SpacecraftOrbit orbit, AbsoluteDate date, Vector3D objectPos) throws Exception {
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);
        Vector3D pointBod = pointingVectorsBod.get(ins);
        Vector3D pointOrb = transform(pointBod, bodyFrame);
        Vector3D pointEarth = transform(pointOrb, orbitFrame);

        String fovType = ins.getFovType();
        if(fovType.equals("square") || fovType.equals("circular")){
            double fov = deg2rad( ins.getFOV() );
            double scanningAngleMinus = deg2rad( ins.getScanAngleMinus() );
            double scanningAnglePlus = deg2rad( ins.getScanAnglePlus() );

            double ATangleTask = getTaskATAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double CTAngleTask = getTaskCTAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double ATAnglePoint = getPointATAngle(orbitFrame, pointEarth);
            double CTAnglePoint = getPointCTAngle(orbitFrame, pointEarth);

            if(ins.getScanningType().equals("side")){
                if(Math.abs(ATangleTask - ATAnglePoint) > fov/2.0) return false;
                else return (Math.abs(CTAngleTask - CTAnglePoint) <= (fov/2.0 + scanningAnglePlus));
            }
            else{
                throw new Exception("Scanning type not yet supported");
            }

        }
        else{
            throw new Exception("FOV type not yet supported");
        }
    }

    public double calcSlewAngleReq(Instrument ins, ArrayList<Vector3D> bodyFrame, SpacecraftOrbit orbit,
                                   AbsoluteDate date, Vector3D objectPos) throws Exception{
        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(orbit,date);
        Vector3D pointBod = pointingVectorsBod.get(ins);
        Vector3D pointOrb = transform(pointBod, bodyFrame);
        Vector3D pointEarth = transform(pointOrb, orbitFrame);

        String fovType = ins.getFovType();
        double fov = deg2rad( ins.getFOV() );

        if(fovType.equals("square") || fovType.equals("circular")){
            double scanningAngleMinus = deg2rad( ins.getScanAngleMinus() );
            double scanningAnglePlus = deg2rad( ins.getScanAnglePlus() );

            double ATangleTask = getTaskATAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double CTAngleTask = getTaskCTAngle(orbitFrame, orbit.getPVEarth(date).getPosition(), objectPos);
            double ATAnglePoint = getPointATAngle(orbitFrame, pointEarth);
            double CTAnglePoint = getPointCTAngle(orbitFrame, pointEarth);

            if(ins.getScanningType().equals("side")){
                // since slew can only move CT, if object is not visible AT then no maneuver is possible
                if(Math.abs(ATangleTask - ATAnglePoint) > fov/2.0) {
                    return Double.POSITIVE_INFINITY;
                }
                else {
                    double maneuver;
                    if(CTAngleTask < CTAnglePoint){
                        maneuver = CTAngleTask - (CTAnglePoint - fov/2.0 - scanningAngleMinus);
                    }
                    else{
                        maneuver = CTAngleTask - (CTAnglePoint + fov/2.0 + scanningAnglePlus);
                    }

                    double maneuverDeg = rad2deg(maneuver);
                    return maneuver;
                }
            }
            else{
                throw new Exception("Scanning type not yet supported");
            }
        }
        else{
            throw new Exception("FOV type not yet supported");
        }
    }

    private double getPointATAngle(ArrayList<Vector3D> orbitFrame, Vector3D pointVec){
        // declare unit vectors wrt satellite
        Vector3D satX = orbitFrame.get(0);
        Vector3D satY = orbitFrame.get(1);
        Vector3D satZ = orbitFrame.get(2);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( pointVec.dotProduct(satX) )
                .add( satZ.scalarMultiply( pointVec.dotProduct(satZ) ) ).normalize();

        return Math.acos( relProj.dotProduct(satZ) / ( relProj.getNorm() * satZ.getNorm() ) );
    }

    private double getPointCTAngle(ArrayList<Vector3D> orbitFrame, Vector3D pointVec){
        // declare unit vectors wrt satellite
        Vector3D satX = orbitFrame.get(0);
        Vector3D satY = orbitFrame.get(1);
        Vector3D satZ = orbitFrame.get(2);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( pointVec.dotProduct(satX) )
                .add( satZ.scalarMultiply( pointVec.dotProduct(satZ) ) ).normalize();

        return Math.acos( relProj.dotProduct(pointVec) / ( relProj.getNorm() * pointVec.getNorm() ) );
    }

    private double getTaskATAngle(ArrayList<Vector3D> orbitFrame, Vector3D satPos, Vector3D taskPos){
        // declare unit vectors wrt satellite
        Vector3D satX = orbitFrame.get(0);
        Vector3D satY = orbitFrame.get(1);
        Vector3D satZ = orbitFrame.get(2);

        // calculate task position relative to satellite
        Vector3D taskRelSat = taskPos.subtract(satPos);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( taskRelSat.dotProduct(satX) )
                .add( satZ.scalarMultiply( taskRelSat.dotProduct(satZ) ) ).normalize();

        return Math.acos( relProj.dotProduct(satZ) / ( relProj.getNorm() * satZ.getNorm() ) );
    }

    public double getTaskCTAngle(ArrayList<Vector3D> orbitFrame, Vector3D satPos, Vector3D taskPos){
        // declare unit vectors wrt satellite
        Vector3D satX = orbitFrame.get(0);
        Vector3D satY = orbitFrame.get(1);
        Vector3D satZ = orbitFrame.get(2);

        // calculate task position relative to satellite
        Vector3D taskRelSat = taskPos.subtract(satPos);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( taskRelSat.dotProduct(satX) )
                .add( satZ.scalarMultiply( taskRelSat.dotProduct(satZ) ) ).normalize();

        return Math.acos( relProj.dotProduct(taskRelSat) / ( relProj.getNorm() * taskRelSat.getNorm() ) );
    }

    public double getTaskCTAngle(){return 1.0;}

    private double calcADCSPower(){
        return 1.0;
    }
    private double calcADCSMass(){
        return -1.0;
    }
    private double deg2rad(double th){
        return th*Math.PI/180;
    }
    private double rad2deg(double th){
        return  th*180/Math.PI;
    }

    @Override
    Component copy() {
        return new ADCS(this.power, this.mass, this.bodyFrame, this.pointingVectorsBod);
    }
    private ADCS(double power, double mass, ArrayList<Vector3D> bodyFrame, HashMap<Instrument, Vector3D> pointingVectors){
        super(power, mass);
        this.bodyFrame = new ArrayList<>(bodyFrame);
        this.pointingVectorsBod = new HashMap<>(pointingVectors);
    }

    public ArrayList<Vector3D> getBodyFrame(){return this.bodyFrame;}
    public double getPower(){return this.power;}
}
