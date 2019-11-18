package CCBBA.scenarios.figure3;

import java.awt.*;
import java.util.Vector;
import CCBBA.bin.*;

public class ValidationAgentMod01 extends SimulatedAbstractAgent {
    @Override
    protected Vector<String> getSensorList() {
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("IR");
        return sensor_list;
    }

    @Override
    protected Dimension getInitialPosition() {
        // Describe initial conditions for agent
        int x_max = 50;
        int y_max = 50;

        int x = (int)(x_max * Math.random());
        int y = (int)(y_max * Math.random());

        return new Dimension(x, y);
    }

    @Override
    protected double getC_merge() {
        return 0.0;
    }

    @Override
    protected double getC_split() {
        return 0.0;
    }

    @Override
    protected double getResources() {
        return 35 + 15 * Math.random();
    }

    @Override
    protected double setMiu(){ return readResources() * 0.0/100; }

    @Override
    protected int getM(){ return 2; }

    @Override
    public int getO_kq(){
        return 2;
    }

    @Override
    public int getW_solo_max(){
        return 2;
    }

    @Override
    public int getW_any_max(){ return 5; }
}