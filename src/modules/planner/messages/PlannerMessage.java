package modules.planner.messages;

import madkit.kernel.Message;
import modules.planner.plans.Plan;

public class PlannerMessage extends Message {
    private Plan plan;

    public PlannerMessage(Plan plan){
        this.plan = plan.copy();
    }
    public Plan getPlan(){return plan;}
}
