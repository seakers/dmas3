package modules.spacecraft.component;

public class ADCS extends Component{
    public ADCS(double power, double mass) {
        super(power, mass);
    }

    @Override
    Component copy() {
        return new ADCS(this.power, this.mass);
    }
}
