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
    private double miu;
    private SimulatedAgent parentAgent;

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
            else if(inputResourceData.get("TravelCost") == null){
                throw new NullPointerException("INPUT ERROR: Travel cost not contained in input file.");
            }
            else if(inputResourceData.get("TravelCostType") == null){
                throw new NullPointerException("INPUT ERROR: Travel cost type not contained in input file.");
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

            if(inputResourceData.get("TravelCostType").toString().equals("Const")){
                this.miu = (double) inputResourceData.get("TravelCost");
            }
            else if(inputResourceData.get("TravelCostType").toString().equals("Proportional")){
                this.miu = (double) inputResourceData.get("TravelCost");
                this.miu = this.miu * this.value;
            }
            else{
                throw new Exception("INPUT ERROR: agent travel cost type not supported.");
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

    public boolean checkResources(String worldType) throws Exception {
        if(worldType.equals("2D_Grid") || worldType.equals("3D_Grid")){
            return (this.value > 0);
        }
        else if(worldType.equals("3D_Earth")){
            return (this.value > 0);
        }
        else{
            throw new Exception("World type not supported.");
        }
    }

    public void deductCost(IterationDatum datum, String worldType, double currentTravelCost) throws Exception {
        if(worldType.equals("2D_Grid") || worldType.equals("3D_Grid")){
            this.value -= datum.getCost() - currentTravelCost;
        }
        else if(worldType.equals("3D_Earth")){
            this.value -= datum.getCost();
        }
        else{
            throw new Exception("World type not supported.");
        }
    }

    public void deductTravelCost(double travelCost, String worldType) throws Exception{
        if(worldType.equals("2D_Grid") || worldType.equals("3D_Grid")){
            this.value -= travelCost;
        }
        else{
            throw new Exception("World type not supported.");
        }
    }

    public void restoreTravelCost(double travelCost, String worldType)throws Exception{
        if(worldType.equals("2D_Grid") || worldType.equals("3D_Grid")){
            this.value += travelCost;
        }
        else{
            throw new Exception("World type not supported.");
        }
    }

    public double getC_merge() { return C_merge; }
    public double getC_split() { return C_split; }
    public double getValue() { return value; }
    public double getMiu() { return miu; }
    public String getType(){ return type;}
}
