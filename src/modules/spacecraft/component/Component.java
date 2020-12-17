package modules.spacecraft.component;

public abstract class Component {
    protected double power = -1.0;
    protected double mass = -1.0;

    public Component(double power, double mass){
        this.power = power;
        this.mass = mass;
    }

    abstract Component copy();
}
