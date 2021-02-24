classdef CovDef
    %COVDEF Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
        DirName;
        DataPath;
        DefData;
    end
    
    methods
        function obj = CovDef(jsonData)
            %COVDEF Construct an instance of this class
            obj.DirName = string(jsonData.constellation) + "_"...
                        + string(jsonData.groundStationNetwork) + "_"...
                        + string(jsonData.scenario) + "_"...
                        + string(jsonData.startDate) + "_"...
                        + string(jsonData.endDate) + "/";
            obj.DataPath = "../data/coverage/" + obj.DirName + "CovDefs.csv";
            obj.DefData = readcell(obj.DataPath);
            
        end
        
    end
end

