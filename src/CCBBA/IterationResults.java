package CCBBA;

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
    private Vector<Vector<SimulatedAbstractAgent>> omega = new Vector<>();// coalition mates
    // *********************************************
    //private Vector<Subtask> bundle = new Vector<>();
    //private Vector<Subtask> path = new Vector<>();

    public IterationResults(Vector<Subtask> J, int O_kq){
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


        for(int i = 0; i < size; i ++){
            y.setElementAt(0.0, i);
            z.setElementAt(null, i);
            tz.setElementAt(0.0, i);

            c.setElementAt(0.0, i);
            s.setElementAt(0, i);
            v.setElementAt(0, i);
            w_solo.setElementAt(O_kq, i);
            w_any.setElementAt(O_kq, i);
            h.setElementAt(1, i);
        }
    }

    public IterationResults(IterationResults prevResults){
        // Copies results from previous iteration
        y = new Vector<>();
        z = new Vector<>();
        tz = new Vector<>();
        c = new Vector<>();
        s = new Vector<>();
        v = new Vector<>();
        w_solo = new Vector<>();
        w_any = new Vector<>();
        //bundle = new Vector<>();
        //path = new Vector<>();

        for(int i = 0; i < prevResults.getY().size(); i++){
            this.y.add(prevResults.getY().get(i));
            this.z.add(prevResults.getZ().get(i));
            this.tz.add(prevResults.getTz().get(i));
            this.c.add(prevResults.getC().get(i));
            this.s.add(prevResults.getS().get(i));
            this.v.add(prevResults.getV().get(i));
            this.w_solo.add(prevResults.getW_solo().get(i));
            this.w_any.add(prevResults.getW_any().get(i));
            this.h.add(prevResults.getH().get(i));
            //bundle.setElementAt(prevResults.getBundle().get(i), i);
            //path.setElementAt(prevResults.getPath().get(i), i);
        }
    }

    public void updateResults(SubtaskBid maxBid, int i_max, SimulatedAbstractAgent agent, int zeta){
        if(y.get(i_max) < maxBid.getC()){
            y.setElementAt(maxBid.getC() ,i_max);
            z.setElementAt(agent, i_max);
            s.setElementAt(zeta, i_max);
            tz.setElementAt(maxBid.getTStart(), i_max);
        }
    }

    public void updateResults(IterationResults receivedResults, int i, Vector<Subtask> bundle){
        double yReceived = receivedResults.getY().get(i);
        SimulatedAbstractAgent zReceived = receivedResults.getZ().get(i);

        this.y.setElementAt(yReceived, i);
        this.z.setElementAt(zReceived, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = J.indexOf(bundle.get(i_b));

                this.y.setElementAt(0.0, i_j);
                this.z.setElementAt(null, i_j);
            }
        }
    }

    public void resetResults(IterationResults receivedResults, int i, Vector<Subtask> bundle){
        this.y.setElementAt(0.0, i);
        this.z.setElementAt(null, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = J.indexOf(bundle.get(i_b));

                this.y.setElementAt(0.0, i_j);
                this.z.setElementAt(null, i_j);
            }
        }
    }

    public void leaveResults(IterationResults receivedResults, int i){

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
    //public Vector<Subtask> getBundle(){ return this.bundle; }
    //public Vector<Subtask> getPath(){ return this.path; }

    public void setY(Vector<Double> y_new){ this.y = y_new; }
    public void setZ(Vector<SimulatedAbstractAgent> y_new){ this.z = y_new; }
    public void setTz(Vector<Double> y_new){this. tz = y_new; }
    public void setC(Vector<Double> y_new){ this.c = y_new; }
    public void setS(Vector<Integer> y_new){ this.s = y_new; }
    public void setV(Vector<Integer> y_new){ this.v = y_new; }
    public void setW_solo(Vector<Integer> y_new){ this.w_solo = y_new; }
    public void setW_any(Vector<Integer> y_new){ this.w_any = y_new; }
    public void setH(Vector<Integer> y_new){ this.h = y_new; }

}
