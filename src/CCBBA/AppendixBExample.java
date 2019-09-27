package CCBBA;

import CCBBA.CCBBASimulation;
import CCBBA.scenarios.appendix_b.*;
import CCBBA.source.*;
import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppendixBExample extends AbstractAgent {

    // Organizational constants
    private String directoryAddress;
    private int numAgents = 2;

    /**
     * Sim Setup
     *
     */
    public static void main(String[] args) {
        for(int i = 0; i < 1; i++) {
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
        Scenario environment = new Scenario("APPENDIX_B",  2);
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgent();

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(this.numAgents, this.directoryAddress), false );
    }

    /**
     * Helping functions
     */

    private void setupAgent(){
        launchAgent(new SimulatedAgent02());
        launchAgent(new SimulatedAgent01());
    }

    private void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-appendix_b-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}
