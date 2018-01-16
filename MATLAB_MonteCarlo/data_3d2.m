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

powersReq=2:1:9;
N=(10*ones(1,length(powersReq))).^powersReq;

lambdaD = 1.0;

%lambdaSLambdaD=10e-6:10e-5:1;
lambdaSLambdaD=[0.01:0.001:0.1 0.15:0.05:1];
%lambdaSLambdaD=0.035:0.001:0.036;

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
numLambdaSLambdaD_jumps=1;

coEfficient=zeros(1,length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)));
slope=zeros(1,length(1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)));

for j=1:numLambdaSLambdaD_jumps:length(lambdaSLambdaD)
    NversusCopy=zeros(1,length(N));
    for i=1:length(N)
        NversusCopy(i)=copySelected(i,j);
    end
    p = polyfit(log10(N),log10(NversusCopy),1);
    display(strcat('C=',num2str(10^p(2)),'N^{',num2str(p(1)),'} for \frac{\lambda_s}{\lambda_d}=',num2str(lambdaSLambdaD(j))))
    coEfficient(j)=10^p(2);
    slope(j)=p(1);
end


[XN,YL] = meshgrid(N,lambdaSLambdaD);
C=zeros(size(XN));

for i=1:size(XN,2)
    for j=1:size(XN,1)
        C(j,i)=coEfficient(j)* ( (XN(j,i)) ^ (slope(j)) );
    end
end

surf(XN,YL,C)
xlabel('N')
ylabel('$\lambda_s / \lambda_d$','Interpreter','latex')
zlabel('C')
set(gca,'XScale','log')
set(gca,'YScale','log')
rotate3d on


logXN=log10(XN);
logYL=log10(YL);
logC=log10(C);

[fitresult, gof]=createFit(logXN, logYL, logC);
rotate3d on


