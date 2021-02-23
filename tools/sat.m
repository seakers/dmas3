classdef Sat
    % SAT - Satellite class
    %   Holds information about the satellite, its orbit, and its position
    
    properties
        Name;
        Fov;
        Alt;
        R_fov;
        OrbitData;
        Lat;
        Lon;
        T;
        DirName;
        DataPath;
    end
    
    methods
        function obj = Sat(name, alt, fov, jsonData)
            %Constructor
            obj.Name = name;
            obj.Alt = alt;
            obj.Fov = fov;
            obj.R_fov = alt * tand(fov/2);
            
%             if obj.R_fov > 1e4
%                 obj.R_fov = 1e4;
%             end
            
            obj.DirName = string(jsonData.constellation) + "_"...
                        + string(jsonData.groundStationNetwork) + "_"...
                        + string(jsonData.scenario) + "_"...
                        + string(jsonData.startDate) + "_"...
                        + string(jsonData.endDate) + "/";
            obj.DataPath = "../data/coverage/" + obj.DirName + name + "_pv.csv";
            obj.OrbitData = readmatrix(obj.DataPath);
            obj.T = obj.OrbitData(:,1);
            obj.Lat = obj.OrbitData(:,5);
            obj.Lon = obj.OrbitData(:,6);
        end
        
        function outputArg = method1(obj,inputArg)
            %METHOD1 Summary of this method goes here
            %   Detailed explanation goes here
            outputArg = obj.Property1 + inputArg;
        end
    end
end

