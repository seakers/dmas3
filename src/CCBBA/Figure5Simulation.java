package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import CCBBA.source.*;
import CCBBA.scenarios.figure5.*;
import madkit.kernel.Madkit;

public class Figure5Simulation extends AbstractAgent {

    /**
     * Organizational constants
     */
    private String directoryAddress;
    private int numAgents = 0;

    /**
     * Sim Setup
     */
    public static void main(String[] args) {
        for(int i = 0; i < 25; i++) {
            executeThisAgent(1, false);
        }
    }

    @Override
    protected void activate() {
        // 0 : create results directory
        createFile();

        // 1 : create the simulation group
        createGroup(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP);

        // 2 : create the environment
        Scenario environment = new ValidationScenario(30);
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgent( 100.0 );

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(this.numAgents, this.directoryAddress), false );
    }

    /**
     * Helping functions
     */
    private void setupAgent(double merge_cost){
        // e = {IR}
        launchAgent(new ValidationAgentMod01(merge_cost));
        launchAgent(new ValidationAgentMod01(merge_cost));
        // e = {MW}
        launchAgent(new ValidationAgentMod02(merge_cost));
        launchAgent(new ValidationAgentMod02(merge_cost));
        // e = {VIS}
        launchAgent(new ValidationAgentMod03(merge_cost));
        launchAgent(new ValidationAgentMod03(merge_cost));
        this.numAgents = 6;
    }

    private void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-validation-5-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}
