package modules.simulation;

import madkit.kernel.AbstractAgent;
import modules.environment.Environment;
import modules.planner.Results;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Simulation extends AbstractAgent {
    private Architecture arch;
    private ProblemStatement prob;
    private Environment environment;
    private HashMap<String, AbstractAgent> spaceSegment;
    private Results results;

    public Simulation(String inputFile, Architecture arch, ProblemStatement prob){
        this.arch = arch;
        this.prob = prob;
        this.results = new Results();
    }

    public void run(){
        try {



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void activate(){
        try {
            // 1 : create results directory
            createFileDirectory();

            // 2 : create the simulation group


            // 3 : launch simulation environment
            launchAgent(new Environment(prob));

            // 4 : launch architecture agents
            launchSpaceSegment();

            // 5 : launch task scheduler


            // 6 : launch results compiler

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
//        for(String key : spaceSegment.keySet()){
//            launchAgent(spaceSegment.get(key));
//        }
    }

    public Results getResults(){return this.results;}
}
