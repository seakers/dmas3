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
    %- Plot 1
    subplot(3,1,1)
    yPlot1 = [plot1.data{2}, plot1.data{3}, plot1.data{4}, plot1.data{5}, plot1.data{6}, plot1.data{7}, plot1.data{1}];
    xPlots = ["0.0", "2.0", "4.0", "6.0", "8.0", "inf", "unrestricted"];
    boxplot( yPlot1, xPlots )
    ylabel({'Coalitions Formed /'; 'Coalitions Available (%)'})
    title("Constraint/Moduar")
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
%     ylim([0 100])
    
    %- Plot 2
    subplot(3,1,2)
    yPlot2 = [plot2.data{2}, plot2.data{3}, plot2.data{4}, plot2.data{5}, plot2.data{6}, plot2.data{7}, plot2.data{1}];
    xPlots = ["0.0", "2.0", "4.0", "6.0", "8.0", "inf", "unrestricted"];
    boxplot( yPlot2, xPlots )
    ylabel({'Score Achieved /'; 'Score of the Scenario'})
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
%     ylim([0 1])
    
    %- Plot 3
    subplot(3,1,3)
    yPlot3 = [plot3.data{2}, plot3.data{3}, plot3.data{4}, plot3.data{5}, plot3.data{6}, plot3.data{7}, plot3.data{1}];
    xPlots = ["0.0", "2.0", "4.0", "6.0", "8.0", "inf", "unrestricted"];
    boxplot( yPlot3, xPlots )
    ylabel({'Total Cost /'; 'Resources per Agent'})
    xlabel("Allowed Waiting Time [t_{corr}(s)]")
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
%     ylim([0.0 1])
    
    % Additional plots
    %- Data for subplot 1
    plot4.name = {};
    plot4.data = {};
    
    for i = 1:length(results.name)
        plot4.name{i} = results.name{i};        
        tasksDone = results.values{i}(:,10:12);
        totalTasks = results.values{i}(:,8);   
        
        plot4.data{i,1} = mean( tasksDone(:,1)./totalTasks );
        plot4.data{i,2} = mean( tasksDone(:,2)./totalTasks );
        plot4.data{i,3} = mean( tasksDone(:,3)./totalTasks );
    end
    
    x =1;
    
    figure
%     subplot(3,1,1)
    yPlots4 = [ [plot4.data{2,:}]; [plot4.data{3,:}]; [plot4.data{4,:}]; [plot4.data{5,:}]; [plot4.data{6,:}]; [plot4.data{7,:}]; [plot4.data{1,:}]];
%     xPlots4 = ["0.0", "2.0", "4.0", "6.0", "8.0", "inf", "unrestricted"];
    xPlots4 = [0.0, 2.0, 4.0, 6.0, 8.0, 10, 12];
    bar( xPlots4, yPlots4 )
%     bar( yPlots4 )
    ylabel({"Average task done with n sensor/";"total tasks done"});
    xlabel("Allowed Waiting Time [t_{corr}(s)]")
    legend("Tasks done with 1 sensor","Tasks done with 2 sensor","Tasks done with 3 sensor")
    set(gca, 'YGrid', 'on', 'XGrid', 'off')
    ylim([0.0 1])
end