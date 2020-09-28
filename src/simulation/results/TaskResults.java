package simulation.results;

import modules.environment.Task;
import modules.environment.TaskCapability;
import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementCapability;
import org.orekit.time.AbsoluteDate;

import java.awt.desktop.AboutEvent;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskResults {
    private Task mainTask;
    private ArrayList<AbsoluteDate> dates = new ArrayList<>();
    private ArrayList<IterationDatum> datumsOrdered = new ArrayList<>();
    private double resSat;
    private double snrSat;
    private double revisitSat;

    public TaskResults(TaskCapability capability) throws Exception {
        capability.calcReqSat();

        mainTask = capability.getParentTask();
        dates = getOrderedDates(capability);
        datumsOrdered = getOrderedData(capability);
        resSat = capability.getResSat();
        snrSat = capability.getSnrSat();
        revisitSat = capability.getRevSat();
    }

    public ArrayList<IterationDatum> getOrderedData(TaskCapability capability) {
        ArrayList<IterationDatum> datumsOrdered = new ArrayList<>();

        for(AbsoluteDate date : dates){
            for(Measurement measurement : capability.getCapabilities().keySet()){
                MeasurementCapability cap = capability.getCapabilities().get(measurement);

                boolean found = false;
                for(IterationDatum dat : cap.getPlannerBids()){
                    if(dat.getTz().compareTo(date) == 0 && !datumsOrdered.contains(dat)){
                        datumsOrdered.add(dat);
                        found = true;
                        break;
                    }
                }

                if(found) break;
            }
        }

        return datumsOrdered;
    }

    public ArrayList<AbsoluteDate> getOrderedDates(TaskCapability capability){
        ArrayList<AbsoluteDate> datesNoOrder = new ArrayList<>();
        ArrayList<AbsoluteDate> datesOrder = new ArrayList<>();

        for(Measurement measurement : capability.getCapabilities().keySet()){
            MeasurementCapability cap = capability.getCapabilities().get(measurement);

            for(IterationDatum dat : cap.getPlannerBids()){
                datesNoOrder.add(dat.getTz());
            }
        }

        while(datesNoOrder.size() > 0){
            AbsoluteDate dateMin = datesNoOrder.get(0);
            for(AbsoluteDate date_j : datesNoOrder){
                if(date_j.compareTo(dateMin) < 0){
                    dateMin = date_j.getDate();
                }
            }
            datesOrder.add(dateMin);
            datesNoOrder.remove(dateMin);
            int x = 1;
        }

        return datesOrder;
    }

    public String toString(){
        StringBuilder results = new StringBuilder();
        results.append(mainTask.getName() + ":\n");
        results.append("Spatial Resolution Satisfaction:\t\t" + resSat + "\n" +
                       "Signal-to-Noise Ratio Satisfaction:\t" + snrSat + "\n" +
                       "Temporal Resolution Satisfaction:\t\t" + revisitSat + "\n" +
                       "Number of Measurements:\t" + dates.size() + "\n");

        for(int i = 0; i < dates.size(); i++){
            IterationDatum dat = datumsOrdered.get(i);
            results.append( "\t\t" + dat.getSubtask() + "\t\t{" + dat.getTz() + "}\t" + ((Spacecraft) dat.getZ()).getName() + "\n");
        }

        results.append("---------------------------------------------------------------------------------\n");
        return results.toString();
    }
}
