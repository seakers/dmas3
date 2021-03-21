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
        
        function [lat, lon] = findPoint(covDef, pnt)
            [n,~] = size(obj.DefData);
            lat = NaN;
            lon = MaN;
            
            for i = 1:n
                if obj.DefData{i,1} == covDef && obj.DefData{i,2} == pnt
                   lat = obj.DefData{i,3};
                   lon = obj.DefData{i,4};
                   break;
                end
            end
        end
    end
end

