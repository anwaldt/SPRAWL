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


s.options.numInputBusChannels  = 64;
s.options.numOutputBusChannels = 64;

~nSystems = 16;

// number of outputs to the sound system
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
					\output_bus_rendering, ~rendering_send_BUS,
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
					\audio_bus, ~audio_BUS_pi.index+cnt,
					\output, cnt,
				],
				target: ~output_GROUP
		);)
	});






	/////////////////////////////////////////////////////////////////
	// OUTPUT SECTION


	~output_GROUP = Group.after(~encoder_GROUP);



	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus,~ambi_BUS.index,
			\out_bus, 0
		],
		target: ~output_GROUP);


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

	// for (0, ~nSystems-1,
	// 	{
	// 		arg cnt;
	// 		~outputs_pi[cnt].set(\output, cnt);
	// });


	/////////////////////////////////////////////////////////////////




	for (0, ~nSystems -1, {arg cnt;

		post('Adding speaker Output Module: ');
		cnt.postln;

		~outputs_speaker = ~outputs_speaker.add(
			Synth(\output_module,
				[
					\audio_bus, ~rendering_send_BUS.index+cnt,
					\output, cnt+16,
				],
				target: ~output_GROUP
		);)
	});

	/*	for (0, ~nSystems-1,
	{
	arg cnt;
	~outputs_speaker[cnt].set(\output, cnt+16);
	});*/



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



ServerMeter(s);