package modules.simulation;

import madkit.kernel.AbstractAgent;
import modules.simulation.results.Results;
import modules.spacecraft.Spacecraft;
import modules.environment.Environment;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Simulation extends AbstractAgent {
    private static Architecture arch;
    private static ProblemStatement prob;
    private static String archName;
    private Environment environment;
    private Results results;
    private String directoryAddress;

    public static void main(String[] args) throws Exception {
        String architecture = "TEST";
        String problem = "TEST";
        archName = architecture;

        // Load scenario problem to evaluate architecture
        prob = new ProblemStatement(architecture+".json", problem);

        // Load architecture to be evaluated
        arch = new Architecture(architecture+".json", problem);

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
            this.environment = new Environment(prob, directoryAddress);
            launchAgent(this.environment, false);

            // 4 : launch architecture agents
            launchSpaceSegment();

            // 5 : launch simulation scheduler
            launchAgent(new SimScheduler(), false);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createFileDirectory(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-hh_mm_ss_SSS");
        LocalDateTime now = LocalDateTime.now();

        directoryAddress = this.prob.getOutputFileDir();
        new File( directoryAddress ).mkdir();
        directoryAddress += "/results-" + archName + "-" + prob.getProblemStatement() + "-" + dtf.format(now);
        new File( directoryAddress).mkdir();
    }

    private void launchSpaceSegment(){
        for(Spacecraft sc : arch.getSpaceSegment()){
            launchAgent(sc);
        }
    }

    public Results getResults(){return this.results;}
}
