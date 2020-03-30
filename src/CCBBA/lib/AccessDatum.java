package CCBBA.lib;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.HashMap;
import java.util.Locale;

public class AccessDatum {
    private Vector3D position;
    private Vector3D velocity;
    private Vector3D acceleration;
    private HashMap<Subtask, Boolean> access;

    public AccessDatum(PVCoordinates pvSat, IterationResults localResults){
        position = pvSat.getPosition();
        velocity = pvSat.getVelocity();
        acceleration = pvSat.getAcceleration();

        access = new HashMap<>();
        for(IterationDatum datum : localResults.getResults()){
            access.put(datum.getJ(), false);
        }
    }

    public String toString(){
        String p = String.format("%f\t%f\t%f\t", position.getX(), position.getY(), position.getZ() );
        String v = String.format("%f\t%f\t%f\t", velocity.getX(), velocity.getY(), velocity.getZ() );
        String a = String.format("%f\t%f\t%f\t", acceleration.getX(), acceleration.getY(), acceleration.getZ() );

        var ref = new Object() {
            String accs = "";
        };
        access.forEach( (j, status) ->{
            if(status){
                ref.accs += String.format("%s_%s-%d\t", j.getParentTask().getName(), j.getName(), 1);
            }
            else{
                ref.accs += String.format("%s_%s-%d\t", j.getParentTask().getName(), j.getName(), 0);
            }
        });

        // print to text
        return String.format(Locale.US, "%s\t%s\t%s\t%s\n", p, v, a, ref.accs);
    }

    public Vector3D getPosition() { return position; }
    public Vector3D getVelocity() { return velocity; }
    public Vector3D getAcceleration() { return acceleration; }
    public Boolean getAccess(Subtask j) { return access.get(j); }

    public void setPosition(Vector3D position) { this.position = position; }
    public void setVelocity(Vector3D velocity) { this.velocity = velocity; }
    public void setAcceleration(Vector3D acceleration) { this.acceleration = acceleration; }
    public void setAccess(Subtask j, Boolean status){        access.replace(j, status); }
}
