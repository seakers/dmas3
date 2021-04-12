classdef measReqs
    %MEASREQ Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
        ReqDir;
        ReqData;
        ReqDataSorted;
        Lat;
        Lon;
    end
    
    methods
        function obj = measReqs(resDir)
            %MEASREQS Construct an instance of this class
            obj.ReqDir = resDir + "/run_0/requests.csv";
            obj.ReqData = readcell(obj.ReqDir, 'Delimiter',',');
            
            obj.Lat = zeros(length(obj.ReqData(:,3)));
            obj.Lon = zeros(length(obj.ReqData(:,4)));
            for i = 1:length(obj.ReqData(:,3))
                obj.Lat(i,1) = obj.ReqData{i,3};
                obj.Lon(i,1) = obj.ReqData{i,4};
            end
        end
    end
end

