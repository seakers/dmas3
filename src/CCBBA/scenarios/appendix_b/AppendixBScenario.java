package CCBBA.scenarios.appendix_b;

import CCBBA.CCBBASimulation;
import CCBBA.source.Scenario;
import CCBBA.source.Task;
import madkit.kernel.AbstractAgent;
import madkit.simulation.probe.PropertyProbe;

import java.awt.*;
import java.util.Vector;

public class AppendixBScenario extends Scenario {
    public AppendixBScenario(int numTasks, int numAgents){
        this.numTasks = numTasks;
        this.numAgents = numAgents;
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

        tc_1.add(0.0);                //t_start
        tc_1.add(inf);                //t_end
        tc_1.add(1.0);                //task duration
        tc_1.add(1.0);                //t_corr
        tc_1.add(0.015);              //lambda

        Task task1 = new Task(100.0, x_1, "CONSTANT", 2.0, e_1, tc_1);
        scenarioTasks.add(task1);

        //Define task 2
        e_2.add("MW");

        tc_2.add(0.0);                //t_start
        tc_2.add(inf);                //t_end
        tc_2.add(1.0);                //task duration
        tc_2.add(1.0);                //t_corr
        tc_2.add(0.015);              //lambda

        Task task2 = new Task(30.0, x_2, "CONSTANT", 2.0, e_2, tc_2);
        scenarioTasks.add(task2);

//            //Define task 3
//            Dimension x_3 = new Dimension(4, 4);
//            Vector<String> e_3 = new Vector<>();
//            Vector<Double> tc_3 = new Vector<>();
//
//            e_3.add("IR");
//
//            tc_3.add(0.0);
//            tc_3.add(inf);
//            tc_3.add(1.0);
//            tc_3.add(1.0);
//            tc_3.add(1.0);
//
//            Task task3 = new Task(30.0, x_3, 2.0, e_3, tc_3);
//            scenarioTasks.add(task3);

        // 1 : request my role so that the viewer can probe me
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1, "environment"));
        addProbe(new AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2, "environment"));
        addProbe(new AgentsProbe(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "environment"));

    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, AppendixBScenario.this);
        }
    }

}
