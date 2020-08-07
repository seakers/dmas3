package modules.simulation;

import madkit.kernel.AbstractAgent;

public class Simulation extends AbstractAgent {
    private Architecture arch;
    private ProblemStatement prob;

    public Simulation(String inputFile, Architecture arch, ProblemStatement prob){
        this.arch = arch;
        this.prob = prob;
    }

    public void run(){
        this.prob.executeEnvironmentAgent();
    }
}
