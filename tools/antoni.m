clc; close all;

path = "../results/antoni_";
fileNames = ["1_1", "1_2", "1_3", "1_4",...
             "2_2", "2_4", "2_6", "2_8",...
             "3_3", "3_6", "3_9", "3_12",...
             "4_4", "4_8", "4_12", "4_16",...
             "5_5", "5_10", "5_15", "5_20"];

data = zeros(4*5,2);
data(1,2) = 1;
data(2,2) = 2;
data(3,2) = 3;
data(4,2) = 4;
data(5,2) = 2;
data(6,2) = 4;
data(7,2) = 6;
data(8,2) = 8;
data(9,2) = 3;
data(10,2) = 6;
data(11,2) = 9;
data(12,2) = 12;
data(13,2) = 4;
data(14,2) = 8;
data(15,2) = 12;
data(16,2) = 16;
data(17,2) = 5;
data(18,2) = 10;
data(19,2) = 15;
data(20,2) = 20;


[n,m] = size(fileNames);
for i = 1:m
   jsonData = jsondecode( fileread(path + fileNames(i) + "/run_0/results.json") );
   data(i,1) = jsonData.results.utility.utilityPtg;
end

figure 
scatter(data(1:4,2),data(1:4,1),'filled')
hold on
grid on
scatter(data(5:8,2),data(5:8,1),'filled')
scatter(data(9:12,2),data(9:12,1),'filled')
scatter(data(13:16,2),data(13:16,1),'filled')
scatter(data(17:20,2),data(17:20,1),'filled')

legend("1 plane", "2 planes", "3 planes", "4 planes", "5 planes","Location","Best")
xlabel("Number of Satellites")
ylabel("Utility Achieved/Utility Available")
title("Utility Percentage vs Number of Satellites")
xticks(1:20)