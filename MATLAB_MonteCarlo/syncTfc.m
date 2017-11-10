clearvars
close all
clc


% Number of copies
c=(2:10).^2;

% Initialize dist matrix
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