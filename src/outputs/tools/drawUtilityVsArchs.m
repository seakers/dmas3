function [] = drawUtilityVsArchs(n)
    m = 5;
    data_all = {};
    
    data_1 = readResults("SMAP-ASCEND",n);
    data_2 = readResults("BIOMASS-ASCEND",n);
    data_3 = readResults("SENTINEL-ASCEND",n);
    data_4 = readResults("ASCEND_All-ASCEND",n);
    data_5 = readResults("ASCEND_All_NO_COALS-ASCEND",n);
    
    data_all{1} = data_1;
    data_all{2} = data_2;
    data_all{3} = data_3;
    data_all{4} = data_4;
    data_all{5} = data_5;
    
    utility = zeros(n,m);
    spatial = zeros(n,m);
    snr = zeros(n,m);
    temporal = zeros(n,m);
    coals = zeros(n,m);
    for i = 1:m
        utility(:,i) = data_all{i}(:,1)./data_all{i}(:,5)*100;
        spatial(:,i) = data_all{i}(:,11)*100;
        snr(:,i) = data_all{i}(:,12)*100;
        temporal(:,i) = data_all{i}(:,13)*100;
        coals(:,i) = data_all{i}(:,2)./data_all{i}(:,3)*100;
    end
    
    x = ['SMAP', 'BIOMASS', 'SENTINEL-1', 'All no coals', 'All'];
    
    % Utility Plot
    figure
    boxplot(utility,x);
    title("Utility vs System Architecture")
    grid on;
    ylabel({'Utility Scored/' ; 'Score Available [%]'});
    xlabel('Architecture Selected');
    
    % Requirement Satisfaction Plot
    figure
    subplot(3,1,1);
    boxplot(spatial);
    title({'Requirement Satisfaction vs'; 'System Architecture'})
    grid on;
    ylabel({'Average Spatial'; 'Resolution Satisfaction'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,2);
    boxplot(snr);
    grid on;
    ylabel({'Average Signal-to-Noise' ; 'Ratio Satisfaction'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,3);
    boxplot(temporal,x);
    grid on;
    ylabel({'Average Temporal';'Resoution Satisfaction'});
    xlabel('Architecture Selected')
    
    % Coalitions formed Plot
    figure 
    boxplot(utility(:,4:5),x(4:5));
    title({'Coalitios Formed vs'; 'System Architecture'})
    grid on;
    ylabel({'Coalitions Formed'; 'Coalitions Availabe [%]'});
    xlabel('Architecture Selected')
end