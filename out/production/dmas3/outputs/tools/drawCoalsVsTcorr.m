function [] = drawCoalsVsTcorr(n)
    data_all = {};
    
    data_0 = readResults("CoalsVsTcorr_0",n);
    data_30 = readResults("CoalsVsTcorr_30",n);
    data_60 = readResults("CoalsVsTcorr_60",n);
    data_120 = readResults("CoalsVsTcorr_120",n);
    data_240 = readResults("CoalsVsTcorr_240",n);
    data_INF = readResults("CoalsVsTcorr_INF",n);
    data_UNR = readResults("CoalsVsTcorr_UNR",n);
    
    data_all{1} = data_0;
    data_all{2} = data_30;
    data_all{3} = data_60;
    data_all{4} = data_120;
    data_all{5} = data_240;
    data_all{6} = data_INF;
    data_all{7} = data_UNR;
    
    coals = zeros(n,7);
    scores = zeros(n,7);
    costs = zeros(n,7);
    for i = 1:7
        if i == 7
            coals(:,i) = zeros(n,1);
        else
            coals(:,i) = data_all{i}(:,2)./data_all{i}(:,3)*100;
        end
        
        scores(:,i) = data_all{i}(:,4)./data_all{i}(:,5)*100;
        costs(:,i) = data_all{i}(:,10)*3600/1000;
    end
    
%     t0 = repmat({'0'},n,1);
%     t1 = repmat({'30'},n,1);
%     t2 = repmat({'60'},n,1);
%     t3 = repmat({'120'},n,1);
%     t4 = repmat({'240'},n,1);
%     t5 = repmat({'\infty'},n,1);
%     t6 = repmat({'Not Constrained'},n,1);
%     x = [t0; t1; t2; t3; t4; t5; t6];
    x = {'0', '30', '60', '120', '240', 'Inf', 'Uncostrained'};
    
    figure 
    subplot(3,1,1);
    boxplot(coals);
    title("Constrained/Modular vs Integral")
    grid on;
    ylabel({'Coalitions formed/' ; 'Coalitions Available [%]'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,2);
    boxplot(scores);
    grid on;
    ylabel({'Score Achieved/' ; 'Score Available [%]'});
    set(gca,'xticklabel',{[]})
    subplot(3,1,3);
    boxplot(costs,x);
    grid on;
    ylabel({'Total Energy Cost';'Per Agent [kW-hr]'});
    xlabel('Allowed waiting time [min]')
end