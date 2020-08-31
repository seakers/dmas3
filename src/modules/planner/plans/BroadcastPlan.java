package modules.planner.plans;

import modules.planner.messages.*;

public class BroadcastPlan extends Plan{
    private CCBBAResultsMessage resultsMessage;

    public BroadcastPlan(CCBBAResultsMessage resultsMessage){
        this.resultsMessage = resultsMessage;
    }

    @Override
    public Plan copy() {
        return new BroadcastPlan(this.resultsMessage);
    }
}
