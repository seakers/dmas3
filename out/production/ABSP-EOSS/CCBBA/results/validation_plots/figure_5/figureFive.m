function [] = figureFive( results )

    %- Data for subplot 1
    plot1.name = {};
    plot1.data = {};
    for i = 1:length(results.name)
        plot1.name{i} = results.name{i};        
        scoreAchieved = results.values{i}(:,3);
        scoreAvailable = results.values{i}(:,4);
        
        plot1.data{i} = scoreAchieved ./ scoreAvailable;
    end
    
    for i = 1:length( plot1.data )
       plot1.avg(i) = mean(plot1.data{i}); 
       plot1.min(i) = mean(plot1.data{i}) - min(plot1.data{i});
       plot1.max(i) = max(plot1.data{i}) - mean(plot1.data{i});
       plot1.std(i) = std(plot1.data{i});
    end 
    plot1.x = [0.0 10.0 20.0 30.0 40.0 50.0 100.0 ];
    
    %- Data for subplot 2
    plot2.name = {};
    plot2.data = {};
    
    for i = 1:length(results.name)
        plot2.tasksDone{i} = results.values{i}(:,10:12);
    end 
    
    for i = 1:length(plot2.tasksDone)
       plot2.e1.avg(i) =  mean(plot2.tasksDone{i}(:,1));
       plot2.e1.min(i) =  mean(plot2.tasksDone{i}(:,1)) - min(plot2.tasksDone{i}(:,1));
       plot2.e1.max(i) =  max(plot2.tasksDone{i}(:,1)) - mean(plot2.tasksDone{i}(:,1));
       
       plot2.e2.avg(i) =  mean(plot2.tasksDone{i}(:,2));
       plot2.e2.min(i) =  mean(plot2.tasksDone{i}(:,2)) - min(plot2.tasksDone{i}(:,2));
       plot2.e2.max(i) =  max(plot2.tasksDone{i}(:,2)) - mean(plot2.tasksDone{i}(:,2));
       
       plot2.e3.avg(i) =  mean(plot2.tasksDone{i}(:,3));
       plot2.e3.min(i) =  mean(plot2.tasksDone{i}(:,3)) - min(plot2.tasksDone{i}(:,3));
       plot2.e3.max(i) =  max(plot2.tasksDone{i}(:,3)) - mean(plot2.tasksDone{i}(:,3));
    end
    plot2.x = plot1.x;
    
    % Plot results
    figure
    
    %- Plot 1
    subplot(2,1,1)
    errorbar(plot1.x, plot1.avg, plot1.min, plot1.max);
    grid on
    title("Score as a function of Merge Cost (Split cost = 0)")
    ylabel({'Coalitions Formed /'; 'Coalitions Available (%)'})
    ylim([.25 .6])
    
    %- Plot 2
    subplot(2,1,2)
    title("Number of coalitions as a function of Merge Cost")
    hold on
    grid on
    errorbar(plot2.x, plot2.e1.avg, plot2.e1.min, plot2.e1.max);
    errorbar(plot2.x, plot2.e2.avg, plot2.e2.min, plot2.e2.max);
    errorbar(plot2.x, plot2.e3.avg, plot2.e3.min, plot2.e3.max);
    ylabel({'Score Achieved /'; 'Score of the Scenario'})
    xlabel("Merge Cpst (% total resources per agent)")
    legend("Tasks done with 1 sensor", "Tasks done with 2 sensors", "Tasks done with 3 sensors", 'Location', 'Best')
end

