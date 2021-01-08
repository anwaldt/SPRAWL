/*

sprawl_SERVER.sc

Simple version of an OSC-controllable
audio routing matrix.


NOTE: there is an offset -
SC outputs start from index 4!

Henrik von Coler
2019-11-19
*/

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";

s.options.device               = "SPRAWL_Server";
s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 32;
s.options.maxLogins            = 4;
s.options.bindAddress          = "0.0.0.0";

~nSystems = 16;

// number of rendering outputs (virtual sound sources)
~nOutputs = 16;

// HOA Order
~hoa_order = 3;

~n_hoa_channnels = pow(~hoa_order + 1.0 ,2.0);


s.boot;

s.waitForBoot({


	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;


	load(~root_DIR++"sprawl_SYNTHDEFS.scd","r");



	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////


	~ambi_BUS             = Bus.audio(s, ~n_hoa_channnels);

	~control_azim_BUS     = Bus.control(s,~nSystems);
	~control_elev_BUS     = Bus.control(s,~nSystems);
	~control_dist_BUS     = Bus.control(s,~nSystems);

	~control_reverb_BUS   = Bus.control(s,~nSystems);

	// create one audio bus for each pi module:
	~audio_BUS_pi = Bus.audio(s,  ~nSystems);


	// create a ~nSystems x ~nSystems routing
	// matrix by using an array ofmultichannel
	// control busses:
	~gain_BUS_pi = Array.fill(~nSystems,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nSystems);
		}
	);





	// create one audio bus for each loudspeaker module:
	~rendering_send_BUS = Bus.audio(s,  ~nOutputs);

	// create a ~nSystems x ~outputs_pi routing
	// matrix by using an array ofmultichannel
	// control busses:
	~rendering_gain_BUS = Array.fill(~nSystems,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nOutputs);
		}
	);

		for (0, ~nSystems -1, {arg idx;
		~rendering_gain_BUS[idx].setAt(idx,1);
	});

	~reverb_send_BUS = Bus.audio(s,2);

	s.sync;
	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.head(s);

	for (0, ~nSystems -1, {arg idx;

		post('Adding Input Module: ');
		idx.postln;

		~inputs = ~inputs.add(
			Synth(\input_module,
				[
					\input_bus, idx,
					\output_bus_pi, ~audio_BUS_pi,
					\control_bus_pi, ~gain_BUS_pi[idx].index,
					\output_bus_rendering,  ~rendering_send_BUS,
					\control_bus_rendering, ~rendering_gain_BUS[idx].index,
				],
				target: ~input_GROUP
		);)
	});

	for (0, ~nSystems-1, {arg cnt;
		~inputs[cnt].set(\input_bus, cnt+0);
	});

	/////////////////////////////////////////////////////////////////
	// Encoder SECTION


	~encoder_GROUP = Group.after(~input_GROUP);

	for (0, ~nSystems -1, {arg cnt;

		post('Adding binaural encoder: ');
		cnt.postln;

		~binaural_encoders = ~binaural_encoders.add(
			Synth(\binaural_mono_encoder_3,
				[
					\in_bus,     ~rendering_send_BUS.index+cnt,
					\out_bus,    ~ambi_BUS.index,
					\reverb_bus, ~reverb_send_BUS
				],
				target: ~encoder_GROUP
		);)
	});

	for (0, ~nSystems -1, {arg cnt;

		post('Mapping binaural encoder: ');
		cnt.postln;

		~binaural_encoders[cnt].map(\azim,   ~control_azim_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\elev,   ~control_elev_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\dist,   ~control_dist_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\reverb, ~control_reverb_BUS.index + cnt);

	});


	/////////////////////////////////////////////////////////////////
	// OUTPUT SECTION

	~output_GROUP = Group.after(~encoder_GROUP);

	for (0, ~nSystems -1, {arg cnt;

		post('Adding PI Output Module: ');
		cnt.postln;

		~outputs_pi = ~outputs_pi.add(
			Synth(\output_module,
				[
					\audio_bus, ~audio_BUS_pi.index+cnt,
					\output, cnt,
				],
				target: ~output_GROUP
		);)
	});




	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus,  ~ambi_BUS.index,
			\out_bus, ~nSystems
		],
		target: ~output_GROUP);

	/////////////////////////////////////////////////////////////////



	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// partitioned convolution stuff (to be used with convolve-synthdef)
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	~fftsize = 4096;

	~reverb_FILE =  ~root_DIR++"../WAV/IR/kirche_1.wav";




	~conv_func_L =  {

		var ir, irbuffer, bufsize;

		irbuffer =   Buffer.readChannel(s, ~reverb_FILE, channels: [0]);

		s.sync;

		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		// ~numpartitions= PartConv.calcNumPartitions(~fftsize, irbuffer);

		~irspectrumL = Buffer.alloc(s, bufsize, 1);
		~irspectrumL.preparePartConv(irbuffer, ~fftsize);

		s.sync;

		irbuffer.free;

	}.fork;

	s.sync;



	~conv_func_R =  {

		var ir, irbuffer, bufsize;

		irbuffer = Buffer.readChannel(s, ~reverb_FILE, channels: [1]);

		s.sync;

		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		// ~numpartitions= PartConv.calcNumPartitions(~fftsize, irbuffer);

		~irspectrumR = Buffer.alloc(s, bufsize, 1);
		~irspectrumR.preparePartConv(irbuffer, ~fftsize);

		s.sync;
		irbuffer.free;

	}.fork;

	s.sync;


	2.sleep;

	post('Adding convolution reverb!');
	~conv = Synth.new(\convolve,
		[
			\outbus_1, 0,
			\outbus_2, 1,
			\bufnum_1, ~irspectrumL.bufnum,
			\bufnum_2, ~irspectrumR.bufnum,
			\fftsize,  ~fftsize
		],
		target: ~output_GROUP);

	~conv.set(\inbus_1, ~reverb_send_BUS.index);
	~conv.set(\inbus_2, ~reverb_send_BUS.index);

	~conv.set(\outbus_1, ~nSystems+2);
	~conv.set(\outbus_2, ~nSystems+3);


});


