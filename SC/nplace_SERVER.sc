/*

nsite_SERVER.sc

Connect multiple sites with multichannel systems.

Henrik von Coler
2021-01-14

*/

// installing HOA
// Quarks.install("https://github.com/florian-grond/SC-HOA");

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";
post(~root_DIR);

// some server parameters
s.options.device               = "nsite_SERVER";
s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 64;
s.options.maxLogins            = 4;
s.options.bindAddress          = "0.0.0.0";


~nSites = 3;

// number of virtual sound sources
~nSources = 2*~nSites;

// HOA Order
~hoa_order = 3;

~n_hoa_channels = pow(~hoa_order + 1.0 ,2.0).asInteger;

// setting spacial_OSC to send position data
~send_ADDRESS  = NetAddr("127.0.0.1", 5005);

s.boot;

s.waitForBoot({

	// load HOA stuff
	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;

	s.sync;

	load(~root_DIR++"sprawl_SYNTHDEFS.scd","r");

	s.sync;

	// Buses for spatial control
	~source_x_BUS = Bus.control(s,~nSources);
	~source_y_BUS = Bus.control(s,~nSources);
	~source_z_BUS = Bus.control(s,~nSources);
	// reverb send level
	~source_gain_BUS    = Bus.control(s,~nSources);
	~source_gain_BUS.setAll(1);
	~source_reverb_BUS  = Bus.control(s,~nSources);
	~source_reverb_BUS.setAll(1);

	~site_x_BUS = Bus.control(s,~nSites);
	~site_y_BUS = Bus.control(s,~nSites);
	~site_z_BUS = Bus.control(s,~nSites);

	~site_pitch_BUS = Bus.control(s,~nSites);
	~site_roll_BUS  = Bus.control(s,~nSites);
	~site_yaw_BUS   = Bus.control(s,~nSites);


	// each input channel (source) has a bus
	~input_BUS = Bus.audio(s,~nSources);

	// audio send reverb buses
	~reverb_send_BUS      = Bus.audio(s,1);
	// stereo reverb
	~reverb_stereo_BUS    = Bus.audio(s,2);

	// each site has its own ambisonics bus
	for (0, ~nSites-1,
		{~ambi_BUS = ~ambi_BUS.add(Bus.audio(s,~n_hoa_channels));}
	);


	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Input
	///////////////////////////////////////////////////////////////////////////////////////////////////


	~input_GROUP = Group.new(s);

	s.sync;

	for (0, ~nSources-1, { arg i;
		~sources = ~sources.add(Synth.new(\simple_input, [],target: ~input_GROUP));
	});

	s.sync;

	for (0, ~nSources-1, { arg i;
		~sources[i].set(\in_bus, i, \out_bus, ~input_BUS.index+i,
			\reverb_bus, ~reverb_send_BUS.index,
			\reverb_gain,~source_reverb_BUS.index+i);
		~sources[i].map(\gain, ~source_gain_BUS.index+i, \reverb_gain,~source_reverb_BUS.index+i);
	});

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Encoding
	///////////////////////////////////////////////////////////////////////////////////////////////////


	~encoder_GROUP = Group.after(~input_GROUP);

	s.sync;

	for (0, ~nSites-1, { arg i;
		~sites = ~sites.add( SprawlReceiverCartesian.new(s,~encoder_GROUP,~nSources))
	});

	3.sleep;

	// map control buses for all sites
	for (0, ~nSites-1, { arg i;
		// and all sources
		for (0, ~nSources-1, { arg k;

			~sites[i].translators()[k].map(\xS, ~source_x_BUS.index+k);
			~sites[i].translators()[k].map(\yS, ~source_y_BUS.index+k);
			~sites[i].translators()[k].map(\zS, ~source_z_BUS.index+k);

			~sites[i].translators()[k].map(\xR, ~site_x_BUS.index+i);
			~sites[i].translators()[k].map(\yR, ~site_y_BUS.index+i);
			~sites[i].translators()[k].map(\zR, ~site_z_BUS.index+i);

			~sites[i].translators()[k].map(\pitch, ~site_pitch_BUS.index+i);
			~sites[i].translators()[k].map(\roll,  ~site_roll_BUS.index+i);
			~sites[i].translators()[k].map(\yaw,   ~site_yaw_BUS.index+i);

		});
	});

	// set inputs and outputs of all sites
	for (0, ~nSites-1, { arg i;
		for (0, ~nSources-1, { arg k;
			~sites[i].encoders()[k].set(\in_bus,  ~input_BUS.index+k);
			~sites[i].encoders()[k].set(\out_bus, ~ambi_BUS[i].index);
		});
	});


	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Decoding
	///////////////////////////////////////////////////////////////////////////////////////////////////

	~decoder_GROUP = Group.after(~encoder_GROUP);

	s.sync;

	~binauralDecoders = Array.fill(~nSites,
		{ arg i;
			Synth(\hoa_binaural_decoder_3,
				[   \in_bus,  ~ambi_BUS[i].index,
					\out_bus, i*2],
				target: ~decoder_GROUP)
	});


	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////


	~output_GROUP = Group.after(~decoder_GROUP);


	~fftsize = 4096;

	~reverb_FILE =  ~root_DIR++"../WAV/IR/kirche_1.wav";

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


	3.sleep;

	postln('Adding convolution reverb!');
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

	~conv.set(\outbus_1, ~nSites*2);
	~conv.set(\outbus_2, ~nSites*2+1);


	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////


	for (0, ~nSites-1, { arg i;
		~hoa_outputs = ~hoa_outputs.add(
			{Out.ar((2*~nSites+2) + (i*~n_hoa_channels), In.ar(~ambi_BUS[i].index, ~n_hoa_channels))}.play());
	});

	s.sync;

	~hoa_outputs.do({arg e; e.moveToTail(~output_GROUP)});


	load(~root_DIR++"nplace_OSC.scd","r");


});



