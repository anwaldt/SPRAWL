/*

sprawl_SERVER_CTL.sc

Examples for connecting to the remote
server for monitoring.

Henrik von Coler
2021-01-18

*/

// connect to the sprawl server

(

o                      = ServerOptions.new;
o.maxLogins            = 4;
o.numInputBusChannels  = 32;
o.numOutputBusChannels = 2;

t = Server.remote(\sprawl_remote, NetAddr("bol", 57110),o);

)

// get information

t.clientID;

t.makeWindow;

t.meter;

t.queryAllNodes();


