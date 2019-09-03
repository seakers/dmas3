package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class Scenario extends Watcher {

    /**
     * environment's boundaries
     */
    private Dimension dimension;
    private Vector<Task> scenarioTasks = new Vector<>();
    private String type;
    private int numTasks;
    private Vector<String> instrumentList = new Vector<>();

    /**
     * so that the agents can perceive my dimension
     */
    public Dimension getDimension() {
        return dimension;
    }

    public Vector<Task> getTasks(){
        return scenarioTasks;
    }

    public Scenario(String type, int numTasks){
        this.type = type;
        this.numTasks = numTasks;

        this.instrumentList.add("IR");
        this.instrumentList.add("MW");
        this.instrumentList.add("VIS");
    }

    @Override
    protected void activate() {
        // Describe initial conditions for environment and tasks
        int x_max = 400;
        int y_max = 400;
        dimension = new Dimension(x_max, y_max);
        double inf = Double.POSITIVE_INFINITY;

        if(this.type == "random"){
            for(int i = 0; i < this.numTasks; i++){ //for every task
                // determine location
                int x = (int)(x_max * Math.random());
                int y = (int)(y_max * Math.random());
                Dimension x_task = new Dimension(x, y);

                // determine instrument requirements
                Vector<String> instruments = new Vector<>();
                int numInstruments = (int) (this.instrumentList.size() * Math.random());

                while(instruments.size() < numInstruments){
                    // guess an instrument
                    int i_ins = (int) (this.instrumentList.size() * Math.random());

                    // check if it's already in the vector
                    if( !instruments.contains(this.instrumentList.get(i_ins)) ){
                        // if not, add to vector
                        instruments.add(this.instrumentList.get(i_ins));
                    }
                }

                // determine time constraints
                // TC = {t_start, t_end, d, t_corr, lambda}
                Vector<Double> tc = new Vector<>();
                tc.add(0.0);
                tc.add(inf);
                tc.add(Math.random());
                tc.add(1.0);
                tc.add(1.2);

                // determine task costs and max score
                double S_max = 100.0;
                double task_cost = 0.0;

                // add task to scenario vector
                Task tempTask = new Task(S_max, x_task, task_cost, instruments, tc);
                scenarioTasks.add(tempTask);
            }
        }
        else if(this.type == "appendix b") {
            Dimension x_1 = new Dimension(0, 3);
            Dimension x_2 = new Dimension(4, 3);
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
            tc_1.add(1.0);
            tc_1.add(1.0);
            tc_1.add(1.0);

            Task task1 = new Task(100.0, x_1, 2.0, e_1, tc_1);
            scenarioTasks.add(task1);

            //Define task 2
            e_2.add("MW");

            tc_2.add(0.0);
            tc_2.add(inf);
            tc_2.add(1.0);
            tc_2.add(1.0);
            tc_1.add(1.0);

            Task task2 = new Task(30.0, x_2, 2.0, e_2, tc_2);
            scenarioTasks.add(task2);
        }

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