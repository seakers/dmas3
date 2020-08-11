import madkit.kernel.AbstractAgent;
import modules.planner.Results;
import modules.simulation.Architecture;
import modules.simulation.ProblemStatement;
import modules.simulation.Simulation;

public class Driver extends AbstractAgent{
    private static Simulation sim;

    public static void main(String[] args) throws Exception {
        String inputFile = "template.json";
        String problem = "SoilMoisture";

        // Load scenario problem to evaluate architecture
        ProblemStatement prob = new ProblemStatement(inputFile, problem);

        // Load architecture to be evaluated
        Architecture arch = new Architecture(inputFile);

        // Initiate simulation
        sim = new Simulation(inputFile, arch, prob);

        // Run simulation and save results
        executeThisAgent(1,false);
    }

    @Override
    protected void activate(){
        launchAgent(sim);
        Results results = sim.getResults();

        // Print score and cost
//        System.out.println(results.getScore() + " " + results.getCost());
    }
}
