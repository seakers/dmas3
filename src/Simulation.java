import madkit.kernel.AbstractAgent;
import simulation.Architecture;
import simulation.ProblemStatement;
import simulation.SimGroups;
import simulation.SimScheduler;
import simulation.results.Results;
import modules.spacecraft.Spacecraft;
import modules.environment.Environment;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Simulation extends AbstractAgent {
    protected static Architecture arch;
    protected static ProblemStatement prob;
    protected static String archName;
    protected Environment environment;
    protected Results results;
    protected String directoryAddress;

    public static void main(String[] args) throws Exception {
        String architecture = "SMAP";
        String problem = "ASCEND";
        archName = architecture;

        // Load architecture to be evaluated
        arch = new Architecture(architecture, problem);

        // Load scenario problem to evaluate architecture
        prob = new ProblemStatement(architecture, problem);

        // Run simulation and save results
        executeThisAgent(1,false);
    }

    @Override
    protected void activate(){
        try {
            // 1 : create results directory
            createFileDirectory();

            // 2 : create the simulation group
            createGroup(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);

            // 3 : launch simulation environment
            this.environment = new Environment(prob, arch, directoryAddress);
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
