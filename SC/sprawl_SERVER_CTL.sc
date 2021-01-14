
// connect to the sprawl server

(

o = ServerOptions.new;
o.maxLogins = 4;

o.numInputBusChannels  = 32;
o.numOutputBusChannels = 32;


t = Server.remote(\sprawl_remote, NetAddr("85.214.78.6", 57110), o);


)

// get information

t.makeWindow;

t.meter;

t.queryAllNodes();

