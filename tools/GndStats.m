classdef GndStats
%   Detailed explanation goes here
    
    properties
        DirName;
        DataPath;
        DefData;
    end
    
    methods
        function obj = GndStats(jsonData)
            %COVDEF Construct an instance of this class
            obj.DirName = string(jsonData.simulation.constellation) + "_"...
                        + string(jsonData.simulation.groundStationNetwork) + "_"...
                        + string(jsonData.simulation.scenario) + "_"...
                        + string(jsonData.simulation.startDate) + "_"...
                        + string(jsonData.simulation.endDate) + "/";
            obj.DataPath = "../data/databases/GroundStationDatabase.xls";
            obj.DefData = readcell(obj.DataPath, 'Sheet', jsonData.simulation.groundStationNetwork);
            obj.DefData(1,:) = [];
        end
        
    end
end

