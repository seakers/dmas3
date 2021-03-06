package modules.antennas;

public class ParabolicAntenna extends AbstractAntenna{

    public ParabolicAntenna(double D) {
        super(AbstractAntenna.PARAB);
        dimensions.add(D);
    }
}
