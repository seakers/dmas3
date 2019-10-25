function [results] = readData(Mfolder)
    % Obtain results folders in results directory
    cd(Mfolder)
    currentFolder = dir;
    cases = [];
    
    for i = 1:length(currentFolder)
        name = currentFolder(i).name;
        if(currentFolder(i).isdir && (name ~= ".") && (name ~= "..") )
            cases = [cases, currentFolder(i)];
        end
    end
    
    % Obtain values from each case
    %- prepare output vectors
    results.name = {};
    results.values = {};
    
    %- check each case folder for values
    for i = 1:length(cases)
        folder = cases(i).folder;
        name = cases(i).name;
        newDir = sprintf("%s%s%s", folder, "\", name);
        cd(newDir);
        currentFolder = dir(newDir);
        
        results.name{i} = cases(i).name;
        tempData = [];
        
        %- read each result from each case
        for j = 1:length(currentFolder)
            folder = currentFolder(j).folder;
            name = currentFolder(j).name;
                       
            if(currentFolder(j).isdir && (name ~= ".") && (name ~= "..") )
                newDir = dir(sprintf("%s%s%s", folder, "\", name));
                if(length(newDir) > 2)
                    % if folder is NOT empty, process data
                    dataFile = sprintf("%s%s%s%s", folder, "\", name, "\performance_metrics.out");
                    fileID = fopen(dataFile,'r');
                    data =  fscanf(fileID, '%f');
                    fclose(fileID);
                    
                    tempData = [tempData; data'];
                end
            end
            
            if(length(tempData) >= 55)
                break;
            end
        end
        results.values{i} = tempData;
        
        %- return to main results folder
        cd ..;
    end
    
    cd ..;
    %- "results" struct now contains data from all validation experiments
end