/////////////////////////////////////////////////////////////////
// OSC listeners:
/////////////////////////////////////////////////////////////////


// the routing function
~route_pi_OSC = OSCFunc(

	{arg msg, time, addr, recvPort;

		var s, r;

		/// User feedback:
		"Set pi gain: ".post;
		msg[1].post;
		" -> ".post;
		msg[2].post;
		" = ".post;
		msg[3].postln;

		s = max(0,min(msg[1],15));
		r = max(0,min(msg[2],15));

		// set the bus value:
		~gain_BUS_pi[s].setAt(r,msg[3]);

}, '/route/pi');





// the routing function
~route_binaural_OSC = OSCFunc(

	{arg msg, time, addr, recvPort;

		var s, r;

		/// User feedback:
		"Set binaural gain: ".post;
		msg[1].post;
		" -> ".post;
		msg[2].post;
		" = ".post;
		msg[3].postln;

		s = max(0,min(msg[1],15));
		r = max(0,min(msg[2],15));

		// set the bus value:
		~rendering_gain_BUS[s].setAt(r,msg[3]);

}, '/route/binaural');




// the routing function
~all_silent_OSC = OSCFunc(

	{arg msg, time, addr, recvPort;

		(0..~nSystems-1).do(
			{arg i;
				(0..~nSystems-1).do(
					{arg j;
						"Resetting: ".post; i.post; " -> ".post; j.postln;
						~gain_BUS_pi[i].setAt(j,0.0)
					}
				);
			}
		);

}, '/all_silent');


~azim_OSC = OSCFunc(
	{
		arg msg, time, addr, recvPort;
		var azim = msg[2];
		~control_azim_BUS.setAt(msg[1],azim);

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



// ServerMeter(s);
