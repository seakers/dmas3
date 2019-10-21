function [] = validationPlots()
    clc; clear all; close all;
    
    % Obtain results folders in results directory
    results = readData("M5");               
    
    % Create plots for all figures
    figureThree(results);
    
end

