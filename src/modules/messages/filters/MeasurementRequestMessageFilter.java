package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.measurements.MeasurementRequest;
import modules.messages.BookkeepingMessage;
import modules.messages.MeasurementMessage;
import modules.messages.MeasurementRequestMessage;

public class MeasurementRequestMessageFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getClass().equals(MeasurementRequestMessage.class);
    }
}
