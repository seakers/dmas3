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
        Scenario environment = new Scenario("2D_VALIDATION", 30);
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgent("2D_VALIDATION_INT");

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(2, this.directoryAddress), false );
    }

    private void setupAgent(String agentType){
        if(agentType == "APPENDIX_B"){
            launchAgent(new SimulatedAgent02());
            launchAgent(new SimulatedAgent01());
        }
        else if(agentType == "2D_VALIDATION_INT"){
            launchAgent(new ValidationAgentInt());
            launchAgent(new ValidationAgentInt());
        }
        else if(agentType == "2D_VALIDATION_MOD"){
            // e = {IR}
            launchAgent(new ValidationAgentMod01());
            launchAgent(new ValidationAgentMod01());
            // e = {MW}
            launchAgent(new ValidationAgentMod02());
            launchAgent(new ValidationAgentMod02());
        }
    }

    private void setupAgent(String agentType, int numAgents) {
        if (agentType == "RANDOM") {
            for (int i = 0; i < numAgents; i++) {
                //random agents
                launchAgent(new SimulatedAgentRandom());
            }
        }
    }

    private void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }

    public static void main(String[] args) {
        for(int i = 0; i < 10; i++) {
            executeThisAgent(1, false);
        }
    }
}
