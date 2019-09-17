package CCBBA;

import java.awt.*;
import java.util.Vector;

public class SimulatedAgentRandom extends SimulatedAbstractAgent {
    Vector<String> instrumentList = new Vector<>();

    @Override
    protected Vector<String> getSensorList(){
        // determine instrument list
        this.instrumentList.add("IR");
        this.instrumentList.add("MW");
        //this.instrumentList.add("VIS");

        Vector<String> instruments = new Vector<>();
        int numInstruments = (int) (this.instrumentList.size() * (Math.random()) + 1 );

        while(instruments.size() < numInstruments){
            // guess an instrument
            int i_ins = (int) (this.instrumentList.size() * Math.random());

            // check if it's already in the vector
            if( !instruments.contains(this.instrumentList.get(i_ins)) ){
                // if not, add to vector
                instruments.add(this.instrumentList.get(i_ins));
            }
        }

        return instruments;
    }

    @Override
    protected Dimension getInitialPosition(){
        // Describe initial conditions for agent
        int x_max = 20;
        int y_max = 20;

        int x = (int)(x_max * Math.random());
        int y = (int)(y_max * Math.random());
        Dimension position = new Dimension(x, y);

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
    protected double getResources(){ return 200*Math.random(); }

}
