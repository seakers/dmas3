package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.messages.MeasurementMessage;

public class MeasurementFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getClass().equals(MeasurementMessage.class);
    }
}
