function [] = figureThree( results )
    % Extract relevant data from results
    
    %- For subplot 1
    plot1.name = {};
    plot1.data = {};
    for i = 2:length(results.name)
        plot1.name{i - 1} = results.name{i};        
        coalsFormed = results.values{i}(:,1);
        coalsAvailable = results.values{i}(:,2);
        
        plot1.data{i - 1} = coalsFormed ./ (coalsAvailable / 2) * 100;
    end
    
    %- For subplot 2
    plot2.name = {};
    plot2.data = {};
    for i = 2:length(results.name)
        plot2.name{i - 1} = results.name{i};        
        scoreAchieved = results.values{i}(:,3);
        scoreAvailable = results.values{i}(:,4);
        
        plot2.data{i - 1} = scoreAchieved ./ scoreAvailable;
    end
    
    %- For subplot 3
    plot3.name = {};
    plot3.data = {};
    for i = 2:length(results.name)
        plot3.name{i - 1} = results.name{i};        
        resourcesPerCostPerAgent = results.values{i}(:,5);
        
        plot3.data{i - 1} = resourcesPerCostPerAgent;
    end
    
    % Plot results
    figure
    title("Constraint/Moduar")
    %- Plot 1
    subplot(3,1,1)
    boxplot([plot1.data{2}, plot1.data{3}, plot1.data{4}, plot1.data{5}, plot1.data{6}])
    ylabel({'Coalitions Formed /'; 'Coalitions Available (%)'})
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    ylim([0 100])
    
    %- Plot 2
    subplot(3,1,2)
    boxplot([plot2.data{2}, plot2.data{3}, plot2.data{4}, plot2.data{5}, plot2.data{6}])
    ylabel({'Score Achieved /'; 'Score of the Scenario'})
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    
    %- Plot 3
    subplot(3,1,3)
    boxplot([plot3.data{2}, plot3.data{3}, plot3.data{4}, plot3.data{5}, plot3.data{6}])
    ylabel({'Total Cost /'; 'Resources per Agent'})
    xlabel("Allowed Waiting Time [t_{corr}(s)]")
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
end

