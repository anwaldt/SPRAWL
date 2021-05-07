

There were some issues with the basic Euclidean distance formula.
Additional abs is needed:

    dist = sqrt( (pow(abs(xS-xR),2) + pow(abs(yS-yR),2) + pow(abs(zS-zR),2)) );
