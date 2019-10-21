package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import CCBBA.source.*;
import CCBBA.scenarios.figure3.*;

public class Figure3Simulation extends AbstractAgent {

    /**
     * Organizational constants
     */
    private String directoryAddress;
    private int numAgents = 0;

    /**
     * Sim Setup
     */
    public static void main(String[] args) {
        for(int i = 0; i < 10; i++) {
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
        Scenario environment = new ValidationScenario(30, "INT");
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgent("INT");

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(this.numAgents, this.directoryAddress), false );
    }

    /**
     * Helping functions
     */
    private void setupAgent(String agentType){
        if(agentType.equals("INT")){
            launchAgent(new ValidationAgentInt());
            launchAgent(new ValidationAgentInt());
            this.numAgents = 2;
        }
        else if(agentType.equals("MOD")){
            // e = {IR}
            launchAgent(new ValidationAgentMod01());
            launchAgent(new ValidationAgentMod01());
            // e = {MW}
            launchAgent(new ValidationAgentMod02());
            launchAgent(new ValidationAgentMod02());
            this.numAgents = 4;
        }
    }

    private void createFile(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-validation-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}
