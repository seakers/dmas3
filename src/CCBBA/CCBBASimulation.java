package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import CCBBA.CCBBASimulation;
import CCBBA.scenarios.random.*;
import CCBBA.source.*;

public class CCBBASimulation extends AbstractAgent {

    // Organizational constants
    public static final String MY_COMMUNITY = "simu";
    public static final String SIMU_GROUP = "simu";
    public static final String AGENT_THINK = "agent_planner";
    public static final String AGENT_DO = "agent_execute";
    public static final String AGENT_DIE = "agent_die";
    public static final String ENV_ROLE = "environment";
    public static final String SCH_ROLE = "scheduler";
    public static final String RESULTS_ROLE = "results";
    private String directoryAddress;
    private int numAgents = 4;

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
        createGroup(MY_COMMUNITY, SIMU_GROUP);

        // 2 : create the environment
        Scenario environment = new RandomScenario(30);
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
    private void setupAgent() {
        for (int i = 0; i < this.numAgents; i++) {
            //random agents
            launchAgent(new SimulatedAgentRandom());
        }
    }

    private void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}
