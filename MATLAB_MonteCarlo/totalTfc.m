clearvars
close all
clc

load distances

sizeOfCopies=6;
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

legend('Synchronization traffic','Data traffic','Total traffic', ... 
'Location','best')

% lambdaD2 = 4;
% lambdaS2 = 0.000:0.025:0.1;
% tfc=[];
% 
% figure
% hold on
% for i=1:length(lambdaS2)
%     tfc = [ tfc ; lambdaS2(i)*syncDist.*c.*(c-1) + lambdaD2*N*mDist];
%     plot(c,lambdaS2(i)*syncDist.*c.*(c-1) + lambdaD2*N*mDist,'-o')
%     hold on
% end


