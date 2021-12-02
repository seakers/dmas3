package modules.messages;

import org.orekit.time.AbsoluteDate;

public class PauseMessage extends DMASMessage {
    public PauseMessage(AbsoluteDate sendDate){
        super(sendDate, null, null);
    }
}
