package CCBBA.scenarios.random;

import CCBBA.CCBBASimulation;
import CCBBA.scenarios.appendix_b.AppendixBScenario;
import CCBBA.source.Scenario;
import CCBBA.source.Task;
import madkit.kernel.AbstractAgent;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class RandomScenario extends Scenario {

    public RandomScenario(int numTasks){
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

        for(int i = 0; i < this.numTasks; i++){ //for every task
            // determine location
            int x = (int)(x_max * Math.random());
            int y = (int)(y_max * Math.random());
            Dimension x_task = new Dimension(x, y);

            // determine instrument requirements
            Vector<String> instruments = new Vector<>();
            int numInstruments = (int) (this.instrumentList.size() * (Math.random()) + 1 );

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
            double S_max = 100.0 * Math.random();
            double task_cost = Math.random();

            // add task to scenario vector
            Task tempTask = new Task(S_max, x_task, "CONSTANT", task_cost, instruments, tc);
            scenarioTasks.add(tempTask);
        }

        // 1 : request my role so that the viewer can probe me
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new RandomScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "environment"));
        addProbe(new RandomScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));

    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, RandomScenario.this);
        }
    }
}
