/*

sprawl_SERVER.sc

Simple version of an OSC-controllable
audio routing matrix.


NOTE: there is an offset -
SC outputs start from index 4!

Henrik von Coler
2019-11-19
*/

s = Server(\sprawl_server, NetAddr("127.0.0.1", 58009));

// s.options.device = "SPRAWL_server";

s.options.numInputBusChannels  = 16;
s.options.numOutputBusChannels = 48;

s.boot;

~nSystems = 16;

// number of outputs to the sound system
~nStereo  = 20;

~sendOSC  = NetAddr("127.0.0.1", 57121);

// thisProcess.openUDPPort(57121);











/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////

SynthDef(\input_module,
	{
		|
		input_bus           = nil,
		control_BUS_speaker = nil,
		output_bus          = nil,
		n_systems           = 16
		|

		var in1, in2, output, gain;

		for (0, 19,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_BUS_speaker + cnt);

				// get the audio input from hardware input:
				in1 = SoundIn.ar(input_bus);
				in2 = SoundIn.ar(input_bus+1);

				// audio output to dedicated bus
				Out.ar(output_bus + (cnt*2), [in1*gain, in2*gain]);

			}
		);


}).add;


SynthDef(\output_module,
	{
		|
		audio_bus = nil,
		out_bus   = nil
		|

		Out.ar(out_bus, In.ar(audio_bus,2));

}).add;





s.waitForBoot({


	/////////////////////////////////////////////////////////////////
// THE BUSSES:
/////////////////////////////////////////////////////////////////


	// create a ~nSystems x ~outputs_pi routing
	// matrix by using an array ofmultichannel
	// control busses:
	~gain_BUS_speaker = Array.fill(~nSystems,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nStereo);
		}
	);



	// create one audio bus for each spatialization/loudspeaker module:
	~audio_BUS_speaker = Bus.audio(s, ~nStereo * 2);


	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.head(s);

	for (0, ~nSystems -1, {arg idx;

		post('Adding Input Module: ');
		idx.postln;

		~inputs = ~inputs.add(
			Synth(\input_module,
				[
					\input_bus, idx*2,
					// \output_bus, idx*2, // direct to outputs is working fine
					\output_bus, ~audio_BUS_speaker.index,
					\control_BUS_speaker, ~gain_BUS_speaker[idx].index,
				],
				target: ~input_GROUP
		);)
	});


	/*
	for (0, ~nSystems-1, {arg cnt;
	~inputs[cnt].set(\input_bus, cnt+0);
	});
	*/

	/////////////////////////////////////////////////////////////////
	// OUTPUT SECTION


	~output_GROUP = Group.after(~input_GROUP);



	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////

	for (0, ~nStereo -1, {arg cnt;

		post('Adding speaker Output Module: ');
		cnt.postln;


		post('With output: ');
		(cnt*2).postln;


		~outputs_speaker = ~outputs_speaker.add(
			Synth(\output_module,
				[
					\audio_bus, ~audio_BUS_speaker.index + (2*cnt),
					\out_bus, cnt*2,
				],
				target: ~output_GROUP
		);)
	});

	/*	for (0, ~nSystems-1,
	{
	arg cnt;
	~outputs_speaker[cnt].set(\output, cnt+16);
	});*/


	post("Listening on port: ");
	postln(NetAddr.langPort);
	ServerMeter(s);








	/////////////////////////////////////////////////////////////////
	// OSC listeners:
	/////////////////////////////////////////////////////////////////




	// the routing function
	~route_speaker_OSC = OSCFunc(

		{arg msg, time, addr, recvPort;

			var s, r;

			/// User feedback:
			// "Set speaker gain: ".post;
			// msg[1].post;
			// " -> ".post;
			// msg[2].post;
			// " = ".post;
			// msg[3].postln;

			s = max(0,min(msg[1],15));
			r = max(0,min(msg[2],19));

			// set the bus value:
			~gain_BUS_speaker[s].setAt(r,msg[3]);

	}, '/route/speaker');



	~sendOSC_routine = Routine({

		inf.do({

			for (0, ~nSystems-1, { arg i;

				var gains    = ~gain_BUS_speaker[i].getnSynchronous(~nStereo);

				for (0, ~nStereo-1, { arg j;

					~sendOSC.sendMsg('/route/speaker', i, j, gains[j]);

					//0.001.wait;
				});

			});

			0.01.wait;
		});

	});

	~sendOSC_routine.play;

});






