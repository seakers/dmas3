package CCBBA;

import java.awt.*;
import java.util.Vector;

public class SimulatedAgent02 extends SimulatedAbstractAgent {
    @Override
    protected Vector<String> getSensorList(){
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("MW");
        return sensor_list;
    }

    @Override
    protected Dimension getInitialPosition(){
        Dimension position = new Dimension(4,0);
        return position;
    }

    @Override
    protected double getC_merge(){
        return 2.0;
    }

    @Override
    protected double getC_split(){
        return 1.0;
    }

    @Override
    protected double getResources(){ return 100*Math.random(); }

}

