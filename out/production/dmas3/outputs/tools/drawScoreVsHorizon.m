function [] = drawScoreVsHorizon(n)
    data_all = {};
    
    data_1 = readResults("ScoreVsAgents_1",n);
    data_2 = readResults("ScoreVsAgents_2",n);
    data_3 = readResults("ScoreVsAgents_3",n);
    data_4 = readResults("ScoreVsAgents_4",n);
    data_5 = readResults("ScoreVsAgents_5",n);
    data_6 = readResults("ScoreVsAgents_6",n);
    data_7 = readResults("ScoreVsAgents_7",n);
    data_8 = readResults("ScoreVsAgents_8",n);
    data_9 = readResults("ScoreVsAgents_9",n);
    data_10 = readResults("ScoreVsAgents_10",n);
    
    data_all{1} = data_1;
    data_all{2} = data_2;
    data_all{3} = data_3;
    data_all{4} = data_4;
    data_all{5} = data_5;
    data_all{6} = data_6;
    data_all{7} = data_7;
    data_all{8} = data_8;
    data_all{9} = data_9;
    data_all{10} = data_10;
    
    utility = zeros(n,10);
    for i = 1:10
       utility(:,i) = data_all{i}(:,1)./data_all{i}(:,5)*100;
    end
    
    t1 = repmat({'1'},n,1);
    t2 = repmat({'2'},n,1);
    t3 = repmat({'3'},n,1);
    t4 = repmat({'4'},n,1);
    t5 = repmat({'5'},n,1);
    t6 = repmat({'6'},n,1);
    t7 = repmat({'7'},n,1);
    t8 = repmat({'8'},n,1);
    t9 = repmat({'9'},n,1);
    t10 = repmat({'10'},n,1);
    
    x = [t1; t2; t3; t4; t5; t6; t7; t8; t9; t10];
    
    figure
    boxplot(scores,x);
    grid on;
    ylabel({'Total Utility/' ; 'Score Available [%]'});
    xlabel('Planning Horizon');
    title('Non-dimensional score achieved by all agents');
end