package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;

public class BookkeepingMessage extends Message {
    private final Message originalMessage;
    private final AgentAddress intendedReceiver;

    public BookkeepingMessage(AgentAddress intendedReceiver, Message originalMessage) {
        this.originalMessage = originalMessage;
        this.intendedReceiver = intendedReceiver;
    }

    public Message getOriginalMessage() { return originalMessage; }
    public AgentAddress getIntendedReceiver() { return intendedReceiver; }
}
