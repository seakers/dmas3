package seakers.orekit.multiagent.CCBBA;

import java.awt.*;
import java.util.Vector;

public class SimulatedAgent01 extends SimulatedAbstractAgent {
    @Override
    protected Vector<String> getSensorList(){
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("IR");
        return sensor_list;
    }

    @Override
    protected Dimension getInitialPosition(){
        Dimension position = new Dimension(0,0);
        return position;
    }
}
