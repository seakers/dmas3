package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.messages.BookkeepingMessage;
import modules.messages.MeasurementMessage;

public class MeasurementMessageFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        if(message.getClass().equals(BookkeepingMessage.class)){
            Message bookKeepingMessage = ((BookkeepingMessage) message).getOriginalMessage();
            return bookKeepingMessage.getClass().equals(MeasurementMessage.class);
        }
        return false;
    }
}
