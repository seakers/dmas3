package CCBBA.source;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import CCBBA.CCBBASimulation;

import java.awt.*;
import java.util.Vector;

public class Scenario extends Watcher {

    /**
     * environment's boundaries
     */
    protected Dimension dimension;
    protected Vector<Task> scenarioTasks = new Vector<>();
    protected int numTasks;
    protected Vector<String> instrumentList = new Vector<>();

    /**
     * so that the agents can perceive my dimension
     */
    public Dimension getDimension() {
        return dimension;
    }
    public Vector<Task> getTasks(){
        return scenarioTasks;
    }

    public Scenario(){

    }

    public Scenario(int numTasks){
        this.numTasks = numTasks;
    }

    @Override
    protected void activate() {
        // Describe initial conditions for environment and tasks
        int x_max = 50;
        int y_max = 50;
        dimension = new Dimension(x_max, y_max);
        double inf = Double.POSITIVE_INFINITY;


        this.instrumentList.add("IR");
        this.instrumentList.add("MW");
        this.instrumentList.add("VIS");

        Dimension x_1 = new Dimension(0, 3);
        Vector<String> e_1 = new Vector<>();
        Vector<Double> tc_1 = new Vector<>();

        // Define task 1
        e_1.add("IR");
        e_1.add("MW");
        e_1.add("VIS");

        tc_1.add(0.0);
        tc_1.add(inf);
        tc_1.add(1.0);
        tc_1.add(1.0);
        tc_1.add(1.0);

        Task task1 = new Task(100.0, x_1, "CONSTANT", 2.0, e_1, tc_1);
        scenarioTasks.add(task1);

        // 1 : request my role so that the viewer can probe me
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "environment"));
        addProbe(new AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));

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