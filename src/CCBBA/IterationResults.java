package CCBBA;

import java.awt.*;
import java.util.Vector;

public class IterationResults {
    // Info used with other agents*********************
    private Vector<Subtask> J = new Vector<>();                     // available task list
    private Vector<Double> y = new Vector<>();                      // winner bid list
    private Vector<SimulatedAbstractAgent> z = new Vector<>();      // winner agent list
    private Vector<Double> tz = new Vector<>();                     // arrival time list
    // *********************************************
    // Info used with self**************************
    private Vector<Double> c = new Vector<>();                      // self bid list
    private Vector<Integer> s = new Vector<>();                     // iteration stamp list
    private Vector<Integer> v = new Vector<>();                     // number of iterations in constraint violation
    private Vector<Integer> w_solo = new Vector<>();                // permission to bid solo
    private Vector<Integer> w_any = new Vector<>();                 // permission to bid any
    private Vector<Integer> h = new Vector<>();                     // availability checks vector
    // *********************************************
    private Vector<Vector<SimulatedAbstractAgent>> omega = new Vector<>();// coalition mates
    private Vector<Subtask> bundle = new Vector<>();
    private Vector<Subtask> path = new Vector<>();
    private Vector<Dimension> xpath = new Vector<>();
    private int M = 0;

    public IterationResults(Vector<Subtask> J, int w_solo_max, int w_any_max, int M){
        int size = J.size();
        this.J = J;
        y.setSize(size);
        z.setSize(size);
        tz.setSize(size);

        c.setSize(size);
        s.setSize(size);
        v.setSize(size);
        w_solo.setSize(size);
        w_any.setSize(size);
        h.setSize(size);
        omega.setSize(M);
        this.M = M;

        for(int i = 0; i < size; i ++){
            y.setElementAt(0.0, i);
            z.setElementAt(null, i);
            tz.setElementAt(0.0, i);

            c.setElementAt(0.0, i);
            s.setElementAt(0, i);
            v.setElementAt(0, i);
            w_solo.setElementAt(w_solo_max, i);
            w_any.setElementAt(w_any_max, i);
            h.setElementAt(1, i);
        }
    }

    public IterationResults(IterationResults prevResults, boolean omega_toggle){
        // Copies results from previous iteration
        this.J = new Vector<>();
        this.y = new Vector<>();
        this.z = new Vector<>();
        this.tz = new Vector<>();
        this.c = new Vector<>();
        this.s = new Vector<>();
        this.v = new Vector<>();
        this.w_solo = new Vector<>();
        this.w_any = new Vector<>();
        this.omega = new Vector<>();
        this.bundle = new Vector<>();
        this.path = new Vector<>();

        for(int i = 0; i < prevResults.getY().size(); i++){
            this.J.add(prevResults.getJ().get(i));
            this.y.add(prevResults.getY().get(i));
            this.z.add(prevResults.getZ().get(i));
            this.tz.add(prevResults.getTz().get(i));
            this.c.add(prevResults.getC().get(i));
            this.s.add(prevResults.getS().get(i));
            this.v.add(prevResults.getV().get(i));
            this.w_solo.add(prevResults.getW_solo().get(i));
            this.w_any.add(prevResults.getW_any().get(i));
            this.h.add(1);
            //bundle.setElementAt(prevResults.getBundle().get(i), i);
            //path.setElementAt(prevResults.getPath().get(i), i);
        }

        this.M = prevResults.getM();

        for(int i = 0; i < prevResults.getBundle().size(); i++){
            this.bundle.add( prevResults.getBundle().get(i) );
            this.path.add( prevResults.getPath().get(i) );
        }

        if(omega_toggle) {
            for (int i = 0; i < this.M; i++) {
                Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();
                if ((this.bundle.size() >= i + 1) && (this.bundle.size() > 0)) {
                    for (int i_j = 0; i_j < this.J.size(); i_j++) {
                        int i_bundle = this.J.indexOf(this.bundle.get(i));
                        if ((this.z.get(i_j) != this.z.get(i_bundle)) && (this.z.get(i_j) != null) && (this.bundle.get(i).getParentTask() == this.J.get(i_j).getParentTask())) {
                            tempCoal.add(this.z.get(i_j));
                        }
                    }
                }
                this.omega.add(tempCoal);
            }
        }
        else{
            for (int i = 0; i < this.M; i++) {
                Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();
                for (int i_j = 0; i_j < prevResults.getOmega().get(i).size(); i_j++) {
                    tempCoal.add(prevResults.getOmega().get(i).get(i_j));
                }
                this.omega.add(tempCoal);
            }
        }
    }

