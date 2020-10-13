function [] = drawUtilityVsArchs(n)
    m = 5;
    data_all = {};
    
    data_1 = readResults("SMAP-ASCEND",n);
    data_2 = readResults("BIOMASS-ASCEND",n);
    data_3 = readResults("SENTINEL-ASCEND",n);
    data_4 = readResults("ASCEND_NoColab-ASCEND",n);
    data_5 = readResults("ASCEND_ALL-ASCEND",n);
    
    data_all{1} = data_1;
    data_all{2} = data_2;
    data_all{3} = data_3;
    data_all{4} = data_4;
    data_all{5} = data_5;
    
    utility = zeros(n,m);
    score = zeros(n,m);
    scoreTotal = zeros(n,m);
    
    spatial = zeros(n,m);
    snr = zeros(n,m);
    temporal = zeros(n,m);
    
    spatialSat = zeros(n,m);
    snrSat = zeros(n,m);
    temporalSat = zeros(n,m);
    
    coals = zeros(n,m);
    n_measurements = zeros(n,m);
    for i = 1:m
        utility(:,i) = data_all{i}(:,1)./data_all{i}(:,5)*100;
        score(:,i)= data_all{i}(:,4)./data_all{i}(:,5)*100;
        scoreTotal(:,i)= data_all{i}(:,4);
        
        spatial(:,i) = data_all{i}(:,16)/1000;
        snr(:,i) = data_all{i}(:,17);
        temporal(:,i) = data_all{i}(:,18)/60;
        
        spatialSat(:,i) = data_all{i}(:,11);
        snrSat(:,i) = data_all{i}(:,12);
        temporalSat(:,i) = data_all{i}(:,13);
        
        coals(:,i) = data_all{i}(:,2)./data_all{i}(:,3)*100;
        n_measurements(:,i) = data_all{i}(:,14)./30;
    end
    
    x = {'SMAP', 'BIOMASS', 'SENTINEL-1', 'All No Coals', 'All'};
    
    % Utility Plot
    figure
    boxplot(utility,x);
    title("Utility vs System Architecture")
    grid on;
    ylabel({'Utility Scored/' ; 'Score Available [%]'});
    xlabel('Architecture Selected');
    ylim([0,100])
    
    % Score Percentage Plot
    figure
    boxplot(score,x);
    title("Score Achieved vs System Architecture")
    grid on;
    ylabel({'Score Achieved/' ; 'Score Available [%]'});
    xlabel('Architecture Selected');
    ylim([0,100])
    
    % Score Plot
    figure
    boxplot(scoreTotal,x);
    title("Total Score Achieved vs System Architecture")
    grid on;
    ylabel({'Score Achieved'});
    xlabel('Architecture Selected');
    ylim([0,100])
    
    % Performance Plots
    figure
    subplot(3,1,1);
    boxplot(spatial);
    title({'System Performance vs'; 'System Architecture'})
    grid on;
    ylabel({'Average Spatial', 'Resolution [km]'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,2);
    boxplot(snr);
    grid on;
    ylabel({'Average SNR [dB]'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,3);
    boxplot(temporal,x);
    grid on;
    ylabel({'Average Revisit','Time [min]'});
    xlabel('Architecture Selected')
    
    % Requirement Satisfaction Plots
    figure
    subplot(3,1,1);
    boxplot(spatialSat);
    title({'Requirement Satisfaction vs'; 'System Architecture'})
    ylim([0,1])
    grid on;
    ylabel({'Average Spatial'; 'Resolution Satisfaction'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,2);
    boxplot(snrSat);
    grid on;
    ylabel({'Average Signal-to-Noise' ; 'Ratio Satisfaction'});
    set(gca,'xticklabel',{[]})
    ylim([0,1])
    subplot(3,1,3);
    boxplot(temporalSat,x);
    grid on;
    ylabel({'Average Revisit';'Time Satisfaction'});
    xlabel('Architecture Selected')
    ylim([0,1])
    
    % Number of Measurements
    figure
    boxplot(n_measurements,x);
    title("Number of Measurements vs System Architecture")
    grid on;
    ylabel({'Number of measurements/' ; 'Tasks Available'});
    xlabel('Architecture Selected');
end