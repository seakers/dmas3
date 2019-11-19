package CCBBA.scenarios.debugger;

import CCBBA.CCBBASimulation;
import CCBBA.bin.*;
import CCBBA.scenarios.appendix_b.AppendixBScenario;
import madkit.kernel.AbstractAgent;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class DebuggerScenario extends Scenario {
    private double t_corr;

    public DebuggerScenario(int numTasks, String simType, double t_corr){
        this.numTasks = numTasks;
        if(simType.equals("INT")){ this.numAgents = 2; }
        else if(simType.equals("MOD")){ this.numAgents = 2; }
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

            // determine instrument requirements
            Vector<String> instruments = new Vector<>();
            if (i < this.numTasks / 1) {
//            if (i < this.numTasks ) { // create all tasks with two sensor requirements
                // create 50% of tasks with two sensors
                instruments.add(this.instrumentList.get(0));
                instruments.add(this.instrumentList.get(1));
            } else if(i < (int) 3.0 / 4.0 * this.numTasks) {
                instruments.add(this.instrumentList.get(0));
            } else {
                instruments.add(this.instrumentList.get(1));
            }

            // determine time constraints
            // TC = {t_start, t_end, d, t_corr, lambda}
            Vector<Double> tc = new Vector<>();
            tc.add(0.0);                //t_start
            tc.add(inf);                //t_end
            tc.add(Math.random());      //task duration
            tc.add(this.t_corr);        //t_corr
            tc.add(0.015);              //lambda

            // determine task costs and max score
            double S_max = 50.0 + 50 * Math.random();
            double task_cost = 0.0 / 100;

            // add task to scenario vector
            Task tempTask = new Task(S_max, x_task, "PROPORTIONAL", task_cost, instruments, tc);
            scenarioTasks.add(tempTask);
        }

        // 1 : request my role so that the viewer can probe me
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new DebuggerScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1, "environment"));
        addProbe(new DebuggerScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));

//        // Describe initial conditions for environment and tasks
//        int x_max = 50;
//        int y_max = 50;
//        dimension = new Dimension(x_max, y_max);
//        double inf = Double.POSITIVE_INFINITY;
//
//        this.instrumentList.add("IR");
//        this.instrumentList.add("MW");
//
//        // Define task 1
//        Dimension x_1 = new Dimension(0, 0);
//
//        Vector<String> e_1 = new Vector<>();
//        e_1.add("IR");
//        e_1.add("MW");
//
//        Vector<Double> tc_1 = new Vector<>();
//        tc_1.add(0.0);                //t_start
//        tc_1.add(inf);                //t_end
//        tc_1.add(1.0);                //task duration
//        tc_1.add(1.0);                //t_corr
//        tc_1.add(0.015);              //lambda
//
//        Task task1 = new Task(100.0, x_1, "CONSTANT", 0.0, e_1, tc_1);
//        scenarioTasks.add(task1);
//
//        //Define task 2
//        Dimension x_2 = new Dimension(10, 0);
//
//        Vector<String> e_2 = new Vector<>();
//        e_2.add("IR");
//        e_2.add("MW");
//
//        Vector<Double> tc_2 = new Vector<>();
//        tc_2.add(0.0);                //t_start
//        tc_2.add(inf);                //t_end
//        tc_2.add(1.0);                //task duration
//        tc_2.add(1.0);                //t_corr
//        tc_2.add(0.015);              //lambda
//
//        Task task2 = new Task(100.0, x_2, "CONSTANT", 0.0, e_2, tc_2);
//        scenarioTasks.add(task2);
//
//        // 1 : request my role so that the viewer can probe me
//        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);
//
//        // 2 : this probe is used to initialize the agents' environment field
//        addProbe(new DebuggerScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1, "environment"));
//        addProbe(new DebuggerScenario.AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));

    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, DebuggerScenario.this);
        }
    }
}