function [] = validationPlots()
    clc; clear all;
    
    % Obtain results folders in results directory
    currentFolder = dir;
    cases = [];
    
    for i = 1:length(currentFolder)
        name = currentFolder(i).name;
        if(currentFolder(i).isdir && (name ~= ".") && (name ~= "..") )
            cases = [cases, currentFolder(i)];
        end
    end
    
    % Obtain values from each case
    for i = 1:length(cases)
        folder = cases(i).folder;
        name = cases(i).name;
        newDir = sprintf("%s%s%s", folder, "\", name);
        cd(newDir);
        currentFolder = dir(newDir);
        
        % read each result from each case
        for j = 1:length(currentFolder)
            folder = currentFolder(j).folder;
            name = currentFolder(j).name;
                       
            if(currentFolder(j).isdir && (name ~= ".") && (name ~= "..") )
                newDir = dir(sprintf("%s%s%s", folder, "\", name));
                if(~isempty(newDir))
                    % if folder is NOT empty, process data
                    dataFile = sprintf("%s%s%s%s", folder, "\", name, "\performance_metrics.out");
                    fileID = fopen(dataFile,'r');
                    data =  fscanf(fileID, ['d' '\t'])
                end
            end
        end
        
        % return to main results folder
        cd ..;
    end
end

