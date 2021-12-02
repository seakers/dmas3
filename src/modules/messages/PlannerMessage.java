package modules.messages;

import org.orekit.time.AbsoluteDate;

public class PlannerMessage extends DMASMessage {
    public PlannerMessage(AbsoluteDate sendDate){
        super(sendDate, null, null);
    }
}
