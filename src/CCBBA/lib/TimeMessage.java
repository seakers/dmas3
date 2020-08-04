package CCBBA.lib;

import madkit.kernel.Message;

public class TimeMessage extends Message {
    private double time;

    public TimeMessage(double newTime){ this.time = newTime; }
    public double getTime(){ return time; }
}
