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
            obj.DirName = string(jsonData.simulation.constellation) + "_"...
                        + string(jsonData.simulation.groundStationNetwork) + "_"...
                        + string(jsonData.simulation.scenario) + "_"...
                        + string(jsonData.simulation.startDate) + "_"...
                        + string(jsonData.simulation.endDate) + "/";
            obj.DataPath = "../data/coverage/" + obj.DirName + "CovDefs.csv";
            obj.DefData = readcell(obj.DataPath);
        end
        
    end
end

