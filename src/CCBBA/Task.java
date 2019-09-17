package CCBBA;

import java.awt.*;
import java.util.Arrays;
import java.util.Vector;

public class Task {
    protected Dimension location;               // (x,y) Location of task
    protected double cost;                      // cost of executing task
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
    protected double gamma = 1.0;               // Proximity parameter

    /**
     * Task constructor
     * @param task_location location of the new task
     * @param task_cost cost of the task
     * @param task_sensors list of sensors required
     * @param time_constraints time constraint vector
     */
    public Task(Double s_max, Dimension task_location, double task_cost, Vector<String> task_sensors, Vector<Double> time_constraints){
        double inf = Double.POSITIVE_INFINITY;

        this.S_max = s_max;
        complete = false;
        location = task_location;
        cost = task_cost;
        req_sensors = task_sensors;
        TC = time_constraints;
        I = req_sensors.size();
        J = new Vector<>();
        K = new Vector<>();

        // Create subtask list from Sensor list
        // WARNING: Method set to only accommodate for I <= 2. Needs generalized function based on I
        if (I == 1) {
            Subtask subtask01 = new Subtask(req_sensors.get(0), 1, this, 1);
            J.add(subtask01);
            K.add(1);
        }
        else if(I == 2){
            Subtask subtask01 = new Subtask(req_sensors.get(0),1, this, 2);
            subtask01.addDep_task(req_sensors.get(1),2);
            Subtask subtask02 = new Subtask(req_sensors.get(1),2, this, 2);
            subtask02.addDep_task(req_sensors.get(0),1);
            Subtask subtask03 = new Subtask(req_sensors.get(0),1, this, 1);
            Subtask subtask04 = new Subtask(req_sensors.get(1),2, this, 1);

            J.add(subtask01);
            J.add(subtask02);
            J.add(subtask03);
            J.add(subtask04);

            K.add(2);
            K.add(2);
            K.add(1);
            K.add(1);
        }
        // *******
        N_sub = J.size();

        // Create T and D matrices
        D = new int[N_sub][N_sub];
        T = new double[N_sub][N_sub];

        for (int j = 0; j < N_sub; j++){
            Subtask temp_task1 = J.get(j);
            for (int q = j; q < N_sub; q++){
                Subtask temp_task2 = J.get(q);
                if (j == q){
                    // Subtask has no dependency with itself
                    D[j][q] = 0;
                    T[j][q] = inf;
                    T[q][j] = inf;
                }
                else if(temp_task2.getDep_nums().size() == 0){
                    // Subtask q has no dependent subtasks
                    D[j][q] = -1;
                    D[q][j] = -1;
                    T[j][q] = inf;
                    T[q][j] = inf;
                }
                else{
                    // Checks for dependency constraints
                    for (int i = 0; i < temp_task1.getDep_nums().size(); i++){
                        if (temp_task1.getDep_nums().get(i) == temp_task2.getMain_num()){
                            D[j][q] = 1;
                            D[q][j] = 1;
                            T[j][q] = TC.get(2);
                            T[q][j] = TC.get(2);
                        }
                        else{ T[j][q] = inf; T[q][j] = inf;}
                    }
                }
            }
        }


    }


    /**
     * Getters and Setters
     * @return returns values
     */
    public void setLocation(Dimension new_location){ location = new_location; }
    public Vector<String> getSensors(){return req_sensors; }
    public void setSensors(Vector<String> new_sensors){ req_sensors = new_sensors; }
    public void setTC(Vector<Double> new_TC){ TC = new_TC; }
    public void set_status(boolean status){ complete = status; }

    public boolean getStatus(){ return complete; }
    public int[][] getD(){ return D; }
    public double[][] getT(){ return T; }
    public Vector<Subtask> getJ(){ return J; }
    public double getCost(){ return cost; }
    public double getS_max(){ return S_max; }
    public double getI(){ return I; }
    public double getGamma(){ return gamma; }
    public Dimension getLocation(){ return this.location; }
    public Vector<Double> getTC(){
        //TC = {t_start, t_end, d, t_corr, lambda}
        return TC;
    }

    /*
    public static void main(String[] args){
        Dimension location1 = new Dimension(0,3);
        Vector<String> sensors1 = new Vector<>();
        sensors1.add("IR");
        sensors1.add("MW");

        Dimension location2 = new Dimension(4,3);
        Vector<String> sensors2 = new Vector<>();
        sensors2.add("MW");

        Vector<Double> TC = new Vector<Double>();
        TC.add(1.0);
        TC.add(2.0);
        TC.add(3.0);
        TC.add(4.0);

        Task TestTask1 = new Task(location1, 3, sensors1, TC);
        Task TestTask2 = new Task(location2, 3, sensors2, TC);
    }
    */
}