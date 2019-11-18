package CCBBA.bin;

import java.awt.*;
import java.util.Vector;

public class IterationLists {
    // Info used with other agents*********************
    private Vector<Subtask> J;                                      // available task list
    private Vector<Double> y = new Vector<>();                      // winner bid list
    private Vector<AbstractSimulatedAgent> z = new Vector<>();      // winner agent list
    private Vector<Double> tz = new Vector<>();                     // arrival time list
    // *********************************************
    // Info used with self**************************
    private AbstractSimulatedAgent parentAgent;                     //
    private Vector<Double> c = new Vector<>();                      // self bid list
    private Vector<Integer> s = new Vector<>();                     // iteration stamp list
    private Vector<Integer> v = new Vector<>();                     // number of iterations in constraint violation
    private Vector<Double> cost = new Vector<>();                   // cost vector
    private Vector<Double> score = new Vector<>();                  // score vector
    private Vector<Integer> w_solo = new Vector<>();                // permission to bid solo
    private Vector<Integer> w_any = new Vector<>();                 // permission to bid any
    private Vector<Integer> h = new Vector<>();                     // availability checks vector
    private Vector<Vector<AbstractSimulatedAgent>> omega = new Vector<>();// coalition mates
    private Vector<Vector<AbstractSimulatedAgent>> overallOmega = new Vector<>(); // overall coalition mates
    private Vector<Subtask> bundle = new Vector<>();                // current bundle
    private Vector<Subtask> overallBundle = new Vector<>();         // overall bundle
    private Vector<Subtask> path = new Vector<>();                  // current path order
    private Vector<Subtask> overallPath = new Vector<>();           // overall path order
    private Vector<Dimension> xpath = new Vector<>();               // current path coordinates
    // Fixed quantities ****************************
    private int M;                                                  // planning horizon
    private double C_merge;                                         // merge cost
    private double C_split;                                         // split cost
    private double resources;


    public IterationLists(Vector<Subtask> J, int w_solo_max, int w_any_max, int M, double C_merge, double C_split, double resources, AbstractSimulatedAgent parentAgent){
        // initialize all values
        this.parentAgent = parentAgent;
        int size = J.size();
        this.J = J;
        this.y.setSize(size);
        this.z.setSize(size);
        this.tz.setSize(size);

        this.c.setSize(size);
        this.s.setSize(size);
        this.v.setSize(size);
        this.w_solo.setSize(size);
        this.w_any.setSize(size);
        this.h.setSize(size);
        this.omega.setSize(M);
        this.cost.setSize(size);
        this.score.setSize(size);

        this.M = M;
        this.C_merge = C_merge;
        this.C_split = C_split;
        this.resources = resources;

        for(int i = 0; i < size; i ++){
            this.y.setElementAt(0.0, i);
            this.z.setElementAt(null, i);
            this.tz.setElementAt(0.0, i);

            this.c.setElementAt(0.0, i);
            this.s.setElementAt(0, i);
            this.v.setElementAt(0, i);
            this.w_solo.setElementAt(w_solo_max, i);
            this.w_any.setElementAt(w_any_max, i);
            this.h.setElementAt(1, i);
            this.cost.setElementAt(0.0, i);
            this.score.setElementAt(0.0, i);
        }
    }

    IterationLists(IterationLists prevResults, boolean omega_toggle, AbstractSimulatedAgent parentAgent){
        // Copies results from previous iteration
        this.parentAgent = parentAgent;
        this.J = new Vector<>();
        this.y = new Vector<>();
        this.z = new Vector<>();
        this.tz = new Vector<>();
        this.c = new Vector<>();
        this.s = new Vector<>();
        this.v = new Vector<>();
        this.w_solo = new Vector<>();
        this.w_any = new Vector<>();
        this.h = new Vector<>();
        this.omega = new Vector<>();
        this.overallOmega = new Vector<>();
        this.bundle = new Vector<>();
        this.overallBundle = new Vector<>();
        this.path = new Vector<>();
        this.overallPath = new Vector<>();
        this.cost = new Vector<>();
        this.score = new Vector<>();

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
            this.cost.add(prevResults.getCost().get(i));
            this.score.add(prevResults.getScore().get(i));
        }

        this.M = prevResults.getM();
        this.C_merge = prevResults.getC_merge();
        this.C_split = prevResults.getC_split();
        this.resources = prevResults.getResources();

