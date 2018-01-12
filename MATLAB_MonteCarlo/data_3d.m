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

lambdaD = 1;
lambdaSLambdaD=10.^(-6:1);
lambdaS = lambdaSLambdaD./lambdaD;


powersReq=2:1:7;
N=(10*ones(1,length(powersReq))).^powersReq;

lambdaForCurveFittingStart=[1 1 1 1 2 3];
lambdaForCurveFittingEnd=[length(lambdaSLambdaD)-1 length(lambdaSLambdaD)*ones(1,5)];

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

for i=1:length(lambdaForCurveFittingStart)
    xAxis{i}=lambdaSLambdaD(lambdaForCurveFittingStart(i):lambdaForCurveFittingEnd(i));
    yAxis{i}=copySelected(i,lambdaForCurveFittingStart(i):lambdaForCurveFittingEnd(i));
end

k=[];
m=[];
for i=1:length(N)
    p = round(polyfit(log10(xAxis{i}),log10(yAxis{i}),1),2);
    k=[k (round(10^p(2),2))];
    m=[m p(1)];
    display(strcat('C=',num2str(round(10^p(2),2)),' \left( \lambda_s/\lambda_d \right)  ^{',num2str(p(1)),'} for N=10^{',num2str(log10(N(i))),'}'))
end

x=10e-6:10e-3:1;
newlambdaSLambdaD=sort(unique([lambdaSLambdaD(1:end-1) x]));
[XN,YL] = meshgrid(N,newlambdaSLambdaD);
C=zeros(size(XN));

for i=1:size(XN,2)
    for j=1:size(XN,1)
        C(j,i)=k(i)* ( (YL(j,i)) ^ (m(i)) );
    end
end

surf(XN,YL,C)
xlabel('N')
ylabel('$\lambda_s / \lambda_d$','Interpreter','latex')
zlabel('C')
set(gca,'XScale','log')
set(gca,'YScale','log')
rotate3d on
