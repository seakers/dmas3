clc; close all;

% Instrument Info
pulse_w = 0.000015;
D = 6;
h = 685 * 1e3;
Pt = 500;
n_eff = 0.6;
W = D;
L = D;
freq = 1.22E09;
c = 3e8;
lambda = c/freq;
B = 1.00E06;
k = 1.380649e-23;
T = 290;

% Requirements
Smax = 1;
C = 0;
res_req = [1e3 10e3];
sigma_req = [-30 -20];

% Measurement Conditions
Re = 6.3781e6;
th_max = pi/2 - acos(Re/(Re+h));
th_look = [15 35 50];
n_tasks = [3 1 1];
th_look_rad = th_look * pi / 180;

% Calc performance
[n,m] = size(th_look);
spat_res = ones(n,m) * -1;
sigma_eq = ones(n,m) * -1;
utl = zeros(n,m);

for i = 1:length(th_look)
%     spat_res(i) = c * pulse_w / (2 * sin(th_look_rad(i))) / 1e3;
    spat_res(i) = c / (2 * B * sin(th_look_rad(i)));
    
    num = Pt * n_eff^2 * W^2 * L * c * cos(th_look_rad(i))^4;
    den = 8 * pi * lambda * h^3 * B * sin(th_look_rad(i)) * k * T * B;
    sigma_eq(i) = 10*log10(den/num);
    
    utl(i) = n_tasks(i) * utility(Smax, spat_res(i)*1e3, res_req, sigma_eq(i), sigma_req);
end

% Output results
fprintf("Look angles: \t\t%.1f\t%.1f\t%.1f\t[deg]\n", th_look(1),th_look(2),th_look(3));
fprintf("Spatial resolution:\t%.2f\t%.2f\t%.2f\t[m]\n",spat_res(1), spat_res(2), spat_res(3));
fprintf("Sigma Eq:\t\t%.3f\t%.3f\t%.3f\t[dB]\n",sigma_eq(1), sigma_eq(2), sigma_eq(3));
fprintf("Utility:\t\t%.3f\t%.3f\t%.3f\t[-]\n",utl(1), utl(2), utl(3));

% sigmoid plots
A_max = [log(1/0.05 - 1) -1; 
         log(1/0.95 - 1) -1];
         
A_min = [log(1/0.95 - 1) -1; 
         log(1/0.05 - 1) -1];
     
res_req = [1e3 10e3]';
sigma_req = [-30 -20]';

acc_req = [.7 .9]';
res = linspace(0, 20e3);
acc = linspace(0, 1);

[gamma_res, res_ref] = coefs(res_req,true);
[gamma_sigma, sigma_ref] = coefs(sigma_req,true);
[gamma_acc, acc_ref] = coefs(acc_req,false);

sig_res = zeros(1,length(res));
sig_acc = zeros(1,length(acc));
for i = 1:length(res)
    sig_res(i) = sigmoid(res(i),res_ref,gamma_res);
    sig_acc(i) = sigmoid(acc(i),acc_ref,gamma_acc);
end

% figure
% plot(res,sig_res)
% grid on
% xlabel("")


function [gamma, ref] = coefs(req, min)
    A_max = [log(1/0.05 - 1) -1; 
            log(1/0.95 - 1) -1];
         
    A_min = [log(1/0.95 - 1) -1; 
             log(1/0.05 - 1) -1];
    if min
        c = A_min\-req;
    else 
        c = A_max\-req;
    end
    gamma = 1/c(1);
    ref = c(2);
end

function u = utility(Smax, res, res_req, sigma_eq, sigma_req) 
    [gamma1, x_ref_1] = coefs(res_req', true);
    [gamma2, x_ref_2] = coefs(sigma_req', true);

    sigm1 = sigmoid(res,x_ref_1,gamma1);
    sigm2 = sigmoid(sigma_eq,x_ref_2,gamma2);
    
    u = Smax * (0.5*sigm1 + 0.5*sigm2);
end

function s = sigmoid(x,x_ref,gamma)
    s = 1/(1 + exp(gamma * (x_ref - x)));
end
