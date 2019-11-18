package CCBBA.bin;

import java.awt.*;
import java.util.Vector;

public class Task {
    double inf = Double.POSITIVE_INFINITY;
    protected Dimension location;               // (x,y) Location of task
    protected double cost_constant;             // constant cost of executing task
    protected double cost_proportion;           // cost of executing task proportional to agent resources
    protected Vector<Subtask> J;                // List of Subtasks
    protected Vector<Integer> K;                // Level of partiality
    protected Vector<Double> TC;                // Time constraints
    protected Vector<String> req_sensors;       // List of required sensors
    protected int N_sub;                        // Number of subtasks
    protected int I;                            // Number of sensor needed
    protected boolean complete;                 // Status of task
    protected double[][] T;                     // Max allowable time of arrival between tasks j and q
    protected int[][] D;                        // Dependency Matrix
    protected double S_max;                     // Maximum score
    protected double gamma = Double.NEGATIVE_INFINITY;  // Proximity parameter

    /**
     * Task constructor
     * @param task_location location of the new task
     * @param task_cost cost of the task
     * @param task_sensors list of sensors required
     * @param time_constraints time constraint vector
     */
    public Task(Double s_max, Dimension task_location,String cost_type, double task_cost, Vector<String> task_sensors, Vector<Double> time_constraints){
        this.S_max = s_max;
        this.complete = false;
        this.location = task_location;
        this.req_sensors = task_sensors;
        this.TC = time_constraints;
        this.I = req_sensors.size();
        this.J = new Vector<>();
        this.K = new Vector<>();

        if(cost_type.equals("CONSTANT")){
            this.cost_constant = task_cost;
            this.cost_proportion = 0.0;
        }
        else if(cost_type.equals("PROPORTIONAL")){
            this.cost_constant = 0.0;
            this.cost_proportion = task_cost;
        }
        else{
            this.cost_constant = task_cost;
            this.cost_proportion = 0.0;
        }

        // Create subtask list from Sensor list
        for(int i = 0; i < this.req_sensors.size(); i++) {
            Vector<String> remainingSensors = new Vector<>();
            String mainSensor = this.req_sensors.get(i);

            // create vector of dependent sensors
            for(int j = 0; j < this.req_sensors.size(); j++){
                if(i != j){ remainingSensors.add(this.req_sensors.get(j)); }
            }

            // create combinations of dependencies
            Vector<Vector<String>> combinations = getCombinations( remainingSensors );

            for(int j = 0; j < combinations.size(); j++){
                Vector<String> depTasks = combinations.get(j);
                Subtask mainSubtask = new Subtask(mainSensor, i+1, this, (depTasks.size()+1) );
                for(int k = 0; k < depTasks.size(); k++){
                    if(depTasks.get(k).length() > 0) {
                        mainSubtask.addDep_task(depTasks.get(k), this.req_sensors.indexOf(depTasks.get(k)) + 1);
                    }
                }
                this.J.add(mainSubtask);
                this.K.add( (depTasks.size()+1) );
            }
        }
        N_sub = J.size();

        // Create T and D matrices
        D = new int[N_sub][N_sub];
        T = new double[N_sub][N_sub];

        for (int j = 0; j < N_sub; j++){
            Subtask temp_task1 = J.get(j);
            for (int q = 0; q < N_sub; q++){
                Subtask temp_task2 = J.get(q);
                if (j == q){
                    // Subtask has no dependency with itself
                    D[j][q] = 0;
                    T[j][q] = inf;
                }
                else if(temp_task1.getDep_nums().size() == 0){
                    // Subtask j has no dependent subtasks
                    D[j][q] = -1;
                    T[j][q] = inf;
                }
                else if(temp_task2.getDep_nums().size() == 0){
                    // Subtask q has no dependent subtasks
                    D[j][q] = -1;
                    T[j][q] = inf;
                }
                else{
                    // Checks for dependency constraints
                    for (int i = 0; i < temp_task1.getDep_nums().size(); i++){
                        if (temp_task1.getDep_nums().get(i) == temp_task2.getMain_num()){
                            D[j][q] = 1;
                            T[j][q] = TC.get(3);
                        }
                        else{
                            T[j][q] = inf;
                        }
                    }
                }
            }
        }


    }

    private Vector<Vector<String>> getCombinations(Vector<String> remainingSensors){
        Vector<Vector<String>> combinations = new Vector<>();

        for(int i = 0; i < (int) Math.pow(2, remainingSensors.size()); i++ ){
            String bitRepresentation = Integer.toBinaryString(i);
            Vector<String> tempSet = new Vector<>();

            for(int j = 0; j < bitRepresentation.length(); j++){
                int delta = remainingSensors.size() - bitRepresentation.length();

                if (bitRepresentation.charAt(j) == '1') {
                    tempSet.add(remainingSensors.get(j + delta));
                }
            }

            combinations.add(tempSet);
        }
        return combinations;
    }

    public void setSubtaskComplete(Subtask j) {
        // set subtask to complete
        int i_j = this.J.indexOf(j);
        Subtask tempTask = this.J.get(i_j);
        tempTask.complete();
        this.J.setElementAt(tempTask, i_j);

        // if this task is done, no other mutually exclusive tasks are allowed to be bid on
        for (int i = 0; i < this.J.size(); i++) {
            if (this.D[i_j][i] == -1) {
                Subtask exclusiveTemp = this.J.get(i);
                exclusiveTemp.complete();
                this.J.setElementAt(exclusiveTemp, i);
            }
        }

//        // if all dependent subtasks are complete, mark task all as complete
//        boolean req = true;
//        for (int i = 0; i < this.J.size(); i++) {
//            if ((this.D[i_j][i] >= 1) && (!this.J.get(i).getComplete())) {
//                req = false;
//            }
//        }
//
//        if (req) {
//            for (int i = 0; i < this.J.size(); i++) {
//                this.J.get(i).complete();
//            }
//            this.complete = true;
//        }
    }

    /**
     * Getters and Setters
     * @return returns values
     */
    public void setLocation(Dimension new_location){ location = new_location; }
    public void setSensors(Vector<String> new_sensors){ req_sensors = new_sensors; }
    public void setTC(Vector<Double> new_TC){ TC = new_TC; }
    public void set_status(boolean status){ complete = status; }

    public Vector<String> getSensors(){return req_sensors; }
    public Dimension getLocation(){ return this.location; }
    public boolean getStatus(){ return complete; }
    public Vector<Subtask> getJ(){ return J; }
    public double[][] getT(){ return T; }
    public int[][] getD(){ return D; }
    public double getCostConst(){ return this.cost_constant; }
    public double getCostProp(){ return this.cost_proportion; }
    public double getS_max(){ return S_max; }
    public double getI(){ return I; }
    public double getGamma(){ return gamma; }
    public Vector<Double> getTC(){
        //TC = {t_start, t_end, d, t_corr, lambda}
        return TC;
    }
    public double getT_d(){ return TC.get(2); }
    public double getLambda(){ return TC.get(4); }
}