clearvars
close all
clc

load distances

sizeOfCopies=10;
lambdaD = 3;
N = 100; %nodes
lambdaS=0.01;

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
