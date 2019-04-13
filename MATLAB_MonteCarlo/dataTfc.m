clearvars
close all
clc


% Number of copies
c=(2:20).^2;

% Number of inner iterations
numIters = 1000000;

% Initialize dist matrix
dist = zeros(numIters, length(c));

for i=1:length(c)

    for j=1:numIters

        width=1/sqrt(c(i));

        % Generate uniformly src & dst
        xSrc = rand;
        ySrc = rand;
        xDst = rand;
        yDst = rand;

        % Find the corner of the square the src exists in
        xCorner = floor(xSrc/width)*width;
        yCorner = floor(ySrc/width)*width;

        % Find the center of the square
        xCenter = xCorner + width/2;
        yCenter = yCorner + width/2;

        % Find the distance from src to center & center to dst
        dist(j,i) = sqrt((xSrc - xCenter)^2 + (ySrc - yCenter)^2) + ...
            sqrt((xCenter - xDst)^2 + (yCenter - yDst)^2);
    end
end

plot(c,mean(dist),'-ok')
%line([0,max(c)],[0.5412,0.5412])
hold on
ylim([0 1])

pause 

syncDist = zeros(1, length(c));

for iter=1:length(c)

    width=1/sqrt(c(iter));

    xCoords = (0:width:0.99999999)+ width/2;
    yCoords = (0:width:0.99999999)+ width/2;
    [X,Y]=meshgrid(xCoords,yCoords);
    cP=[X(:) Y(:)];

    totalDist=0;

    for i=1:c(iter)
        for j=1:c(iter)

            if i==j
                continue;
            end

            totalDist = totalDist + ...
                sqrt((cP(i,1)-cP(j,1))^2+(cP(i,2)-cP(j,2))^2);
        end
    end

    syncDist(iter)=totalDist/(c(iter)*(c(iter)-1));
   
end

plot(c,syncDist,'-*r')

%plot(c,0.05*syncDist.*c.*(c-1),'-ok')
%hold on
%plot(c,0.05*100*mean(dist),'-*r')
%plot(c,0.5*100*mean(dist)+0.01*syncDist.*c.*(c-1),'-xb')
