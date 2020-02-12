package CCBBA.lib;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.SchedulingMessage;

import java.util.ArrayList;
import java.util.List;

public class ResultsCompiler extends AbstractAgent {
    private Scenario environment;                           // world environment

    @Override
    protected void activate(){
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE);
    }

    private void checkResults() throws Exception {
        //Receive results
        List<AgentAddress> resultsAddress = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
        List<AgentAddress> agentsEnvironment = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_EXIST);

        if( (resultsAddress != null)&&(resultsAddress.size() == agentsEnvironment.size()) ){
            List<Message> receivedMessages = nextMessages(null);
            ArrayList<IterationResults> receivedResults = new ArrayList<>();
            ArrayList<String> senderList = new ArrayList<>();

            for (int i = 0; i < receivedMessages.size(); i++) {
                ResultsMessage message = (ResultsMessage) receivedMessages.get(i);

                if(!senderList.contains(message.getSenderName()) ) {
                    senderList.add(message.getSenderName());
                    receivedResults.add(message.getResults());
                }
            }

            if (receivedResults.size() >= agentsEnvironment.size()) { // checks if every agent has sent finished its tasks.
                // Every agent has finished its tasks
                getLogger().info("Terminating Sim. Saving Results.");

                // print results
//                printResults(receivedResults);

                // terminate sim
                SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
                sendMessage(getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE), terminate);
            }
        }
    }
}
