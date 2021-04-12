package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.messages.MeasurementRequestMessage;
import modules.messages.RelayMessage;

public class RelayMessageFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getClass().equals(RelayMessage.class);
    }
}
