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
            obj.ReqData = readmatrix(obj.ReqDir);
            obj.Lat = obj.ReqData(:,3);
            obj.Lon = obj.ReqData(:,4);            
        end
    end
end

