/*

sprawl_SERVER.sc

Simple version of an OSC-controllable
audio routing matrix for access points with
multiple inputs and outputs.

Henrik von Coler
2021-01-14

*/

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";


// some server parameters
s.options.device               = "SPRAWL_Server";
s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 64;
s.options.maxLogins            = 4;
s.options.bindAddress          = "0.0.0.0";

// maximum number of access points to be used
// @todo: this could be dynamic
~nSystems = 16;

// number of in/out channels per access point (and jacktrip connection)
// @todo: at this point, all access points need to have the same number
//        on in/outputs
~nChannels = 2;

// number of direct outputs is the same as all channels from all access points
~nSystemSends    = ~nSystems * ~nChannels;

// number of virtual sound sources
~nVirtualSources = ~nSystems * ~nChannels;

// HOA Order
~hoa_order = 3;
~n_hoa_channnels = pow(~hoa_order + 1.0 ,2.0);



s.boot;

s.waitForBoot({

	// load HOA stuff
	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;

	s.sync;

	load(~root_DIR++"sprawl_SYNTHDEFS.scd","r");


	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////


	// for the encoded ambisonics signal
	~ambi_BUS             = Bus.audio(s, ~n_hoa_channnels);

	// for the spherical control parameters
	~control_azim_BUS     = Bus.control(s,~nSystems);
	~control_elev_BUS     = Bus.control(s,~nSystems);
	~control_dist_BUS     = Bus.control(s,~nSystems);

	// reverb send level
	~control_reverb_BUS   = Bus.control(s,~nSystems);
	// audio reverb bus
	~reverb_send_BUS      = Bus.audio(s,2);


	// audio bus with mutliple channels for each pi module:
	~audio_BUS_pi = Bus.audio(s,  ~nSystemSends);

	// create a ~nSystems x ~nSystems routing
	// matrix by using an array of multichannel
	// control busses:
	~gain_BUS_pi = Array.fill(~nSystems,
		{
			Bus.control(s, ~nSystems*~nChannels*~nChannels);
		}
	);


	// create one audio bus for each virtual sound source:
	~rendering_send_BUS = Bus.audio(s,  ~nVirtualSources);

	~rendering_gain_BUS = Array.fill(~nSystems,
		{
			Bus.control(s, ~nVirtualSources*~nChannels);
		}
	);

    // per default each access points is routed to two sources
	for(0, ~nSystems -1,
		{ arg sysIDX;
			for (0, ~nChannels -1,
				{arg chanIDX;
					~rendering_gain_BUS[sysIDX].setAt((2*sysIDX)+chanIDX,1);
			});
	});


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

/*	for (0, ~nSystems-1, {arg cnt;
		~inputs[cnt].set(\input_bus, cnt+0);
	});*/

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
					\audio_bus, (~audio_BUS_pi.index)+cnt,
					\output, cnt,
				],
				target: ~output_GROUP
		);)
	});




	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus,  ~ambi_BUS.index,
			\out_bus, ~nSystems*~nChannels,
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

	~conv.set(\outbus_1, ~nSystems*~nChannels+2);
	~conv.set(\outbus_2, ~nSystems*~nChannels+3);



	load(~root_DIR++"sprawl_OSC.scd","r");


});


/*
// Bus monitoring
{
	ServerMeter(s);

	s.scope(1,~rendering_gain_BUS[0].index);


	s.scope(16,~rendering_gain_BUS[0].index);
	s.scope(16,~gain_BUS_pi[1].index);

}
*/