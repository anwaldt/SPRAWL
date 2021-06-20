# SuperCollider Implementation Details

There were some issues with the basic Euclidean distance formula in the nplace classes. Additional abs is needed, even when using pow(2):

    dist = sqrt( (pow(abs(xS-xR),2) + pow(abs(yS-yR),2) + pow(abs(zS-zR),2)) );
