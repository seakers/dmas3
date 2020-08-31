package modules.simulation;

import madkit.kernel.AbstractAgent;
import modules.spacecraft.Spacecraft;
import modules.environment.Environment;
import modules.planner.Results;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Simulation extends AbstractAgent {
    private static Architecture arch;
    private static ProblemStatement prob;
    private Environment environment;
    private Results results;

    public static void main(String[] args) throws Exception {
        String inputFile = "template.json";
        String problem = "SoilMoisture";

        // Load scenario problem to evaluate architecture
        prob = new ProblemStatement(inputFile, problem);

        // Load architecture to be evaluated
        arch = new Architecture(inputFile, problem);

        // Run simulation and save results
        executeThisAgent(1,false);
    }

//    public Simulation(String inputFile, Architecture arch, ProblemStatement prob){
//        this.arch = arch;
//        this.prob = prob;
//        this.results = new Results();
//    }

    @Override
    protected void activate(){
        try {
            // 1 : create results directory
            createFileDirectory();

            // 2 : create the simulation group
            createGroup(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);

            // 3 : launch simulation environment
            this.environment = new Environment(prob);
            launchAgent(this.environment, false);

            // 4 : launch architecture agents
            launchSpaceSegment();

            // 5 : launch simulation scheduler
            launchAgent(new SimScheduler(), false);

            // 6 : launch results compiler
            int x = 1;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createFileDirectory(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();

        String directoryAddress = this.prob.getOutputFileDir();
        new File( directoryAddress ).mkdir();
        directoryAddress += "/results-" + prob.getProblemStatement() + "-"+ dtf.format(now);
        new File( directoryAddress).mkdir();
    }

    private void launchSpaceSegment(){
        for(Spacecraft sc : arch.getSpaceSegment()){
            launchAgent(sc);
        }
    }

    public Results getResults(){return this.results;}
}
