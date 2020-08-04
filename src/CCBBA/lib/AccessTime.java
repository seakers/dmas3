package CCBBA.lib;

import org.orekit.time.AbsoluteDate;

public class AccessTime {
    private AbsoluteDate accessStart;
    private AbsoluteDate accessEnd;

    public AccessTime(AbsoluteDate accessStart, AbsoluteDate accessEnd){
        this.accessStart = accessStart;
        this.accessEnd = accessEnd;
    }

    public AbsoluteDate getAccessStart() { return accessStart; }
    public AbsoluteDate getAccessEnd() { return accessEnd; }
}
