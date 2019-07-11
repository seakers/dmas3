package seakers.orekit.multiagent.CCBBA;

import java.awt.Dimension;
import java.util.Vector;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;

public class Scenario extends Watcher {

    /**
     * environment's boundaries
     */
    private Dimension dimension;
    private Vector<Task> scenarioTasks = new Vector<>();

    /**
     * so that the agents can perceive my dimension
     */
    public Dimension getDimension() {
        return dimension;
    }

    public Vector<Task> getTasks(){
        return scenarioTasks;
    }

    @Override
    protected void activate() {
        // Describe initial conditions for environment and tasks
        dimension = new Dimension(400, 400);
        double inf = Double.POSITIVE_INFINITY;

        Dimension x_1 = new Dimension( 0, 3);
        Dimension x_2 = new Dimension( 4, 3);
        Vector<String> e_1 = new Vector<>();
        Vector<String> e_2 = new Vector<>();
        // TC = {t_start, t_end, d, t_corr, lambda}
        Vector<Double> tc_1 = new Vector<>();
        Vector<Double> tc_2 = new Vector<>();

        // Define task 1
        e_1.add("IR");
        e_1.add("MW");

        tc_1.add(0.0);
        tc_1.add(inf);
        tc_1.add(5.0);
        tc_1.add(1.0);

        Task task1 = new Task( x_1, 2.0, e_1, tc_1);
        scenarioTasks.add(task1);

        //Define task 2
        e_2.add("MW");

        tc_2.add(0.0);
        tc_2.add(inf);
        tc_2.add(5.0);
        tc_2.add(1.0);

        Task task2 = new Task( x_2, 2.0, e_2, tc_2);
        scenarioTasks.add(task2);

        // 1 : request my role so that the viewer can probe me
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new AgentsProbe(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "environment"));

    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, Scenario.this);
        }
    }

}

