package CCBBA.lib;

import jmetal.encodings.variable.Int;
import madkit.kernel.Watcher;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;

public class Scenario extends Watcher {
    /**
     * environment's description
     */
    private JSONObject scenarioData;
    private JSONArray scenarioTaskData;
    private ArrayList<Task> scenarioTasks;
    private Vector3D dimension;

    public Scenario(JSONObject inputData) throws Exception {
        // Load Scenario data
        this.scenarioData = (JSONObject) inputData.get("Scenario");
    }

    @Override
    protected void activate() {
        // Create tasks
        // 1-Unpack task list
        this.scenarioTaskData = (JSONArray) scenarioData.get("TaskList");

        // 2-Create the required instances of each task on the list
        for(int i = 0; i < scenarioTaskData.size(); i++){
            JSONObject taskData = (JSONObject) scenarioTaskData.get(i);
            int instances = Integer.parseInt( taskData.get("Instances").toString() );

            for(int j = 0; j < instances; j++){
                scenarioTasks.add( new Task((JSONObject) scenarioTaskData.get(i)) );
            }
        }

        int x = 1;
    }
}
