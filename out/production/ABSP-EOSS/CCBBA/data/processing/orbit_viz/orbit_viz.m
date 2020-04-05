function [] = orbit_viz()
%   Visualizes agent orbits and measurement-taking 
    clc; close all; clear all;
        
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
    taskOrbitFiles = importdata( string(cases.folder) + '\' + string(cases.name) + '\task_orbit_files.out' ).data;
    
    agentOrbitData = {};
    cd('..'); cd('..');
    cd('orbits\agents')
    for i = 1:length(agentOrbitFiles)
       localOrbit = importdata( agentOrbitFiles{i} ).data;
       agentOrbitData{i} = localOrbit;
    end
    cd('..'); cd('..');
    cd('processing\orbit_viz');
    
    x = 1;
end

