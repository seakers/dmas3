package modules.antennas;

public class ParabolicAntenna extends AbstractAntenna{

    public ParabolicAntenna(double D,double f) {
        super(AbstractAntenna.PARAB, f);
        dimensions.add(D);
    }

    public double getGain(){
        double D = dimensions.get(0);
        double lambda = 3e8 / frequency;
        return Math.pow(Math.PI * D / lambda , 2) * 0.60;
    }
}
