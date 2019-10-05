package CCBBA;

import madkit.kernel.AbstractAgent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import CCBBA.source.*;
import CCBBA.scenarios.validation.*;

public class ValidationSimulation extends AbstractAgent {

    /**
     * Organizational constants
     */
    private String directoryAddress;
    private int numAgents = 0;

    /**
     * Sim Setup
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
        Scenario environment = new ValidationScenario(10);
        launchAgent(environment);

        // 3 : launch some simulated agents
        setupAgent("2D_VALIDATION_MOD");

        // 4 : create the scheduler
        launchAgent(new myScheduler("CCBBA"), false);

        // 5 : launch results compiler
        launchAgent( new ResultsCompiler(this.numAgents, this.directoryAddress), false );
    }

    /**
     * Helping functions
     */
    private void setupAgent(String agentType){
        if(agentType.equals("2D_VALIDATION_INT")){
            launchAgent(new ValidationAgentInt());
            launchAgent(new ValidationAgentInt());
            this.numAgents = 2;
        }
        else if(agentType.equals("2D_VALIDATION_MOD")){
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
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();
        this.directoryAddress = "src/CCBBA/results/results-validation-"+ dtf.format(now);
        new File( this.directoryAddress ).mkdir();
    }
}
