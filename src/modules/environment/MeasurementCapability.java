package modules.environment;

import madkit.kernel.AbstractAgent;
import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementCapability {
    private ArrayList<AbstractAgent> winners = new ArrayList<>();
    private ArrayList<Subtask> parentSubtasks = new ArrayList<>();
    private ArrayList<ArrayList<Instrument>> instrumentsUsed = new ArrayList<>();
    private Measurement measurement;
    private Requirements requirements;
    private ArrayList<MeasurementPerformance> performance = new ArrayList<>();
    private ArrayList<IterationDatum> plannerBids = new ArrayList<>();

    // aggregated performance values
    private double totalUtility;
    private int numMeasurements;
    private ArrayList<AbsoluteDate> measurementDates = new ArrayList<>();


    public MeasurementCapability(Measurement measurement){
        winners = new ArrayList<>();
        parentSubtasks = new ArrayList<>();
        instrumentsUsed = new ArrayList<>();
        this.measurement = measurement;
        requirements = null;
        performance = new ArrayList<>();
        plannerBids = new ArrayList<>();
        totalUtility = 0.0;
        numMeasurements = 0;
        measurementDates = new ArrayList<>();
    }

    public MeasurementCapability(Subtask subtask, ArrayList<Instrument> instrumentsUsed, Measurement measurement,
                                 Requirements requirements, MeasurementPerformance performance, IterationDatum plannerBid,
                                 AbstractAgent winner) {
        this.winners.add(winner);
        this.parentSubtasks.add(subtask);
        this.instrumentsUsed.add(instrumentsUsed);
        this.measurement = measurement;
        this.requirements = requirements;
        this.performance = new ArrayList<>(); this.performance.add(performance.copy());
        this.plannerBids.add(plannerBid.copy());

        totalUtility = plannerBid.getY();
        numMeasurements = 1;
        measurementDates.add(plannerBid.getTz());
    }

    public void update(MeasurementCapability newCapability){
        this.totalUtility += newCapability.getTotalUtility();
        this.numMeasurements += 1;
        this.measurementDates.addAll(newCapability.getMeasurementDates());

        this.winners.addAll(newCapability.getWinners());
        this.parentSubtasks.addAll(newCapability.getParentSubtasks());
        this.instrumentsUsed.addAll(newCapability.getInstrumentsUsed());
        this.performance = new ArrayList<>(); this.performance.addAll(newCapability.getPerformance());
        this.plannerBids.addAll(newCapability.getPlannerBids());
    }

    public ArrayList<AbstractAgent> getWinners(){return winners;}

    public ArrayList<Subtask> getParentSubtasks() {
        return parentSubtasks;
    }

    public ArrayList<ArrayList<Instrument>> getInstrumentsUsed() {
        return instrumentsUsed;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public Requirements getRequirements() {
        return requirements;
    }

    public ArrayList<MeasurementPerformance> getPerformance(){return performance;}

    public ArrayList<IterationDatum> getPlannerBids(){return this.plannerBids;}

    public double getTotalUtility(){return this.totalUtility;}

    public ArrayList<AbsoluteDate> getMeasurementDates(){return this.measurementDates;}

    public int getNumMeasurements(){return numMeasurements;}
}
