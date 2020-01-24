package CCBBA.lib;

import CCBBA.scenarios.validation.ValidationScenario;
import jmetal.encodings.variable.Int;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.io.IOException;
import java.util.ArrayList;

public class Scenario extends Watcher {
    /**
     * environment's description
     */
    private JSONObject scenarioData;
    private JSONArray scenarioTaskData;
    private JSONObject worldData;
    private ArrayList<Task> scenarioTasks = new ArrayList<>();
    // 2D world
    private ArrayList<Double> bounds = new ArrayList<>();


    public Scenario(JSONObject inputData) throws Exception {
        // Load Scenario data
        this.scenarioData = (JSONObject) inputData.get("Scenario");
    }

    @Override
    protected void activate() {
        try {
            // Set up world
            // 1-Unpack world data
            this.worldData = (JSONObject) scenarioData.get("World");

            // 2-Check what type of world it is
            String worldType = this.worldData.get("Type").toString();
            if( worldType.equals("2D_Grid") ){
                // set up 2D world
                JSONArray boundsData = (JSONArray) this.worldData.get("Bounds");
                if(boundsData.size() != 2){
                    throw new Exception("INPUT ERROR: World bounds do not match world type");
                }
                double x_max = (double) boundsData.get(0);
                double y_max = (double) boundsData.get(1);

                bounds.add(x_max);
                bounds.add(y_max);
            }
            else if( worldType.toString().equals("3D_Grid") ){
                // set up 3D world
                JSONArray boundsData = (JSONArray) this.worldData.get("Bounds");
                if(boundsData.size() != 3){
                    throw new Exception("INPUT ERROR: World bounds do not match world type");
                }
                double x_max = (double) boundsData.get(0);
                double y_max = (double) boundsData.get(1);
                double z_max = (double) boundsData.get(2);

                bounds.add(x_max);
                bounds.add(y_max);
                bounds.add(z_max);
            }
//            else if( worldType.toString().equals("Earth") ){
//                // set up Orekit 3D world
//            }
            else{
                throw new Exception("INPUT ERROR: World type not supported.");
            }

            // Create tasks
            // 1-Unpack task list
            this.scenarioTaskData = (JSONArray) scenarioData.get("TaskList");

            // 2-Create the required instances of each task on the list
            for(int i = 0; i < scenarioTaskData.size(); i++){
                JSONObject taskData = (JSONObject) scenarioTaskData.get(i);
                int instances = Integer.parseInt( taskData.get("Instances").toString() );

                for(int j = 0; j < instances; j++){
                    scenarioTasks.add( new Task((JSONObject) scenarioTaskData.get(i), this.worldData) );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 1 : request my role so that the viewer can probe me
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENV_ROLE);

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK, "environment"));
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE, "environment"));
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


