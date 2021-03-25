/*

sprawl_SERVER_CTL.sc

Examples for connecting to the remote
server for monitoring.

Henrik von Coler
2021-01-18

*/

// connect to the sprawl server

(

o = ServerOptions.new;
o.maxLogins = 4;

o.numInputBusChannels  = 32;
o.numOutputBusChannels = 66;


t = Server.remote(\sprawl_remote, NetAddr("85.214.78.6", 57110), o,1);


)

// get information

t.makeWindow;

t.meter;

t.queryAllNodes();

