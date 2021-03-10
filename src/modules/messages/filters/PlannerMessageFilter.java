package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.messages.PlannerMessage;

public class PlannerMessageFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getClass().equals(PlannerMessage.class);
    }
}