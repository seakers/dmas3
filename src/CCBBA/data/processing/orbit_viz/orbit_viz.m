function [] = orbit_viz()
%   Visualizes agent orbits and measurement-taking 
    clc; close all; clear all;
    Re = (6.371e6);
    pointsPerLine = 70;
        
    currentFolder = dir;
    cases = [];
    
    for i = 1:length(currentFolder)
        name = currentFolder(i).name;
        if(currentFolder(i).isdir && (name ~= ".") && (name ~= "..") )
            cases = [cases, currentFolder(i)];
        end
    end
    
    if( size(cases) > 1 )
       e = MException(ME.identifier, 'MATLAB:FileIO:TooManyFilesInFolder');
       throw(e);
    end
    
    agentOrbitFiles = importdata( string(cases.folder) + '\' + string(cases.name) + '\agent_orbit_files.out' );
    taskOrbitData = importdata( string(cases.folder) + '\' + string(cases.name) + '\task_orbit_files.out' ).data;
    
    cd('..'); cd('..');
    cd('orbits\agents')
    agentOrbitData = cell(length(agentOrbitFiles),1);
    for i = 1:length(agentOrbitFiles)
       localOrbit = importdata( agentOrbitFiles{i} ).data;
       agentOrbitData{i} = localOrbit;
    end
    cd('..'); cd('..');
    cd('processing\orbit_viz');
    
    
    figure
    hold on
    [X, Y, Z] = sphere;
    earth = surf(X,Y,Z);
    set(earth,'FaceColor',[0 1 1],'FaceAlpha',0.5,'FaceLighting','gouraud','EdgeColor','none')
    curveSat = cell(length(agentOrbitData),1);
    for i = 1:length(agentOrbitData)
       curveSat{i} = animatedline('Color', 'b');
    end
    curveTask = animatedline('Color', 'r');
    set(gca, 'XLim', [-2 2], 'YLim', [-2 2], 'ZLim', [-2 2]);
    grid on
    daspect([1 1 1]);
    view(3)
    
    
    
    plotTaskLocation(taskOrbitData, Re);
    [n m] = size(taskOrbitData);
    
    for i = 1:length(agentOrbitData{1})
        t = agentOrbitData{1}(i,10);
        addpoints(curveTask, taskOrbitData(1,1)/Re, taskOrbitData(1,2)/Re, taskOrbitData(1,3)/Re);
        
        for j = 1:length(agentOrbitData)
            if i < pointsPerLine + 1
                clearpoints(curveSat{j})
                addpoints(curveSat{j}, agentOrbitData{j}(1:i,1)/Re, agentOrbitData{j}(1:i,2)/Re,  agentOrbitData{j}(1:i,3)/Re);
            else
                clearpoints(curveSat{j})
                addpoints(curveSat{j}, agentOrbitData{j}(i-pointsPerLine:i,1)/Re, ...
                    agentOrbitData{j}(i-pointsPerLine:i,2)/Re, ...
                    agentOrbitData{j}(i-pointsPerLine:i,3)/Re);     
            end
        end
        
        for j = 1:n
           tz_j = taskOrbitData(n,7);
           if tz_j == t
                % draw measurement location 
                x_a = taskOrbitData(n,1)/Re;
                y_a = taskOrbitData(n,2)/Re;
                z_a = taskOrbitData(n,3)/Re;
                
                if x_a ~= 0.0 || y_a~=0.0 || z_a ~= 0.0
                    plot3(x_a, y_a, z_a,'.r','markersize',10) 
                end
           end
        end
        drawnow
    end
    
end

function [] = plotTaskLocation(taskOrbitData, Re)
    [n ~] = size(taskOrbitData);
    for i = 1:n
        x = taskOrbitData(i,1)/Re;
        y = taskOrbitData(i,2)/Re;
        z = taskOrbitData(i,3)/Re;
        plot3(x, y, z,'.k','markersize',10) 
        drawnow
        hold on
    end
end

