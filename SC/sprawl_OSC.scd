
/////////////////////////////////////////////////////////////////
// OSC routing listeners:
/////////////////////////////////////////////////////////////////


// the routing function
OSCdef(\route_pi,

	{arg msg, time, addr, recvPort;

		var from_ID, from_CH, to_ID, to_CH, gain, bus_IDX;


		from_ID = msg[1];
		from_CH = msg[2];
		to_ID   = msg[3];
		to_CH   = msg[4];
		gain    = msg[5];

		/// User feedback:
		("Set pi gain from "+from_ID+"["+from_CH+"] to "+to_ID+"["+to_CH+"] = "+gain).postln;

		bus_IDX = ((to_ID*~nChannels*~nChannels) + from_CH + (~nChannels*to_CH));
		bus_IDX.postln;


		// set the bus value:
		~gain_BUS_pi[from_ID].setAt(bus_IDX  ,msg[5]);

}, '/route/pi',n);


// the routing function
OSCdef(\route_spat,

	{arg msg, time, addr, recvPort;

		var from_ID, from_CH, to_ID, gain, index;


		from_ID = msg[1];   // = max(0,min(msg[1],~nSystems-1));
		from_CH = msg[2];   //max(0,min(msg[2],~nChannels-1));
		to_ID   = msg[3];   //max(0,min(msg[3],~nVirtualSources-1));
		gain    = msg[4];   //max(0,msg[4]);


		("Set pi gain from "+from_ID+"["+from_CH+"] to source "+to_ID+" = "+gain).postln;


		index = (~nChannels*to_ID) + from_CH;

		// set the bus value:
		~rendering_gain_BUS[from_ID].setAt(index,gain);

}, '/route/spatial',n);





/*~azim_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var gain = msg[2];
		~binaural_gain_BUS.setAt(msg[1],gain);

}, '/monitor/gain');

~azim_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var gain = msg[2];
		~binaural_mono_BUS.setAt(msg[1],gain);

}, '/monitor/mono');*/



OSCdef(\default_spatial_routing,

	{arg msg, time, addr, recvPort;

		~default_spatial_routing.choose();

}, '/default_spatial_routing',n);



OSCdef(\mute_all_spatial,

	{arg msg, time, addr, recvPort;

		~mute_all_spatial.choose();

}, '/mute_all_spatial',n);


OSCdef(\mute_all_ap,

	{arg msg, time, addr, recvPort;

		~mute_all_ap.choose();

}, '/mute_all_ap',n);


/////////////////////////////////////////////////////////////////
// OSC spatial listeners:
/////////////////////////////////////////////////////////////////

post(NetAddr.langPort);
~azim_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var azim = msg[2];
		~control_azim_BUS.setAt(msg[1],azim);
		postln('received new azim!');

}, '/source/azim');

~elev_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var elev = msg[2];
		~control_elev_BUS.setAt(msg[1],elev);

}, '/source/elev');

~dist_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var dist = msg[2];
		~control_dist_BUS.setAt(msg[1],dist);

}, '/source/dist');


~dist_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var amnt = msg[2];
		~control_reverb_BUS.setAt(msg[1],amnt);

}, '/source/reverb');


