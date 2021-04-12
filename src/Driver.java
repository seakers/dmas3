import modules.simulation.Dmas;
import org.orekit.errors.OrekitException;

public class Driver extends Dmas {

    /**
     * Main driver for DMAS3 simulations.
     * Must specify input json file before executing simulation
     */
    public static void main(String[] args) throws OrekitException {
        inputFile = "inputTemplate.json";
        executeThisAgent(1, true);
    }
}
