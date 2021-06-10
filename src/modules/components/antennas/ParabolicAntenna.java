package modules.components.antennas;

public class ParabolicAntenna extends AbstractAntenna{

    public ParabolicAntenna(double power, double mass, double x_dim, double y_dim, double z_dim,double D,double f) {
        super(power, mass, x_dim, y_dim, z_dim, AbstractAntenna.PARAB, f);
        dims.add(D);
    }

    public double getGain(){
        double D = dims.get(0);
        double lambda = 3e8 / frequency;
        return Math.pow(Math.PI * D / lambda , 2) * 0.60;
    }
}
