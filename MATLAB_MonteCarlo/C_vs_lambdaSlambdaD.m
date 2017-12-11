clearvars
close all
clc

set(0,'DefaultAxesFontName', 'Times New Roman')
load distances

Copies=119;

copies=c;
c=c(1:Copies);
syncDist=syncDist(1:Copies);
mDist=mDist(1:Copies);

powersReq=5:6;
N=(10*ones(1,length(powersReq))).^powersReq;
%N=10^10;

lambdaD = 1;

lambdaSLambdaD=0:0.05:1;

%lambdaSLambdaD=10.^(-6:0)

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

colorspec = {[0.1 0.1 0.1];[0.9 0.9 0.9]; [0.8 0.8 0.8]; [0.6 0.6 0.6]; ...
  [0.4 0.4 0.4]; [0.2 0.2 0.2] ; [0.3 0.3 0.3] ; [0.9 0.5 0.5];...
  [0.7 0.7 0.7];[0 1.0 0];[0 1.0 0];[1.0 0.5 0]};

pointTypes = ['+','o','*','s','d','x','>','h','<','p'];
colorTypes = ['r','b','k','m','c'];

figure
hold on
xlabel('$\lambda_s / \lambda_d$','Interpreter','latex')
ylabel('Number of copies')
set(gca, 'FontSize', 15) 
%set(h, 'FontSize', 15) 

for i=1:length(N)
    semilogy(lambdaSLambdaD,...
        copySelected(i,:),strcat('-',pointTypes(rem(i,length(pointTypes))),colorspec{i}));
end

%ylim([0 2000])


Legend=cell(length(N),1);
for iter=1:length(N)
    Legend{iter}=strcat('N = 10^{',num2str(log10(N(iter))),'}');
end
legend(Legend)

%%
currentN = 6;
figure
hold on
xlabel('Copies'); ylabel('Traffic');
for i=1:length(lambdaSLambdaD)
   temp1=totTfc(currentN,i,:);
   temp2=syncTfc(currentN,i,:);
   temp3=dataTfc(currentN,i,:);
   plot(c,temp1(:)')%, ylim([0 max(temp1(:))])
   plot(c,temp2(:)')%, ylim([0 max(temp2(:))])
   %plot(c,temp3(:)')%, ylim([0 max(temp3(:))])
   set(gca, 'FontSize', 15) 
   %
end
title('N=10e10')
text(5000,5e9,'Total Traffic','FontSize', 15)
text(5000,0.30e9,'Sync Traffic','FontSize', 15) 
 
 
 