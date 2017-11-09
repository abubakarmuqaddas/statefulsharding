clearvars
close all
clc


% Number of copies
c=(2:10).^2;

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
line([0,100],[0.5412,0.5412])
ylim([0 1])