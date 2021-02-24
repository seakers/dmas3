
function [] = gndPlot(varargin)
    clc; close all;
       
    % Read JSON input file
    [fileName,fileDir,~] = uigetfile('../inputs/*.json*','Select Input File');
    filePath = [fileDir fileName];
    
    jsonData = jsondecode( fileread(filePath) );
    
    % Load Ortbit Data
    sensingSatsData = readcell( '../data/constellations/' + string(jsonData.constellation) + '.xls', 'Sheet', 'Remote Sensing');
    [n_sense ~] = size(sensingSatsData);
    commsSatsData = readcell( '../data/constellations/' + string(jsonData.constellation) + '.xls', 'Sheet', 'Communications');
    [n_comms ~] = size(commsSatsData);
    
    satNames = [];
    for i = 2:n_sense
       satNames = [satNames; sensingSatsData{i,2}];
    end
    for i = 2:n_comms
       satNames = [satNames; commsSatsData{i,2}]; 
    end
    
    sats = cell( length(satNames),1 );
    j = 1;
    for i = 2:n_sense
        name = sensingSatsData{i,2};
        alt = sensingSatsData{i,3};
        fov = 50;
        sats{j} = Sat(name, alt, fov, jsonData);
        j = j + 1;
    end
    
    for i = 2:n_comms
        name = commsSatsData{i,2};
        alt = commsSatsData{i,3};
        fov = commsSatsData{i,8};
        sats{j} = Sat(name, alt, fov, jsonData);
        j = j + 1;
    end
    
    % Load Ground Points
    gndPts = CovDef(jsonData);
    
    % Generate Plot
    
    worldmap world
    load coastlines
    
    [latcells, loncells] = polysplit(coastlat, coastlon);
    plotm(coastlat, coastlon, 'black')
    hold on
    x0=200;
    y0=200;
    width=1000;
    height=1000;
    set(gcf,'position',[x0,y0,width,height])

    t0 = tic;
    t = sats{1}.T;
    n_draw = 10;
    
    lines = cell(size(sats));
    points = cell(size(sats));
    circles = cell(size(sats));
    for j = 1:length(sats)
        lines{j} = plotm(0,0);
        points{j} = plotm(0,0);
        circles{j} = circlem(0,0,0);
    end
    
    plotm([gndPts.DefData{:,3}], [gndPts.DefData{:,4}], '.', 'Color', [1, 1, 1]*.75);
    x = 1;
  
%     for i = 1:length(t)    
%         if i <= n_draw
%            for j = 1:length(sats)
%                sat = sats{j};
%                lines{j} = plotm(sat.Lat(1:i),sat.Lon(1:i));
%            end
%         else
%            for j = 1:length(sats)
%                sat = sats{j};
%                lines{j} = plotm(sat.Lat(i-n_draw:i),sat.Lon(i-n_draw:i));
%            end
%         end
%         
%         for j = 1:length(sats)
%            sat = sats{j};
%            points{j} = plotm(sat.Lat(i),sat.Lon(i),'s', 'MarkerSize', 10);
%            circles{j} = circlem(sat.Lat(i),sat.Lon(i),...
%                sat.R_fov, 'units', validateLengthUnit('kilometer'),...
%                'facecolor', 'b',...
%                'FaceAlpha', 0.5,...
%                'EdgeAlpha', 0.5);
%         end
%         
%         drawnow;
%         
%         while toc(t0) < 1/30
%             %wait     
%         end
%         
%         for j = 1:length(sats)
%             delete(lines{j})
%             delete(points{j})
%             delete(circles{j})
%         end
% 
%         t0 = tic;
%         
%     end
    
    for j = 1:length(sats)
       sat = sats{j};
       lines{j} = plotm(sat.Lat(:),sat.Lon(:));
%        circles{j} = circlem(sat.Lat(:),sat.Lon(:),...
%                sat.R_fov, 'units', validateLengthUnit('kilometer'),...
%                'facecolor', 'b',...
%                'FaceAlpha', 0.5,...
%                'EdgeAlpha', 0.5);
    end

    disp('DONE')
end