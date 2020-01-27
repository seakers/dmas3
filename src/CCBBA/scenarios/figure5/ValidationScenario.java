package CCBBA.scenarios.figure5;

import CCBBA.CCBBASimulation;
import CCBBA.bin.*;
import madkit.kernel.AbstractAgent;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class ValidationScenario extends Scenario {

    public ValidationScenario(int numTasks){
        this.numTasks = numTasks;
        this.numAgents = 6;

        this.t_0 = 0;
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

        for (int i = 1; i <= this.numTasks; i++) { //for every task
            // determine location
            int x = (int) (x_max * Math.random());
            int y = (int) (y_max * Math.random());
            Dimension x_task = new Dimension(x, y);

            // determine instrument requirements
            Vector<String> instruments = new Vector<>();
            if (i < (this.numTasks / 3.0) * 1.0 ) {
                // create 9 tasks with three sensors
                instruments.add(this.instrumentList.get(0));
                instruments.add(this.instrumentList.get(1));
                instruments.add(this.instrumentList.get(2));
            }
            else if (i <= (this.numTasks / 5.0) * 2.0){ // create 9 tasks with two sensors
                instruments.add(this.instrumentList.get(0));
                instruments.add(this.instrumentList.get(1));
            }
            else if (i <= (this.numTasks / 2.0) * 1.0){
                instruments.add(this.instrumentList.get(0));
                instruments.add(this.instrumentList.get(2));
            }
            else if (i <= (this.numTasks / 5.0) * 3.0){
                instruments.add(this.instrumentList.get(1));
                instruments.add(this.instrumentList.get(2));
            }
            else if (i <= (this.numTasks / 15.0) * 11.0){ // create 12 tasks with one sensors
                instruments.add(this.instrumentList.get(0));
            }
            else if (i <= (this.numTasks / 15.0) * 13.0){
                instruments.add(this.instrumentList.get(1));
            }
            else {
                instruments.add(this.instrumentList.get(2));
            }

            // determine time constraints
            // TC = {t_start, t_end, d, t_corr, lambda}
            Vector<Double> tc = new Vector<>();
            tc.add(0.0);                //t_start
            tc.add(inf);                //t_end
            tc.add(Math.random());      //task duration
            tc.add(inf);                //t_corr
            tc.add(0.015);              //lambda

            // determine task costs and max score
            double S_max = 100.0 * Math.random();
            double task_cost = 1.14 / 100;

            // add task to scenario vector
            Task tempTask = new Task(S_max, x_task, "PROPORTIONAL", task_cost, instruments, tc);
            scenarioTasks.add(tempTask);
        }

        // 1 : request my role so that the viewer can probe me
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new ValidationScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1, "environment"));
        addProbe(new ValidationScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));
    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, ValidationScenario.this);
        }
    }
}