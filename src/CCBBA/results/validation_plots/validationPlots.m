function [] = validationPlots()
    clc; clear all; close all;
    addpath("./figure_3");
    
    % Figure 3
    results3 = readData("figure_3"); 
    figureThree(results3);
    
%     % Figure 5
%     results5 = readData("figure_3/M5"); 
%     figureFive(results5);
    
end

