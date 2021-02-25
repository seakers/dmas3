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
            obj.DirName = string(jsonData.constellation) + "_"...
                        + string(jsonData.groundStationNetwork) + "_"...
                        + string(jsonData.scenario) + "_"...
                        + string(jsonData.startDate) + "_"...
                        + string(jsonData.endDate) + "/";
            obj.DataPath = "../data/databases/GroundStationDatabase.xls";
            obj.DefData = readcell(obj.DataPath, 'Sheet', jsonData.groundStationNetwork);
            obj.DefData(1,:) = [];
        end
        
    end
end