    public void updateResults(SubtaskBid maxBid, int i_max, SimulatedAbstractAgent agent, int zeta){
        if(y.get(i_max) < maxBid.getC()){
            this.y.setElementAt(maxBid.getC() ,i_max);
            this.z.setElementAt(agent, i_max);
            this.c.setElementAt(maxBid.getC() ,i_max);
            this.s.setElementAt(zeta, i_max);
            this.tz.setElementAt(maxBid.getTStart(), i_max);

            this.bundle = new Vector<>();
            this.path = new Vector<>();
            for(int i = 0; i < agent.getBundle().size(); i++){
                this.bundle.add( agent.getBundle().get(i) );
                this.path.add( agent.getPath().get(i) );
            }
        }


        Vector<Vector<SimulatedAbstractAgent>> newOmega = new Vector<>();
        for(int i = 0; i < this.M; i++) {
            Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();
            if((this.bundle.size() >= i+1)&&(this.bundle.size() > 0)) {
                for (int i_j = 0; i_j < this.J.size(); i_j++) {
                    if ((this.z.get(i_j) != agent) && (this.z.get(i_j) != null) && (this.bundle.get(i).getParentTask() == this.J.get(i_j).getParentTask())) {
                        tempCoal.add(this.z.get(i_j));
                    }
                }
            }
            newOmega.add(tempCoal);
        }

        this.omega = newOmega;

    }

    public void updateResults(IterationResults receivedResults, int i, Vector<Subtask> bundle){
        double yReceived = receivedResults.getY().get(i);
        SimulatedAbstractAgent zReceived = receivedResults.getZ().get(i);
        double tzReceived = receivedResults.getTz().get(i);

        this.y.setElementAt(yReceived, i);
        this.z.setElementAt(zReceived, i);
        this.tz.setElementAt(tzReceived, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = J.indexOf(bundle.get(i_b));

                this.y.setElementAt(0.0, i_j);
                this.z.setElementAt(null, i_j);
                this.tz.setElementAt(0.0, i_j);
            }
        }
    }

    public void updateResults(Vector<Subtask> newBundle, Vector<Subtask> newPath, Vector<Dimension> newX_path){
        this.bundle = new Vector<>();
        this.path = new Vector<>();
        this.xpath = new Vector<>();

        for(int i = 0; i< newBundle.size(); i++){
            this.bundle.add(newBundle.get(i));
            this.path.add(newPath.get(i));
            this.xpath.add(newX_path.get(i));
        }
    }

    public void resetResults(IterationResults receivedResults, int i, Vector<Subtask> bundle){
        this.y.setElementAt(0.0, i);
        this.z.setElementAt(null, i);
        this.tz.setElementAt(0.0, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = J.indexOf(bundle.get(i_b));

                this.y.setElementAt(0.0, i_j);
                this.z.setElementAt(null, i_j);
                this.tz.setElementAt(0.0, i_j);
            }
        }
    }

    public void leaveResults(IterationResults receivedResults, int i){

    }

    public void updateOmega(){

    }

    /**
     * Getters and Setters
     */
    public Vector<Subtask> getJ(){ return this.J; }
    public Vector<Double> getY(){ return this.y; }
    public Vector<SimulatedAbstractAgent> getZ(){ return this.z; }
    public Vector<Double> getTz(){ return this.tz; }
    public Vector<Double> getC(){ return this.c; }
    public Vector<Integer> getS(){ return this.s; }
    public Vector<Integer> getV(){ return this.v; }
    public Vector<Integer> getW_solo(){ return this.w_solo; }
    public Vector<Integer> getW_any(){ return this.w_any; }
    public Vector<Integer> getH(){ return this.h; }
    public Vector<Vector<SimulatedAbstractAgent>> getOmega(){ return this.omega; }
    public Vector<Subtask> getBundle(){ return this.bundle; }
    public Vector<Subtask> getPath(){ return this.path; }
    public Integer getM(){ return this.M; }

    public void setY(Vector<Double> y_new){ this.y = y_new; }
    public void setZ(Vector<SimulatedAbstractAgent> z_new){ this.z = z_new; }
    public void setTz(Vector<Double> tz_new){this. tz = tz_new; }
    public void setC(Vector<Double> c_new){ this.c = c_new; }
    public void setS(Vector<Integer> s_new){ this.s = s_new; }
    public void setV(Vector<Integer> v_new){ this.v = v_new; }
    public void setW_solo(Vector<Integer> w_solo_new){ this.w_solo = w_solo_new; }
    public void setW_any(Vector<Integer> w_any_new){ this.w_any = w_any_new; }
    public void setH(Vector<Integer> y_new){ this.h = y_new; }

}
