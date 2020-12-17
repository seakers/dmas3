package modules.spacecraft.component;

public class EPS extends Component {

    public EPS(double power, double mass) {
        super(power, mass);
    }

    public EPS copy(){
        return new EPS(this.power, this.mass);
    }
}
