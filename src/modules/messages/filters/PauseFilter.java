package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.messages.PauseMessage;

public class PauseFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getClass().equals(PauseMessage.class);
    }
}
