package CCBBA.lib;

import org.json.simple.JSONObject;

public class AgentResources {

    /**
     * Properties
     */
    private String type;

    // constant values
    private double C_merge;
    private double C_split;
    private double value;

    // realistic values
    // NEEDS TO BE IMPLEMENTED

    /**
     * Constructor
     */
    public AgentResources(JSONObject inputResourceData) throws Exception{
        // Check for input formatting
        checkInputFormat(inputResourceData);

        // Unpack input data
        unpackInput(inputResourceData);
    }

    private void checkInputFormat(JSONObject inputResourceData){
        if(inputResourceData.get("Type") == null){
            throw new NullPointerException("INPUT ERROR: resource type not contained in input file.");
        }
        else if(inputResourceData.get("Type").toString().equals("Const")){
            // values are constant
            if(inputResourceData.get("MergeCost") == null){
                throw new NullPointerException("INPUT ERROR: merge cost not contained in input file.");
            }
            else if(inputResourceData.get("SplitCost") == null){
                throw new NullPointerException("INPUT ERROR: split cost not contained in input file.");
            }
            else if(inputResourceData.get("Value") == null){
                if(inputResourceData.get("Dist") == null){
                    throw new NullPointerException("INPUT ERROR: resource value not contained in input file.");
                }
                else {
                    if(inputResourceData.get("Max") == null) {
                        throw new NullPointerException("INPUT ERROR: max resource value not contained in input file.");
                    }
                    else if(inputResourceData.get("Min") == null){
                        throw new NullPointerException("INPUT ERROR: min resource value not contained in input file.");
                    }
                }
            }
        }
//        else if(inputResourceData.get("Type").toString().equals("Real")){
//            // values are physical
//            if(inputResourceData.get("MaxPower") == null){
//                throw new NullPointerException("INPUT ERROR: maximum power supply not contained in input file.");
//            }
//        }
    }

    private void unpackInput(JSONObject inputResourceData) throws Exception{
        // -Type
        this.type = inputResourceData.get("Type").toString();

        // -Resources
        if(this.type.equals("Const")){
            // values are constant
            this.C_merge = (double) inputResourceData.get("MergeCost");
            this.C_split = (double) inputResourceData.get("SplitCost");

            if(inputResourceData.get("Value") != null){
                this.value = (double) inputResourceData.get("Value");
            }
            else{
                if(inputResourceData.get("Dist").equals("Linear")){
                    double maxVal = (double) inputResourceData.get("Max");
                    double minVal = (double) inputResourceData.get("Min");
                    this.value = maxVal * Math.random() + minVal;
                }
                else{
                    throw new Exception("INPUT ERROR: agent resource distribution not supported.");
                }
            }
        }
//        else if(inputResourceData.get("Type").toString().equals("Real")){
//            // values are physical
//
//        }
        else{
            throw new Exception("INPUT ERROR: agent resource type not supported.");
        }
    }
}
