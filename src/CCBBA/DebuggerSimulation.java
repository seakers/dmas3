package CCBBA;

import CCBBA.bin.*;
import CCBBA.scenarios.debugger.DebuggerAgentInt;
import CCBBA.scenarios.debugger.DebuggerAgentMod01;
import CCBBA.scenarios.debugger.DebuggerAgentMod02;
import CCBBA.scenarios.debugger.DebuggerScenario;
import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class DebuggerSimulation extends AbstractAgent {

    /**
     * Organizational constants
     */
    private String directoryAddress;
    private int numAgents = 0;

    // Set up parameters
    private static String symType;
    private static double t_corr;

    /**
     * Sim Setup
     */
    public static void main(String[] args) {
        Vector<Double> timeList = new Vector<>();
//        timeList.add(Double.POSITIVE_INFINITY);
        timeList.add(0.0);
//        timeList.add(2.0);
//        timeList.add(4.0);
//        timeList.add(6.0);
//        timeList.add(8.0);

        Vector<String> nameList = new Vector<>();
        nameList.add("MOD");
//        nameList.add("INT");

        for(String name : nameList){
            for(double time : timeList){
                String directoryAddress;
                directoryAddress = "src/CCBBA/results/debugger/" + name + "_" + time;
                new File( directoryAddress ).mkdir();

                symType = name;
                t_corr = time;

                for(int i = 0; i < 10; i++) {
                    executeThisAgent(1, false);
                }

                if(name.equals("INT")) break;
            }
        }
    }

    @Override
    protected void activate() {
        String type = symType;
        double t_c = t_corr;
        // 0 : create results directory
        createFile(type, t_c);

        // 1 : create the simulation group
        createGroup(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP);

        // 2 : create the environment
        Scenario environment = new DebuggerScenario(20, type, t_c);
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
        Vector<AbstractSimulatedAgent> launchedAgents = new Vector<>();
        if(type.equals("INT")){
            launchedAgents.add(new DebuggerAgentInt());
            launchedAgents.add(new DebuggerAgentInt());
        }
        else if(type.equals("MOD")){
            // e = {IR}
            launchedAgents.add(new DebuggerAgentMod01());
            launchedAgents.add(new DebuggerAgentMod01());
            // e = {MW}
            launchedAgents.add(new DebuggerAgentMod02());
            launchedAgents.add(new DebuggerAgentMod02());
        }

        for (AbstractSimulatedAgent launchedAgent : launchedAgents) {
            launchAgent(launchedAgent);
        }
        this.numAgents = launchedAgents.size();
    }

    private void createFile(String type, double t_corr){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/debugger/" + type + "_" + t_corr +
                "/debugger_results-" + type + "_" + t_corr + "-" + dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}