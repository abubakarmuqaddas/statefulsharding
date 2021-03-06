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
%l=length(N);
%for i=1:l-1
%N=[N (N(i)+N(i+1))/2];
%end
%N=sort(N);

lambdaD = 1.0;

%lambdaSLambdaD=10e-6:10e-5:1;
%lambdaSLambdaD=[0.01:0.001:0.1 0.15:0.05:1];
%lambdaSLambdaD=0.035:0.001:0.036;
lambdaSLambdaD=0.01:0.001:1;

lambdaS = lambdaSLambdaD./lambdaD;
copySelected = zeros(length(N),length(lambdaSLambdaD));
totTfc=zeros(length(N),length(lambdaSLambdaD),length(c));
syncTfc=zeros(length(N),length(lambdaSLambdaD),length(c));
dataTfc=zeros(length(N),length(lambdaSLambdaD),length(c));

for j=1:length(N)
    for i=1:length(lambdaSLambdaD)
        syncTfc(j,i,:)=lambdaS(i)*syncDist.*c.*(c-1);
        dataTfc(j,i,:)=lambdaD*N(j)*mDist;
        totTfc(j,i,:)=sqrt(N(j)).*(syncTfc(j,i,:) + dataTfc(j,i,:));
        [minTotTfc,minCopy]=min(totTfc(j,i,:));
        copySelected(j,i)=c(minCopy);   
    end
end

%figure(10)
%hold on
k=1;
numLambdaSLambdaD_jumps=1;

dataToWrite = N';

colors = distinguishable_colors(length(lambdaSLambdaD),'w');

coEfficient=zeros(1,length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)));
power=zeros(1,length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)));

for j=1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)
    NversusCopy=zeros(1,length(N));
    for i=1:length(N)
        NversusCopy(i)=copySelected(i,j);
    end
%     figure(10)
%     plot(N,NversusCopy,strcat('-',pointTypes(rem(k,length(pointTypes))+1)),'color',colors(k,:))
%     if j==1
%         figure(10)
%         hold on
%         xlabel('N')
%         ylabel('Minimum number of copies')
%         set(gca, 'FontSize', 15) 
%     end
%     k=k+1;
    p = polyfit(log10(N),log10(NversusCopy),1);
%    display(strcat('C=',num2str(10^p(2)),'N^{',num2str(p(1)),'} for \frac{\lambda_s}{\lambda_d}=',num2str(lambdaSLambdaD(j))))
    coEfficient(j)=10^p(2);
    power(j)=p(1);
end

% numLegendEntry=length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD));
% figure(10)
% set(gca,'XScale','log')
% set(gca,'YScale','log')
% Legend=cell(numLegendEntry,1);
% k=1;
% for iter=1:numLegendEntry
%     Legend{iter}=strcat('\lambda_s / \lambda_d=',num2str(lambdaSLambdaD(k)));
%     k=k+numLambdaSLambdaD_jumps;
% end
% legend(Legend)

figure
plot(lambdaSLambdaD,coEfficient,'bd')
hold on
plot(lambdaSLambdaD,power,'ko')
xlabel('$\lambda_s / \lambda_d$','Interpreter','latex')
legend('10^k','m')
set(gca, 'FontSize', 15) 

figure
plot(lambdaSLambdaD,coEfficient,'bd')
hold on
plot(lambdaSLambdaD,0.47.*(lambdaSLambdaD.^(-0.40)),'-k')


xlabel('$\lambda_s / \lambda_d$','Interpreter','latex')
ylabel('Coefficient')
ylim([0 3])
set(gca, 'FontSize', 15) 
