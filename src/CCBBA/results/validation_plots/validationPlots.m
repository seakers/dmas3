function [] = validationPlots()
    clc; clear all; close all;               
    
    % Figure 3
    results3 = readData("figure_3/M5"); 
    figureThree(results3);
    
%     % Figure 5
%     results5 = readData("figure_3/M5"); 
%     figureFive(results5);
    
end

