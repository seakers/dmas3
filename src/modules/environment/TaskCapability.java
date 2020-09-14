package modules.environment;

import org.orekit.time.AbsoluteDate;

import java.util.HashMap;

public class TaskCapability {
    private Task parentTask;
    private int numLooks;
    private HashMap<Subtask, SubtaskCapability> subtaskCapabilities;

    public TaskCapability(Task task){
        parentTask = task;
        numLooks = 0;

        subtaskCapabilities = new HashMap<>();
        for(Subtask subtask : task.getSubtasks()){
            SubtaskCapability subtaskCapability = new SubtaskCapability(subtask);
            subtaskCapabilities.put(subtask,subtaskCapability);
        }
    }

    public SubtaskCapability getSubtaskCapability(Subtask subtask){
        return this.subtaskCapabilities.get(subtask);
    }

    public void updateSubtaskCapability(SubtaskCapability newCapability){
        subtaskCapabilities.put(newCapability.getParentSubtask(), newCapability);
    }
}
