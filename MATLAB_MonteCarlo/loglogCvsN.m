clearvars
close all
clc


colorspec = {[0.1 0.1 0.1];[0.9 0.9 0.9]; [0.8 0.8 0.8]; [0.6 0.6 0.6]; ...
  [0.4 0.4 0.4]; [0.2 0.2 0.2] ; [0.3 0.3 0.3] ; [0.9 0.5 0.5];...
  [0.7 0.7 0.7];[0 1.0 0];[0 1.0 0];[1.0 0.5 0]};

pointTypes = ['+','o','*','s','d','x','>','h','<','p'];
colorTypes = ['r','b','k','m','c'];

set(0,'DefaultAxesFontName', 'Times New Roman')
load distances

Copies=119;

copies=c;
c=c(1:Copies);
syncDist=syncDist(1:Copies);
mDist=mDist(1:Copies);

powersReq=2:1:9;
N=(10*ones(1,length(powersReq))).^powersReq;

lambdaD = 1;

lambdaSLambdaD=0.01:0.01:1;

lambdaS = lambdaSLambdaD./lambdaD;
copySelected = zeros(length(N),length(lambdaSLambdaD));
totTfc=zeros(length(N),length(lambdaSLambdaD),length(c));
syncTfc=zeros(length(N),length(lambdaSLambdaD),length(c));
dataTfc=zeros(length(N),length(lambdaSLambdaD),length(c));

for j=1:length(N)
    for i=1:length(lambdaSLambdaD)
        syncTfc(j,i,:)=lambdaS(i)*syncDist.*c.*(c-1);
        dataTfc(j,i,:)=lambdaD*N(j)*mDist;
        totTfc(j,i,:)=syncTfc(j,i,:) + dataTfc(j,i,:);
        [minTotTfc,minCopy]=min(totTfc(j,i,:));
        copySelected(j,i)=c(minCopy);   
    end
end

figure(10)
hold on
k=1;
numLambdaSLambdaD_jumps=10;

dataToWrite = N';

for j=1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)
    NversusCopy=zeros(1,length(N));
    for i=1:length(N)
        NversusCopy(i)=copySelected(i,j);
    end
    figure(10)
    plot(N,NversusCopy,strcat('-',pointTypes(rem(k,length(pointTypes))+1),colorspec{rem(k,length(colorspec))+1}))
    if j==1
        figure(10)
        hold on
        xlabel('N')
        ylabel('Minimum number of copies')
        set(gca, 'FontSize', 15) 
    end
    k=k+1;
    p = round(polyfit(log10(N),log10(NversusCopy),1),2);
    display(strcat('C=',num2str(p(2)),'N^{',num2str(p(1))...
         ,'} for \frac{\lambda_s}{\lambda_d}=',num2str(lambdaSLambdaD(j))))
end

numLegendEntry=length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD));
figure(10)
set(gca,'XScale','log')
set(gca,'YScale','log')
Legend=cell(numLegendEntry,1);
k=1;
for iter=1:numLegendEntry
    Legend{iter}=strcat('\lambda_s / \lambda_d=',num2str(lambdaSLambdaD(k)));
    k=k+numLambdaSLambdaD_jumps;
end
legend(Legend)


