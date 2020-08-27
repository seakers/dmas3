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
                    for(Measurement depMeasurements : Q.getDepMeasurements()){
                        if(depMeasurements.equals(J.getMainMeasurement())){
                            D[j][q] = 1;
                            T_max[j][q] = requirements.getTemporalResolutionMax();
                            if(J.getMainMeasurement().getBand().equals(Q.getMainMeasurement().getBand())){
                                // if the measurements are of the same band, add lower bound to temporal resolution
                                T_min[j][q] = requirements.getTemporalResolutionMin();
                            }
                            else{
                                // if they are measurements of the same band, there is no lower bound to temporal resolution
                                T_min[j][q] = 0.0;
                            }

                        }
                    }
                }
            }
        }
    }
}
