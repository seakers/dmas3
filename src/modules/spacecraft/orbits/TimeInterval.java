package modules.spacecraft.orbits;

import org.orekit.time.AbsoluteDate;

public class TimeInterval {
    private AbsoluteDate accessStart;
    private AbsoluteDate accessEnd;

    private TimeInterval(AbsoluteDate accessStart, AbsoluteDate accessEnd){
        this.accessStart =  accessStart.getDate();
        this.accessEnd = accessEnd.getDate();
    }

    public TimeInterval(){
        this.accessStart = null;
        this.accessEnd = null;
    }

    public TimeInterval(AbsoluteDate accessStart){
        this.accessStart = accessStart.getDate();
        this.accessEnd = null;
    }

    public TimeInterval copy(){
        return new TimeInterval(this.accessStart, this.accessEnd);
    }

    public AbsoluteDate getAccessStart(){
        return  accessStart;
    }
    public AbsoluteDate getAccessEnd(){
        return accessEnd;
    }
    public void setAccessStart(AbsoluteDate accessStart){ this.accessStart = accessStart.getDate(); }
    public void setAccessEnd(AbsoluteDate accessEnd){
        this.accessEnd = accessEnd.getDate();
    }
}
