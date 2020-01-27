package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

import CCBBA.bin.*;
import CCBBA.scenarios.figure3.*;

public class Figure3Simulation extends AbstractAgent {

    /**
     * Organizational constants
     */
    private String directoryAddress;
    double inf = Double.POSITIVE_INFINITY;
    private int numAgents = 0;

    // Set up parameters
    private static String symType;
    private static double t_corr;

    /**
     * Sim Setup
     */
    public static void main(String[] args) {
        Vector<Double> timeList = new Vector<>();
        timeList.add(Double.POSITIVE_INFINITY);
        timeList.add(0.0);
        timeList.add(2.0);
        timeList.add(4.0);
        timeList.add(6.0);
        timeList.add(8.0);

        Vector<String> nameList = new Vector<>();
        nameList.add("MOD");
        nameList.add("INT");

        for(String name : nameList){
            for(double time : timeList){
                String directoryAddress;
                directoryAddress = "src/CCBBA/results/validation_plots/figure_3/" + name + "_" + time;
                new File( directoryAddress ).mkdir();

                symType = name;
                t_corr = time;

                for(int i = 0; i < 128; i++) {
                    executeThisAgent(1, false);
                }

                if(name.equals("INT")) break;
            }
        }
    }

    @Override
    protected void activate() {
        String type = symType;
        double t = t_corr;
        // 0 : create results directory
        createFile(type, t);

        // 1 : create the simulation group
        createGroup(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP);

        // 2 : create the environment
        Scenario environment = new ValidationScenario(30, type, t);
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgents(type);

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new AbstractResultsCompiler(this.numAgents, this.directoryAddress), false );
    }

    /**
     * Helping functions
     */
    private void setupAgents(String type){
        if(type.equals("INT")){
            launchAgent(new ValidationAgentInt());
            launchAgent(new ValidationAgentInt());
            this.numAgents = 2;
        }
        else if(type.equals("MOD")){
            // e = {IR}
            launchAgent(new ValidationAgentMod01());
            launchAgent(new ValidationAgentMod01());
            // e = {MW}
            launchAgent(new ValidationAgentMod02());
            launchAgent(new ValidationAgentMod02());
            this.numAgents = 4;
        }
    }

    private void createFile(String type, double t){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/validation_plots/figure_3/" + type + "_" + t
                + "/results-validation-3-" + type + "_" + t + "-" + dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }


}
