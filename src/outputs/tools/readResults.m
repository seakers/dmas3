% input - Name of the scenario directory
% n     - number of max data points 
function [ data ] = readResults(input, n)
    directory = "../" + input;
    folder = dir(directory);
    data = zeros(n,18);
    i_curr = 1;
    for i = 3:length(folder)
        subfolder = strcat(folder(i).folder,strcat('\',folder(i).name));
        filename = strcat(subfolder,'\results.out');
        fileID = fopen(filename, 'r');
        if fileID >= 0 && n >= i_curr
            data_i = fscanf(fileID, '%f\t%d\t%d\t%f\t%f\t%d\t%d\t%d\t%d\t%f\t%f\t%f\t%f\t%d\t%f\t%f\t%f\t%f');
            data(i_curr,:) = data_i;
            i_curr = i_curr + 1;
        end
    end
    
    if(i_curr < n)
        warn = "Not enough data in " + input + " results folders";
        ME = MException("ERROR:INPUT",warn);
        throw(ME);
    end
end

% utility + "\t" + coalsFormed + "\t" + coalsAvailable + "\t" + scoreAchieved + "\t" + scoreAvailable
%+ "\t" + tasksDone + "\t" + tasksAvailable + "\t" + numAgents + "\t" + planningHorizon + "\t" + overallCostPerAgent
%+ "t" + resSat  + "\t" + snrSat + "\t" + revSat + "\t" + numMeasurements + "\t" + runTime