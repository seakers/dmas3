package modules.agents;

import org.orekit.time.AbsoluteDate;

public class TimeInterval {
    private AbsoluteDate accessStart;
    private AbsoluteDate accessEnd;

    public TimeInterval(AbsoluteDate accessStart, AbsoluteDate accessEnd){
        this.accessStart =  accessStart.getDate();
        this.accessEnd = accessEnd.getDate();
    }

    public AbsoluteDate getAccessStart(){
        return  accessStart;
    }
    public void setAccessStart(AbsoluteDate accessStart){
        this.accessStart = accessStart.getDate();
    }
    public AbsoluteDate getAccessEnd(){
        return accessEnd;
    }
    public void setAccessEnd(AbsoluteDate accessEnd){
        this.accessEnd = accessEnd.getDate();
    }
}
