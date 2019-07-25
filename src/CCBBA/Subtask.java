package CCBBA;

import java.util.Vector;

public class Subtask {
    protected String main_task;
    protected Vector<String> dep_tasks;
    protected Vector<Integer> dep_nums;
    protected int main_num;
    protected String name;
    protected boolean complete = false;
    protected Task parentTask;
    protected int K;

    /**
     * Subtask Constructor
     * @param new_main main subtask name
     * @param new_num main subtask number
     */
    public Subtask(String new_main, int new_num, Task parent, int k){
        main_task = new_main;
        main_num = new_num;
        dep_tasks = new Vector<String>();
        dep_nums = new Vector<Integer>();
        name = new_main;
        parentTask = parent;
        K = k;
    }

    public String getMain_task(){
        return main_task;
    }
    public void setMain_task(String new_main){
        main_task = new_main;
    }
    public Vector<String> getDep_tasks(){
        return dep_tasks;
    }
    public void setDep_tasks(Vector<String> newDep_tasks){
        dep_tasks = newDep_tasks;
    }
    public Vector<Integer> getDep_nums(){ return dep_nums; }
    public void setDep_nums(Vector<Integer> newDep_nums) { dep_nums = newDep_nums; }
    public int getMain_num(){ return main_num; }
    public void setMain_num(int new_main){ main_num = new_main; }
    public String getName(){ return name; }
    public void setComplete(boolean status){ complete = complete; }
    public boolean getComplete(){ return complete; }
    public Task getParentTask(){ return parentTask; }
    public int getK(){ return K; }

    public void addDep_task(String new_task, int new_num){
        dep_tasks.add(new_task);
        dep_nums.add(new_num);
        String temp_name = "";
        for (int i = 0; i < dep_tasks.size(); i++){
            if (i == 0){
                temp_name = temp_name + "_{";
            }
            temp_name = temp_name + dep_tasks.get(i);
            if ( i != (dep_tasks.size()-1) ){
                temp_name = temp_name + ", ";
            }
            else {
                temp_name = temp_name + "}";
            }
            name = name + temp_name;
        }

    }

    public void addDep_num(int new_num) { dep_nums.add(new_num); }
}