/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.sensitivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.moeaframework.analysis.sensitivity.Parameter;
import org.moeaframework.analysis.sensitivity.SampleGenerator;
import org.moeaframework.analysis.sensitivity.SobolAnalysis;

/**
 *
 * @author nozomihitomi
 */
public abstract class SobolSensitivityAnalysis {

    private final int n;

    private List<Parameter> parameters;

    private final File paramsFile = new File("params.txt");

    private final File metricsFile = new File("metrics.txt");

    public SobolSensitivityAnalysis(int n) {
        this.n = n;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void run() throws Exception {
        File sampleFile = generateSamples(n, parameters);
        //read a sample file 
        RealMatrix samples = MatrixUtils.createRealMatrix(
                n * (2 * parameters.size() + 2), parameters.size());
        int row = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(sampleFile))) {
            String line = br.readLine();
            while (line != null) {
                String[] vals = line.split("\\s");
                for (int col = 0; col < parameters.size(); col++) {
                    samples.setEntry(row, col, Double.parseDouble(vals[col]));
                }
                line = br.readLine();
                row++;
            }
        } catch (IOException ex) {
            Logger.getLogger(SobolSensitivityAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }

        RealMatrix metrics = evaluateAll(samples);
        //write all metrics to a file 
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(metricsFile))) {
            for (int i = 0; i < metrics.getRowDimension(); i++) {
                for (int j = 0; j < metrics.getColumnDimension(); j++) {
                    bw.append(String.valueOf(metrics.getEntry(i, j))).append(" ");
                }
                bw.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(SobolSensitivityAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }

        computeIndices(metricsFile, 1);
    }

    /**
     * Generates the samples for the sensitivity analysis using the Saltelli
     * method
     *
     * @param n a parameter to set the number of samples. Number of samples =
     * n*(2*p+2) where p is the number of parameters
     * @param parameters the parameters involved in the
     * @return the file containing the samples
     */
    private File generateSamples(int n, List<Parameter> parameters) throws Exception {
        //Create a parameters file 
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(paramsFile))) {
            for (Parameter p : parameters) {
                bw.append(p.getName()).append(" ");
                bw.append(String.valueOf(p.getLowerBound())).append(" ");
                bw.append(String.valueOf(p.getUpperBound()));
                bw.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(SobolSensitivityAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }

        String[] cmd = new String[8];
        cmd[0] = "-n";
        cmd[1] = String.valueOf(n);
        cmd[2] = "-p";
        cmd[3] = paramsFile.getName();
        cmd[4] = "-m";
        cmd[5] = "saltelli";
        cmd[6] = "-o";
        cmd[7] = "samples.txt"; //write samples out to a file

        SampleGenerator.main(cmd);

        File out = new File(cmd[7]);
        if (out.isFile()) {
            return out;
        } else {
            throw new IllegalStateException("Error: could not create sample file.");
        }
    }

    /**
     * Evaluates the metrics associated with the parameters set at the given
     * input values
     *
     * @param values the input values of the parameters where each row is a
     * sample and each column contains the values for a parameter
     * @return the output metrics where each row corresponds to a sample and
     * each column is a different metric
     */
    public abstract RealMatrix evaluateAll(RealMatrix values);

    public File computeIndices(File metricsFile, int ind) throws Exception {
        String[] cmd = new String[8];
        cmd[0] = "-p";
        cmd[1] = paramsFile.getName();
        cmd[2] = "-i";
        cmd[3] = metricsFile.getName();
        cmd[4] = "-m";
        cmd[5] = String.valueOf(ind);
        cmd[6] = "-o";
        cmd[7] = "indices.txt"; //write samples out to a file

        SobolAnalysis.main(cmd);

        File out = new File(cmd[7]);
        if (out.isFile()) {
            return out;
        } else {
            throw new IllegalStateException("Error: could not compute indices file.");
        }
    }

}
