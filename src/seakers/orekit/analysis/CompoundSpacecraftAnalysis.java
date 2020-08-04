/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis;

import java.util.ArrayList;
import java.util.Collection;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This class allows multiple analyses on the same spacecraft so that the
 * spacecraft only needs to be propagated once as opposed to once per analysis.
 * All analyses will be conducted with the time step and start/end date given to
 * construct this analysis
 *
 * @author nozomihitomi
 */
public class CompoundSpacecraftAnalysis extends AbstractSpacecraftAnalysis<Object> {

    /**
     * G
     */
    private final Collection<AbstractSpacecraftAnalysis<?>> analyses;

    public CompoundSpacecraftAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            double timeStep, Satellite sat, PropagatorFactory propagatorFactory,
            Collection<AbstractSpacecraftAnalysis<?>> analyses) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.analyses = analyses;
    }

    /**
     * Traverses all analyses recursively and obtains all analyses under this
     * compound analysis
     *
     * @return
     */
    public Collection<AbstractSpacecraftAnalysis<?>> getAnalyses() {
        ArrayList<AbstractSpacecraftAnalysis<?>> out = new ArrayList<>();
        for (AbstractSpacecraftAnalysis<?> a : analyses) {
            if (a instanceof CompoundSpacecraftAnalysis) {
                out.addAll(((CompoundSpacecraftAnalysis) a).getAnalyses());
            } else {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        for (Analysis<?> a : analyses) {
            a.handleStep(currentState, isLast);
        }
    }

    @Override
    public Collection<Record<Object>> getHistory() {
        throw new UnsupportedOperationException("Cannot obtain history from compound analysis.");
    }

    @Override
    public String getExtension() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        String name="CompoundAnalysis";
        for (Analysis<?> anal:this.getAnalyses()){
            name=name.concat(String.format("_%s",anal.getName()));
        }
        return name;
    }

}
