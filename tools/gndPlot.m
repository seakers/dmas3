
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

    % plotm(lat, lon, 'blue')
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
    
%     pos = [];
% 
%     n_points = 100;
%     t = linspace(0,1000,n_points);
%     lon1 = linspace(-180, 180, n_points);
%     lat1 = 45 * sin(t*pi()/180);
% 
%     lon2 = linspace(-180, 180, n_points);
%     lat2 = 45 * sin(t*pi()/180 - pi()/2);
% 
%     n_draw = 10;
% 
%     n_contacts = 10;
%     cntc = [0 100;
%             200 300;
%             400 500;
%             600 700;
%             800 900];
% 
%     [latcells, loncells] = polysplit(coastlat, coastlon);
%     plotm(coastlat, coastlon, 'black')
%     hold on
%     x0=200;
%     y0=200;
%     width=1000;
%     height=1000;
%     set(gcf,'position',[x0,y0,width,height])
% 
%     % plotm(lat, lon, 'blue')
%     t0 = tic;
%     j = 1;
%     for i = 1:n_points    
%         if i <= n_draw
%            lin1 = plotm(lat1(1:i),lon1(1:i),'blue');
%            lin2 = plotm(lat2(1:i),lon2(1:i),'red');
%         else
%            lin1 = plotm(lat1(i-n_draw:i),lon1(i-n_draw:i),'blue');
%            lin2 = plotm(lat2(i-n_draw:i),lon2(i-n_draw:i),'red');
%         end
%         pnt1 = plotm(lat1(i),lon1(i),'bs','MarkerSize', 10);
%         pnt2 = plotm(lat2(i),lon2(i),'rs','MarkerSize', 10);
%         crcl1 = circlem(lat1(i),lon1(i),1000,'EdgeColor','b');
%         crcl2 = circlem(lat2(i),lon2(i),1000,'EdgeColor','r');
% 
%         if j <= length(cntc)
%             c = cntc(j,:);
%             if t(i) >= c(1) && t(i) < c(2)
%                 cln = linem( [lat1(i);lat2(i)], [lon1(i);lon2(i)]);
%             elseif t(i) >= c(2)
%                 j = j+1;
%             end
%         end
% 
%         drawnow;
%         while toc(t0) < 1/30
%             %wait     
%         end
%         delete(lin1);
%         delete(lin2);
%         delete(pnt1);
%         delete(pnt2);
%         delete(crcl1);
%         delete(crcl2);
%         delete(cln);
% 
%         t0 = tic;
%     end
%     plotm(lat1, lon1, 'blue')
%     plotm(lat2, lon2, 'red')
%     circlem(lat1,lon1,1000,'EdgeColor','b');
%     circlem(lat2,lon2,1000,'EdgeColor','r');
%     drawnow;
% 
%     disp('DONE')

end