/////////////////////////////////////////////////////////////////
// source gain listeners:
/////////////////////////////////////////////////////////////////


OSCdef('/source/gain',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~source_gain_BUS.setAt(msg[1],val);
}, '/source/gain');


OSCdef('/source/reverb',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~source_reverb_BUS.setAt(msg[1],val);
}, '/source/reverb');


/////////////////////////////////////////////////////////////////
// source position listeners:
/////////////////////////////////////////////////////////////////


OSCdef('/source/pos/x',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~source_x_BUS.setAt(msg[1],val);
}, '/source/pos/x');

OSCdef('/source/pos/y',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~source_y_BUS.setAt(msg[1],val);
}, '/source/pos/y');

OSCdef('/source/pos/z',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~source_z_BUS.setAt(msg[1],val);
}, '/source/pos/z');



/////////////////////////////////////////////////////////////////
// receiver position listeners:
/////////////////////////////////////////////////////////////////

OSCdef('/receiver/pos/x',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_x_BUS.setAt(msg[1],val);
}, '/receiver/pos/x');

OSCdef('/receiver/pos/y',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_y_BUS.setAt(msg[1],val);
}, '/receiver/pos/y');

OSCdef('/receiver/pos/z',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_z_BUS.setAt(msg[1],val);
}, '/receiver/pos/z');


/////////////////////////////////////////////////////////////////
// receiver orientation listeners:
/////////////////////////////////////////////////////////////////

OSCdef('/receiver/ori/pitch',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_pitch_BUS.setAt(msg[1],val);
}, '/receiver/ori/pitch');

OSCdef('/receiver/ori/roll',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_roll_BUS.setAt(msg[1],val);
}, '/receiver/ori/roll');

OSCdef('/receiver/ori/yaw',
	{
		arg msg, time, addr, recvPort;
		var val = msg[2];
		~site_yaw_BUS.setAt(msg[1],val);
}, '/receiver/ori/yaw');


/////////////////////////////////////////////////////////////////
// receiver orientation listeners:
/////////////////////////////////////////////////////////////////


~send_OSC_ROUTINE = Routine({

	inf.do({

		var x,y,z, p,r,yaw;

		for (0, ~nSites-1, { arg i;

			x = ~site_x_BUS.getnSynchronous(~nSites)[i];
			y = ~site_y_BUS.getnSynchronous(~nSites)[i];
			z = ~site_z_BUS.getnSynchronous(~nSites)[i];

			~send_ADDRESS.sendMsg('/receiver/pos', i, x, y, z);

			p = ~site_pitch_BUS.getnSynchronous(~nSites)[i];
			r = ~site_roll_BUS.getnSynchronous(~nSites)[i];
			yaw = ~site_yaw_BUS.getnSynchronous(~nSites)[i];

			~send_ADDRESS.sendMsg('/receiver/ori', i, p, r, yaw);

		});


		for (0,~nSources-1, { arg i;

			x = ~source_x_BUS.getnSynchronous(~nSources)[i];
			y = ~source_y_BUS.getnSynchronous(~nSources)[i];
			z = ~source_z_BUS.getnSynchronous(~nSources)[i];

			~send_ADDRESS.sendMsg('/source/pos', i, x, y, z);

		});

		0.1.wait;

	});

});

// ~send_OSC_ROUTINE.next;
~send_OSC_ROUTINE.play;