        for(int i = 0; i < prevResults.getBundle().size(); i++){
            this.bundle.add( prevResults.getBundle().get(i) );
            this.path.add( prevResults.getPath().get(i) );
        }

        this.overallBundle.addAll( prevResults.getOverallBundle() );
        this.overallPath.addAll( prevResults.getOverallPath() );
        this.overallOmega.addAll( prevResults.getOverallOmega() );

        if(omega_toggle) { // update coalitions
            updateOmega(parentAgent);
        }
        else{ // copy previous iteration's coalitions
            copyOmega(prevResults);
        }
    }

    void updateResults(SubtaskBid maxBid, int i_max, AbstractSimulatedAgent agent, int zeta){
        // updates results with highest bid
        if(y.get(i_max) < maxBid.getC()){
            this.y.setElementAt(maxBid.getC() ,i_max);
            this.z.setElementAt(agent, i_max);
            this.c.setElementAt(maxBid.getC() ,i_max);
            this.s.setElementAt(zeta, i_max);
            this.tz.setElementAt(maxBid.getTStart(), i_max);
            this.cost.setElementAt(maxBid.getCost_aj(), i_max);
            this.score.setElementAt(maxBid.getScore(), i_max);

            this.bundle = new Vector<>();
            this.path = new Vector<>();
            for(int i = 0; i < agent.getBundle().size(); i++){
                this.bundle.add( agent.getBundle().get(i) );
                this.path.add( agent.getPath().get(i) );
            }
        }

        updateOmega(parentAgent);
    }

    void updateResults(){
        for(int i = 0; i < this.bundle.size(); i++){
            Subtask j = this.bundle .get(i);
            Subtask j_p = this.path.get(i);
            this.overallBundle.add(j);
            this.overallPath.add(j_p);
            this.overallOmega.add(this.omega.get(i));
        }

        this.bundle = new Vector<>();
        this.path = new Vector<>();
    }

    public void updateResults(IterationLists receivedResults, int i, Vector<Subtask> bundle){
        double yReceived = receivedResults.getY().get(i);
        AbstractSimulatedAgent zReceived = receivedResults.getZ().get(i);
        double tzReceived = receivedResults.getTz().get(i);
        double costReceived = receivedResults.getCost().get(i);
        double scoreReceived = receivedResults.getScore().get(i);
        int timeStampReceived = receivedResults.getS().get(i);
        int violationIterations = receivedResults.getV().get(i);

        this.y.setElementAt(yReceived, i);
        this.z.setElementAt(zReceived, i);
        this.tz.setElementAt(tzReceived, i);
        this.cost.setElementAt(costReceived, i);
        this.score.setElementAt(scoreReceived, i);
        this.s.setElementAt(timeStampReceived, i);
        this.v.setElementAt(violationIterations, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(this.J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = this.J.indexOf(bundle.get(i_b));

                if(i_j != i) {
                    this.y.setElementAt(0.0, i_j);
                    this.z.setElementAt(null, i_j);
                    this.tz.setElementAt(0.0, i_j);
                    this.cost.setElementAt(0.0, i_j);
                    this.score.setElementAt(0.0, i_j);
                    this.s.setElementAt(0, i_j);
                    this.v.setElementAt(0, i_j);
                }
            }
        }
    }

    public void updateResults(Vector<Subtask> newBundle, Vector<Subtask> newPath, Vector<Dimension> newX_path){
        // updates bundle and path
        this.bundle = new Vector<>();
        this.path = new Vector<>();

        for(int i = 0; i< newBundle.size(); i++){
            this.bundle.add(newBundle.get(i));
            this.path.add(newPath.get(i));
        }
    }

    public void resetResults(int i, Vector<Subtask> bundle){
        // sets results to zero
        this.y.setElementAt(0.0, i);
        this.z.setElementAt(null, i);
        this.tz.setElementAt(0.0, i);
        this.cost.setElementAt(0.0, i);
        this.score.setElementAt(0.0, i);
        this.s.setElementAt(0, i);
        this.v.setElementAt(0, i);

        //if task is in bundle, then reset subsequent scores
        if(bundle.contains(J.get(i))){
            for(int i_b = bundle.indexOf( J.get(i) ); i_b < bundle.size(); i_b++){
                int i_j = J.indexOf(bundle.get(i_b));

                this.y.setElementAt(0.0, i_j);
                this.z.setElementAt(null, i_j);
                this.tz.setElementAt(0.0, i_j);
                this.cost.setElementAt(0.0, i_j);
                this.score.setElementAt(0.0, i_j);
                this.s.setElementAt(0, i_j);
                this.v.setElementAt(0, i_j);

                this.omega.setElementAt(new Vector<>(), i_b);
            }
        }
    }

    public void leaveResults(IterationLists receivedResults, int i){

    }

    private void updateOmega(AbstractSimulatedAgent agent){
        Vector<Vector<AbstractSimulatedAgent>> newOmega = new Vector<>();
        for(int i = 0; i < this.M; i++) {
            Vector<AbstractSimulatedAgent> tempCoal = new Vector<>();

            if( this.bundle.size() >= i+1 ) {
                for (int i_j = 0; i_j < this.J.size(); i_j++) {
                    if ((this.z.get(i_j) != agent)
                            && (this.z.get(i_j) != null)
                            && (this.bundle.get(i).getParentTask() == this.J.get(i_j).getParentTask())) {
                        tempCoal.add(this.z.get(i_j));
                    }
                }
            }
            newOmega.add(tempCoal);
        }

        this.omega = newOmega;
    }

    private void copyOmega(IterationLists prevResults){
        for (int i = 0; i < this.M; i++) {
            Vector<AbstractSimulatedAgent> tempCoal = new Vector<>();
            tempCoal.addAll(prevResults.getOmega().get(i));
            this.omega.add(tempCoal);
        }
    }

    /**
     * Getters and Setters
     */
    public Vector<Subtask> getJ(){ return this.J; }
    public Vector<Double> getY(){ return this.y; }
    public Vector<AbstractSimulatedAgent> getZ(){ return this.z; }
    public Vector<Double> getTz(){ return this.tz; }
    public Vector<Double> getC(){ return this.c; }
    public Vector<Integer> getS(){ return this.s; }
    public Vector<Integer> getV(){ return this.v; }
    public Vector<Integer> getW_solo(){ return this.w_solo; }
    public Vector<Integer> getW_any(){ return this.w_any; }
    public Vector<Integer> getH(){ return this.h; }
    public Vector<Vector<AbstractSimulatedAgent>> getOmega(){ return this.omega; }
    public Vector<Vector<AbstractSimulatedAgent>> getOverallOmega(){ return this.overallOmega; }
    public Vector<Subtask> getBundle(){ return this.bundle; }
    public Vector<Subtask> getOverallBundle(){ return this.overallBundle; }
    public Vector<Subtask> getPath(){ return this.path; }
    public Vector<Subtask> getOverallPath(){ return this.overallPath; }
    public Integer getM(){ return this.M; }
    public Double getC_merge(){ return this.C_merge; }
    public Double getC_split(){ return this.C_split; }
    public double getResources(){ return this.resources; }
    public Vector<Double> getCost(){ return this.cost; }
    public Vector<Double> getScore(){ return this.score; }
    public AbstractSimulatedAgent getParentAgent(){ return this.parentAgent; }

    public void setY(Vector<Double> y_new){ this.y = y_new; }
    public void setZ(Vector<AbstractSimulatedAgent> z_new){ this.z = z_new; }
    public void setTz(Vector<Double> tz_new){this. tz = tz_new; }
    public void setC(Vector<Double> c_new){ this.c = c_new; }
    public void setS(Vector<Integer> s_new){ this.s = s_new; }
    public void setV(Vector<Integer> v_new){
        this.v = new Vector<>();
        this.v.setSize(v_new.size());
        for(int i = 0; i < v_new.size(); i++){
            this.v.setElementAt( v_new.get(i), i);
        }
    }
    public void setW_solo(Vector<Integer> w_solo_new){
        this.w_solo = new Vector<>();
        this.w_solo.setSize(w_solo_new.size());
        for(int i = 0; i < w_solo_new.size(); i++){
            this.w_solo.setElementAt( w_solo_new.get(i), i);
        }
    }
    public void setW_any(Vector<Integer> w_any_new){
        this.w_any = new Vector<>();
        this.w_any.setSize(w_any_new.size());
        for(int i = 0; i < w_any_new.size(); i++){
            this.w_any.setElementAt( w_any_new.get(i) , i);
        }
    }
    public void setH(Vector<Integer> y_new){ this.h = y_new; }

}

