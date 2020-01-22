package CCBBA.lib;

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
    private JSONArray scenarioTaskData;
    private ArrayList<Task> scenarioTasks;
    private Vector3D dimension;

    public Scenario(JSONObject inputData) throws Exception {
        // Load Scenario data
        JSONArray scenarioData = (JSONArray) inputData.get("Scenario");

        // Get Task List
        int i_tasks = -1;
        for (int i = 0; i < scenarioData.size(); i++) {
            JSONObject object_i = (JSONObject) scenarioData.get(i);

            if (object_i.containsKey("TaskList")) {
                i_tasks = i;
                break;
            }
        }
        if(i_tasks == -1) throw new Exception("INPUT ERROR: Task List not included in input file.");
        JSONObject taskListObject = (JSONObject) scenarioData.get(i_tasks);
        this.scenarioTaskData = (JSONArray) taskListObject.get("TaskList");

    }

    @Override
    protected void activate() {
        // Create tasks
        // -Unpack Task List and create a task for each entry
        for(int i = 0; i < scenarioTaskData.size(); i++){
            JSONObject taskData = (JSONObject) scenarioTaskData.get(i);
            int instances = (int) taskData.get("Instances");

            for(int j = 0; j < instances; j++){
                scenarioTasks.add( new Task((JSONObject) scenarioTaskData.get(i)) );
            }
        }
    }
}
