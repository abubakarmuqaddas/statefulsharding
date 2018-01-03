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

powersReq=2:1:14;
N=(10*ones(1,length(powersReq))).^powersReq;

lambdaD = 1;

%lambdaSLambdaD=0.01:0.01:1;
lambdaSLambdaD=10.^(-6:1);

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

colorspec = {[0.1 0.1 0.1];[0.9 0.9 0.9]; [0.8 0.8 0.8]; [0.6 0.6 0.6]; ...
  [0.4 0.4 0.4]; [0.2 0.2 0.2] ; [0.3 0.3 0.3] ; [0.9 0.5 0.5];...
  [0.7 0.7 0.7];[0 1.0 0];[0 1.0 0];[1.0 0.5 0]};

colors = distinguishable_colors(length(N),'w');

pointTypes = ['+','o','*','s','d','x','>','h','<','p'];
colorTypes = ['r','b','k','m','c'];

for i=1:length(N)
    loglog(lambdaSLambdaD,...
        copySelected(i,:),strcat('-',pointTypes(rem(i,length(pointTypes))+1)),'color',colors(i,:));
        %copySelected(i,:),strcat('-',pointTypes(rem(i,length(pointTypes))+1),colorspec{rem(i,length(colorspec))+1}));
    if i==1
        hold on
        xlabel('$\lambda_s / \lambda_d$','Interpreter','latex')
        ylabel('Number of copies')
        set(gca, 'FontSize', 15) 
    end
end


Legend=cell(length(N),1);
for iter=1:length(N)
    Legend{iter}=strcat('N = 10^{',num2str(log10(N(iter))),'}');
end
legend(Legend)


% cutOffLsLd=floor(length(lambdaSLambdaD)/2);
% NversusCopy=zeros(1,length(N));
% for i=1:length(N)
%     NversusCopy(i)=copySelected(i,cutOffLsLd);
% end
% 
% figure
% semilogx(N,NversusCopy,'-ks')
% xlabel('N')
% ylabel('Minimum number of copies')
% set(gca, 'FontSize', 15) 

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
    dataToWrite = [dataToWrite NversusCopy'];
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


%ylim([0 2000])



%%
% currentN = 2;
% figure
% hold on
% xlabel('Copies'); ylabel('Traffic');
% for i=1:1
%    temp1=totTfc(currentN,i,:);
%    temp2=syncTfc(currentN,i,:);
%    temp3=dataTfc(currentN,i,:);
%    %plot(c,temp1(:)')%, ylim([0 max(temp1(:))])
%    %plot(c,temp2(:)')%, ylim([0 max(temp2(:))])
%    plot(c,temp3(:)')%, ylim([0 max(temp3(:))])
%    set(gca, 'FontSize', 15) 
%    %
% end
%title('N=10e10')
%text(5000,5e9,'Total Traffic','FontSize', 15)
%text(5000,0.30e9,'Sync Traffic','FontSize', 15) 

figure
plot(c,mDist,'LineWidth',1.5); xlabel('Number of Copies'); ylabel('$\hat{d}_{data}$','Interpreter','latex'); set(gca, 'FontSize', 15)
 
 
 