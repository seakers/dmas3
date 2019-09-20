package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AgentSimulation extends AbstractAgent {

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

    @Override
    protected void activate() {
        // 0 : create results directory
        createFile();

        // 1 : create the simulation group
        createGroup(MY_COMMUNITY, SIMU_GROUP);

        // 2 : create the environment
        //Scenario environment = new Scenario("random", 2);
        Scenario environment = new Scenario("RANDOM", 3);
        launchAgent(environment);

        // 3 : launch some simulated agents
        for (int i = 0; i < 2; i++) {
            //launchAgent(new SimulatedAgent02());
            //launchAgent(new SimulatedAgent01());

            //random agents
            launchAgent(new SimulatedAgentRandom());
        }

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(2, this.directoryAddress), false );
    }

    public void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/Results/results-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }

    public static void main(String[] args) {
        for(int i = 0; i < 1; i++) {
            executeThisAgent(1, false);
        }
    }
}
