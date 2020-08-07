package modules.agents;

import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class TaskAccess {
    private ArrayList<TimeInterval> access;

    public TaskAccess(){
        this.access = new ArrayList<>();
    }

    public void addAccess(AbsoluteDate accessStart, AbsoluteDate accesEnd){
        TimeInterval newAccess = new TimeInterval(accessStart, accesEnd);
        addAccess(newAccess);
    }
    public void addAccess(TimeInterval newAccess){
        this.access.add(newAccess);
    }

    public ArrayList<TimeInterval> getAccess(){
        return access;
    }
}
