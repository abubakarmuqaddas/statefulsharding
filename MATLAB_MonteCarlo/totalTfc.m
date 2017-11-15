clearvars
close all
clc

load distances

sizeOfCopies=7;
lambdaD = 4;
N = 100; %nodes
lambdaS=0.05;

c=c(1:sizeOfCopies);
mDist=mDist(1:sizeOfCopies);
syncDist=syncDist(1:sizeOfCopies);

syncTfc=lambdaS*syncDist.*c.*(c-1);
dataTfc=lambdaD*N*mDist;

totTfc=syncTfc + dataTfc;

plot(c,syncTfc,'-ok')
hold on
plot(c,dataTfc,'-*r')
plot(c,totTfc,'-xb')
xlabel('Number of copies')
ylabel('Traffic')

legend('Synchronization traffic','Data traffic','Total traffic')

% lambdaD2 = 4.5;
% lambdaS2 = 0.001:0.005:0.05;
% 
% figure
% hold on
% for i=1:length(lambdaS2)
%     plot(c,lambdaS2(i)*syncDist.*c.*(c-1) + lambdaD2*N*mDist)
%     hold on
% end


