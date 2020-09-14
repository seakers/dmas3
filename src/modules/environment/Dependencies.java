package modules.environment;

import java.util.ArrayList;

public class Dependencies {
    private int[][] D;                          // Coalition Dependency Matrix
    private double[][] T_min;                   // Minimum Coalition Correlation Time Matrix
    private double[][] T_max;                   // Maximum Coalition Correlation Time Matrix

    public Dependencies(int N_j, ArrayList<Subtask> subtasks, Requirements requirements){
        this.D = new int[N_j][N_j];
        this.T_max = new double[N_j][N_j];
        this.T_min = new double[N_j][N_j];

        for(int j = 0; j < N_j; j++){
            Subtask J = subtasks.get(j);
            for(int q = 0; q < N_j; q++){
                Subtask Q = subtasks.get(q);

                if(j == q){
                    // Subtask has no dependency with itself
                    D[j][q] = 0;
                    T_min[j][q] = 0.0;
                    T_max[j][q] = Double.POSITIVE_INFINITY;
                }
                else if(J.getDepMeasurements().size() == 0){
                    // Subtask J has no dependent subtasks
                    D[j][q] = -1;
                    T_min[j][q] = 0.0;
                    T_max[j][q] = Double.POSITIVE_INFINITY;
                }
                else if(Q.getDepMeasurements().size() == 0){
                    // Subtask Q has no dependent subtasks
                    D[j][q] = -1;
                    T_min[j][q] = 0.0;
                    T_max[j][q] = Double.POSITIVE_INFINITY;
                }
                else{
                    // Checks for dependency constraints
                    boolean constraintFound = false;
                    if(J.getDepMeasurements().contains(Q.getMainMeasurement())
                            && Q.getDepMeasurements().contains(J.getMainMeasurement())
                            && J.getDepMeasurements().size()==Q.getDepMeasurements().size()){

                        D[j][q] = 1;
                        if(J.getMainMeasurement().getBand().equals(Q.getMainMeasurement().getBand())) {
                            // if the measurements are of the same band, add lower bound to temporal resolution
                            T_max[j][q] = requirements.getTemporalResolutionMax() * J.getDepMeasurements().size();
                            T_min[j][q] = requirements.getTemporalResolutionMin();
                        }
                        else{
                            // if they are measurements of different bands, there is no lower bound to temporal resolution
                            T_max[j][q] = requirements.getTemporalResolutionMax();
                            T_min[j][q] = 0.0;
                        }
                        constraintFound = true;
                    }
                    if(!constraintFound){
                        // Subtask J does not depend on subtask Q
                        D[j][q] = -1;
                        T_min[j][q] = 0.0;
                        T_max[j][q] = Double.POSITIVE_INFINITY;
                    }
                }
            }
        }


    }

    public int countDependencies(Subtask j){
        int count = 0;
        for(Subtask q : j.getParentTask().getSubtasks()){
            if(this.depends(j,q)) count++;
        }
        return count;
    }

    public int getNumDependencies(Subtask subtask){
        int j = subtask.getI_q();
        int numDependencies = 0;
        for(int q = 0; q < subtask.getParentTask().getSubtasks().size(); q++){
            if(depends(j,q)) numDependencies++;
        }
        return numDependencies;
    }

    public boolean depends(Subtask j, Subtask q){
        return D[j.getI_q()][q.getI_q()] >= 1;
    }
    public boolean depends(int j, int q){
        return D[j][q] >= 1;
    }
    public boolean noDependency(Subtask j, Subtask q){
        return D[j.getI_q()][q.getI_q()] == 0;
    }
    public boolean mutuallyExclusive(Subtask j, Subtask q){
        return D[j.getI_q()][q.getI_q()] <= -1;
    }

    public double Tmax(Subtask j, Subtask q){
        return T_max[j.getI_q()][q.getI_q()];
    }

    public double Tmin(Subtask j, Subtask q){
        return T_min[j.getI_q()][q.getI_q()];
    }
}
