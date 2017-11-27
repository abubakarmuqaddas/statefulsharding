clearvars
close all
clc

load distances

Copies=100;

c=c(1:Copies);
syncDist=syncDist(1:Copies);
mDist=mDist(1:Copies);

%powersReq=10:13;
%N=(10*ones(1,length(powersReq))).^powersReq;
N=10^10;

lambdaD = 1;

lambdaSLambdaD=0.5:0.01:1;

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

colorspec = {[0.9 0.9 0.9]; [0.8 0.8 0.8]; [0.6 0.6 0.6]; ...
  [0.4 0.4 0.4]; [0.2 0.2 0.2] ; [0.3 0.3 0.3] ; [0.5 0.5 0.5];...
  [0.7 0.7 0.7];[1 0 0];[0 1 0];[0 1 0];[1 1 0]};

pointTypes = ['+','o','*','s','d','x','>','<','p','h'];
colorTypes = ['r','b','k','m','c'];

figure
hold on
xlabel('$\lambda_s / \lambda_d$','Interpreter','latex')
ylabel('Number of copies')
h=get(gca,'xlabel');
set(h, 'FontSize', 15) 

for i=1:length(N)
    plot(lambdaSLambdaD,...
        copySelected(i,:),strcat('-',pointTypes(rem(i,length(pointTypes))),colorspec{i}));
end

Legend=cell(length(N),1);
for iter=1:length(N)
    Legend{iter}=strcat('N = 10^{',num2str(log10(N(iter))),'}');
end
legend(Legend)

%%
figure
hold on
for i=1:length(lambdaSLambdaD)
   temp1=totTfc(1,i,:);
   temp2=syncTfc(1,i,:);
   temp3=dataTfc(1,i,:);
   %plot(c,temp1(:)')
   %plot(c,temp2(:)')
   plot(c,temp3(:)')
   %
end
 
 
 
 