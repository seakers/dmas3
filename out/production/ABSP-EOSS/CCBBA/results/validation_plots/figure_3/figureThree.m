function [] = figureThree( results )
    % Extract relevant data from results
    
    %- Data for subplot 1
    plot1.name = {};
    plot1.data = {};
    for i = 1:length(results.name)
        plot1.name{i} = results.name{i};        
        coalsFormed = results.values{i}(:,1);
        coalsAvailable = results.values{i}(:,2);
        
        plot1.data{i} = coalsFormed ./ coalsAvailable * 100;
    end
    
    %- Data for subplot 2
    plot2.name = {};
    plot2.data = {};
    for i = 1:length(results.name)
        plot2.name{i} = results.name{i};        
        scoreAchieved = results.values{i}(:,3);
        scoreAvailable = results.values{i}(:,4);
        
        plot2.data{i} = scoreAchieved ./ scoreAvailable;
    end
    
    %- Data for subplot 3
    plot3.name = {};
    plot3.data = {};
    for i = 1:length(results.name)
        plot3.name{i} = results.name{i};        
        resourcesPerCostPerAgent = results.values{i}(:,5);
        
        plot3.data{i} = resourcesPerCostPerAgent;
    end
    
    % Plot results
    figure
    title("Constraint/Moduar")
    %- Plot 1
    subplot(3,1,1)
    boxplot([plot1.data{2}, plot1.data{3}, plot1.data{4}, plot1.data{5}, plot1.data{6}, plot1.data{7}, plot1.data{1}])
    ylabel({'Coalitions Formed /'; 'Coalitions Available (%)'})
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    ylim([0 100])
    
    %- Plot 2
    subplot(3,1,2)
    boxplot([plot2.data{2}, plot2.data{3}, plot2.data{4}, plot2.data{5}, plot2.data{6}, plot2.data{7}, plot2.data{1}])
    ylabel({'Score Achieved /'; 'Score of the Scenario'})
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    ylim([0.2 0.8])
    
    %- Plot 3
    subplot(3,1,3)
    boxplot([plot3.data{2}, plot3.data{3}, plot3.data{4}, plot3.data{5}, plot3.data{6}, plot3.data{7}, plot3.data{1}])
    ylabel({'Total Cost /'; 'Resources per Agent'})
    xlabel("Allowed Waiting Time [t_{corr}(s)]")
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    ylim([0 1])
end
