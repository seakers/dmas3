function [] = drawCoalsVsTcorr()
    data_all = {};
    
    data_0 = readResults("CoalsVsTcorr_0");
    data_10 = readResults("CoalsVsTcorr_10");
    data_30 = readResults("CoalsVsTcorr_30");
    data_90 = readResults("CoalsVsTcorr_90");
    data_INF = readResults("CoalsVsTcorr_INF");
    data_UNR = readResults("CoalsVsTcorr_UNR");
    
    data_all{1} = data_0;
    data_all{2} = data_10;
    data_all{3} = data_30;
    data_all{4} = data_90;
    data_all{5} = data_INF;
    data_all{6} = data_UNR;
    
    x = length(data_all);
end