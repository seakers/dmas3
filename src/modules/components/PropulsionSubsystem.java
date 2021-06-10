package modules.components;

public class PropulsionSubsystem extends AbstractSubsystem {
    private double strMass;
    private double propMass;

    public PropulsionSubsystem(String name, double power, double strMass, double propMass, double x_dim, double y_dim, double z_dim) {
        super(name, power, strMass + propMass, x_dim, y_dim, z_dim);
    }

    public double getStrMass(){
        return strMass;
    }
    public double getPropMass(){
        return propMass;
    }
}
