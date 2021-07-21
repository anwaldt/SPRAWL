
/*

multi_site_SERVER.sc

Server with HOA output and reverb.

Henrik von Coler
2021-06-19

*/

// installing HOA
// Quarks.install("https://github.com/florian-grond/SC-HOA");

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";

post(~root_DIR);

// some server parameters
s.options.device               = "SPRAWL_Server";
s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 88;
s.options.maxLogins            = 4;
s.options.bindAddress          = "0.0.0.0";
// s.options.numBuffers           = 1024 * 256 * 4;
s.options.numWireBufs          = 512;

// maximum number of access points to be used
// @todo: this could be dynamic
~nSystems = 8;

// number of in/out channels per access point (and jacktrip connection)
// @todo: at this point, all access points need to have the same number
//        on in/outputs
~nChannels = 2;

// number of direct outputs is the same as all channels from all access points
~nSystemSends    = ~nSystems * ~nChannels;

// number of virtual sound sources
~nVirtualSources = ~nSystems * ~nChannels;

// HOA Order
~hoa_order       = 3;
~n_hoa_channels  = pow(~hoa_order + 1.0 ,2.0);

// setting spacial_OSC to send position data
~spatial_OSC  = NetAddr("127.0.0.1", 9595);



s.boot;

s.waitForBoot({

	// load HOA stuff
	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;

	s.sync;

	load(~root_DIR++"sprawl_SYNTHDEFS.scd","r");
	load(~root_DIR++"sprawl_FUNCTIONS.scd","r");

	s.sync;

	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////

	// for the encoded ambisonics signal
	~ambi_BUS             = Bus.audio(s, ~n_hoa_channels);

	// for the spherical control parameters
	~control_azim_BUS     = Bus.control(s,~nSystems);
	~control_elev_BUS     = Bus.control(s,~nSystems);
	~control_dist_BUS     = Bus.control(s,~nSystems);

	// reverb send level
	~control_reverb_BUS   = Bus.control(s,~nSystems);
	~control_reverb_BUS.setAll(1);

	// audio reverb bus
	~reverb_send_BUS      = Bus.audio(s,2);

	// stereo reverb
	~reverb_stereo_BUS    = Bus.audio(s,4);

	~reverb_ambi_BUS    = Bus.audio(s,~n_hoa_channels);

	// audio bus with mutliple channels for each pi module:
	~audio_BUS_pi         = Bus.audio(s,  ~nSystemSends);

	// create a ~nSystems x ~nSystems routing
	// matrix by using an array of multichannel
	// control busses:
	~gain_BUS_pi = Array.fill(~nSystems,
		{Bus.control(s, ~nSystems*~nChannels*~nChannels);}
	);


	// create one audio bus for each virtual sound source:
	~rendering_send_BUS = Bus.audio(s,  ~nVirtualSources);

	~rendering_gain_BUS = Array.fill(~nSystems,
		{Bus.control(s, ~nVirtualSources*~nChannels);}
	);

	~default_spatial_routing.choose;

	~binaural_mix_BUS  = Bus.audio(s,  2);
	~binaural_gain_BUS = Bus.control(s,  ~nSystems);
	~binaural_mono_BUS = Bus.control(s,  ~nSystems);

	// every pi is monitoring the binaural mix by default:
	for(0, ~nSystems -1,
		{ arg sysIDX; ~binaural_gain_BUS.setAt(sysIDX,1);}
	);

	~binaural_send_BUS = Bus.audio(s,  2*~nSystems);

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
					\input_bus,           idx*~nChannels,
					\output_bus_pi,       ~audio_BUS_pi,
					\control_bus_pi,      ~gain_BUS_pi[idx].index,
					\output_bus_spatial,  ~rendering_send_BUS,
					\control_bus_spatial, ~rendering_gain_BUS[idx].index,
				],
				target: ~input_GROUP
		);)
	});



	/////////////////////////////////////////////////////////////////
	// Encoder SECTION


	~encoder_GROUP = Group.after(~input_GROUP);

	s.sync;

	for (0, ~nVirtualSources -1, {arg cnt;

		post('Adding binaural encoder: ');
		cnt.postln;

		~binaural_encoders = ~binaural_encoders.add(
			Synth(\hoa_mono_encoder_3,
				[
					\in_bus,     ~rendering_send_BUS.index+cnt,
					\out_bus,    ~ambi_BUS.index,
					\reverb_bus, ~reverb_send_BUS
				],
				target: ~encoder_GROUP
		);)
	});

	s.sync;

	for (0, ~nVirtualSources -1, {arg cnt;

		post('Mapping binaural encoder: ');
		cnt.postln;

		~binaural_encoders[cnt].map(\azim,   ~control_azim_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\elev,   ~control_elev_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\dist,   ~control_dist_BUS.index   + cnt);
		~binaural_encoders[cnt].map(\reverb, ~control_reverb_BUS.index + cnt);

	});






	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Reverb
	///////////////////////////////////////////////////////////////////////////////////////////////////


	~reverb_GROUP = Group.after(~encoder_GROUP);
	s.sync;

	~fftsize = 4096;

	~reverb_FILE   =  ~root_DIR++"../WAV/IR/kirche_1.wav";
	~reverb_2_FILE =  ~root_DIR++"../WAV/IR/kirche_2.wav";

	{

		var ir, irbuffer, bufsize;

		irbuffer = Buffer.readChannel(s, ~reverb_FILE, channels: [0]);
		s.sync;

		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		~irspectrumL = Buffer.alloc(s, bufsize, 1);
		~irspectrumL.preparePartConv(irbuffer, ~fftsize);

		s.sync;


		irbuffer = Buffer.readChannel(s, ~reverb_FILE, channels: [1]);
		s.sync;
		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		~irspectrumR = Buffer.alloc(s, bufsize, 1);
		~irspectrumR.preparePartConv(irbuffer, ~fftsize);

		s.sync;

		irbuffer.free;

	}.fork;




	{

		var ir, irbuffer, bufsize;

		irbuffer = Buffer.readChannel(s, ~reverb_2_FILE, channels: [0]);
		s.sync;

		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		~irspectrumL2 = Buffer.alloc(s, bufsize, 1);
		~irspectrumL2.preparePartConv(irbuffer, ~fftsize);

		s.sync;


		irbuffer = Buffer.readChannel(s, ~reverb_2_FILE, channels: [1]);
		s.sync;
		bufsize = PartConv.calcBufSize(~fftsize, irbuffer);

		~irspectrumR2 = Buffer.alloc(s, bufsize, 1);
		~irspectrumR2.preparePartConv(irbuffer, ~fftsize);

		s.sync;

		irbuffer.free;

	}.fork;

	3.sleep;

	postln('Adding convolution reverb 1!');
	~conv = Synth.new(\convolve,
		[
			\outbus_1, 0,
			\outbus_2, 1,
			\bufnum_1, ~irspectrumL.bufnum,
			\bufnum_2, ~irspectrumR.bufnum,
			\fftsize,  ~fftsize
		],
		target: ~reverb_GROUP);
	s.sync;

	~conv.set(\inbus_1, ~reverb_send_BUS.index);
	~conv.set(\inbus_2, ~reverb_send_BUS.index);

	~conv.set(\outbus_1, ~reverb_stereo_BUS.index);
	~conv.set(\outbus_2, ~reverb_stereo_BUS.index+1);



	postln('Adding convolution reverb 2!');
	~conv = Synth.new(\convolve,
		[
			\outbus_1, 0,
			\outbus_2, 1,
			\bufnum_1, ~irspectrumL2.bufnum,
			\bufnum_2, ~irspectrumR2.bufnum,
			\fftsize,  ~fftsize
		],
		target: ~reverb_GROUP);


	~conv.set(\inbus_1, ~reverb_send_BUS.index);
	~conv.set(\inbus_2, ~reverb_send_BUS.index);

	~conv.set(\outbus_1, ~reverb_stereo_BUS.index+2);
	~conv.set(\outbus_2, ~reverb_stereo_BUS.index+3);
	s.sync;






	/*	~hoa_reverb_sources = {
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+0), 0, pi/2));
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+1), 0, -pi/2));
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+0), pi, 0));
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+1), -pi/2, 0));
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+0), pi, 0));
	Out.ar(~ambi_BUS.index, HOAEncoder.ar(~hoa_order,In.ar(~reverb_stereo_BUS.index+1), -pi/2, 0));
	}.play;
	s.sync;*/

	~hoa_reverb_sources = {
		Out.ar(~ambi_BUS.index+0,  In.ar(~reverb_stereo_BUS.index+3));
		Out.ar(~ambi_BUS.index+1,  In.ar(~reverb_stereo_BUS.index+0));
		Out.ar(~ambi_BUS.index+2,  In.ar(~reverb_stereo_BUS.index+1));
		Out.ar(~ambi_BUS.index+3,  In.ar(~reverb_stereo_BUS.index+2));
	}.play;
	s.sync;

	~hoa_reverb_sources.moveToTail(~reverb_GROUP);
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// DECODER
	///////////////////////////////////////////////////////////////////////////////////////////////////

	~spatial_GROUP = Group.after(~reverb_GROUP);
	s.sync;

	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus,  ~ambi_BUS.index,
			\out_bus, ~binaural_mix_BUS.index,
		],
		target: ~spatial_GROUP);

	s.sync;

	~decoder.set(\out_bus, ~binaural_mix_BUS);

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// OUTPUT SECTION
	///////////////////////////////////////////////////////////////////////////////////////////////////

	~output_GROUP = Group.after(~spatial_GROUP);
	s.sync;

	for (0, ~nSystemSends -1, {arg cnt;

		post('Adding PI Output Module: ');
		cnt.postln;

		~outputs_pi = ~outputs_pi.add(
			Synth(\output_module,
				[
					\audio_bus, (~audio_BUS_pi.index)+cnt,
					\output, cnt,
				],
				target: ~output_GROUP
		);)
	});
	s.sync;

	Synth.new(\binaural_send,
		[
			\binaural_bus, ~binaural_mix_BUS.index,
			\gain_bus,	   ~binaural_gain_BUS.index,
			\mono_bus,     ~binaural_mono_BUS.index,
			\output_bus,   32
		],
		target: ~output_GROUP);
	s.sync;



	for (0, 1, {arg cnt;

		post('Adding MAIN Output Module: ');
		cnt.post;
		post(' - To bus: ');
		(64+cnt).postln;

		~outputs_main = ~outputs_main.add(
			Synth(\output_module,
				[
					\audio_bus, (~binaural_mix_BUS.index)+cnt,
					\output, 64+cnt,
				],
				target: ~output_GROUP
		);)
	});
	s.sync;


	for (0, ~n_hoa_channels-1, {arg cnt;

		post('Adding HOA Encoded Output Module: ');
		cnt.post;
		post(' - To bus: ');
		(66+cnt).postln;

		~outputs_encoded = ~outputs_encoded.add(
			Synth(\output_module,
				[
					\audio_bus, (~ambi_BUS.index)+cnt,
					\output, 66+cnt,
				],
				target: ~output_GROUP
		);)
	});



	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////

	load(~root_DIR++"sprawl_OSC.scd","r");

	NetAddr.langPort;


});

