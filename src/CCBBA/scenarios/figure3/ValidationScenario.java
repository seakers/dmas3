package CCBBA.scenarios.figure3;

import CCBBA.CCBBASimulation;
import CCBBA.scenarios.appendix_b.AppendixBScenario;
import CCBBA.bin.*;
import madkit.kernel.AbstractAgent;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class ValidationScenario extends Scenario {
    private double t_corr;

    public ValidationScenario(int numTasks, String simType, double t_corr){
        this.numTasks = numTasks;
        if(simType.equals("INT")){ this.numAgents = 2; }
        else if(simType.equals("MOD")){ this.numAgents = 4; }
        this.t_corr = t_corr;
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

        for (int i = 0; i < this.numTasks; i++) { //for every task
            // determine location
            int x = (int) (x_max * Math.random());
            int y = (int) (y_max * Math.random());
            Dimension x_task = new Dimension(x, y);

            // determine task costs and max score
            double S_max;
            double task_cost = 1.14 / 100;

            // determine instrument requirements
            Vector<String> instruments = new Vector<>();
            if (i < this.numTasks / 2) {
//            if (i < this.numTasks ) { // create all tasks with two sensor requirements
                // create 50% of tasks with two sensors
                instruments.add(this.instrumentList.get(0));
                instruments.add(this.instrumentList.get(1));

                S_max = 100.0 + 50 * Math.random();
            } else if(i < (int) 3.0 / 4.0 * this.numTasks) {
                instruments.add(this.instrumentList.get(0));

                S_max = 50.0 + 25.0 * Math.random();
            } else {
                instruments.add(this.instrumentList.get(1));

                S_max = 50.0 + 25.0 * Math.random();
            }

            // determine time constraints
            // TC = {t_start, t_end, d, t_corr, lambda}
            Vector<Double> tc = new Vector<>();
            tc.add(0.0);                //t_start
            tc.add(inf);                //t_end
            tc.add(Math.random());      //task duration
            tc.add(this.t_corr);        //t_corr
            tc.add(0.015);              //lambda



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

        AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, ValidationScenario.this);
        }
    }
}