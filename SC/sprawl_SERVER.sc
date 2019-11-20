/*

sprawl_SERVER.sc

Simple version of an OSC-controllable
audio routing matrix.


NOTE: there is an offset -
      SC outputs start from index 4!

Henrik von Coler
2019-11-19
*/

s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 32;

~nSystems = 16;

/////////////////////////////////////////////////////////////////
// THE BUSSES:
/////////////////////////////////////////////////////////////////

// create one audio bus for each output module:
~audio_BUS = Bus.audio(s,  ~nSystems);

// create a ~nSystems x ~nSystems routing
// matrix by using an array ofmultichannel
// control busses:
~gain_BUS = Array.fill(~nSystems,
	{
		// arg i;
		// "Creating control busses for system: ".post;
		// i.postln;
		Bus.control(s, ~nSystems);
	}
);

/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////

SynthDef(\input_module,
	{
		|
		input_bus   = 0,
		control_bus = 0,
		output_bus  = 0,
		n_systems   = 16
		|

		var input, output, gain;

		for (0, 15,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_bus + cnt);

				// get the audio input from hardware input:
				input = SoundIn.ar(input_bus, 1);

				// apply gain
				output = input*gain;

				// audio output to dedicated bus
				Out.ar(output_bus + cnt, output);

			}
		);
}).add;


SynthDef(\output_module,
	{
		|
		audio_bus   = 0,
		output      = 0
		|

		var input;

		input = In.ar(audio_bus,1);

		Out.ar(output+4, input*0.5);

}).add;





s.waitForBoot({


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
					\output_bus, ~audio_BUS,
					\control_bus, ~gain_BUS[idx].index,
				],
				target: ~input_GROUP
		);)
	});

	for (0, ~nSystems-1, {arg cnt;
		~inputs[cnt].set(\input_bus, cnt+0);
	});

	/////////////////////////////////////////////////////////////////
	// OUTPUT SECTION


	~output_GROUP = Group.after(~input_GROUP);

	for (0, ~nSystems -1, {arg cnt;

		post('Adding Output Module: ');
		cnt.postln;

		~outputs = ~outputs.add(
			Synth(\output_module,
				[
					\audio_bus, ~audio_BUS.index+cnt,
					\output, cnt,
				],
				target: ~output_GROUP
		);)
	});

	for (0, ~nSystems-1,
		{
			arg cnt;
			~outputs[cnt].set(\output, cnt);
	});

});




/////////////////////////////////////////////////////////////////
// OSC listeners:
/////////////////////////////////////////////////////////////////


// the routing function
~route_channel_OSC = OSCFunc(

	{arg msg, time, addr, recvPort;

		var s, r;

		/// User feedback:
		"Set gain: ".post;
		msg[1].post;
		" -> ".post;
		msg[2].post;
		" = ".post;
		msg[3].postln;

		s = max(0,min(msg[1],15));
		r = max(0,min(msg[2],15));

		// set the bus value:
		~gain_BUS[s].setAt(r,msg[3]);

}, '/route');


// the routing function
~all_silent_OSC = OSCFunc(

	{arg msg, time, addr, recvPort;

		(0..~nSystems-1).do(
			{arg i;
				(0..~nSystems-1).do(
					{arg j;
						"Resetting: ".post; i.post; " -> ".post; j.postln;
						~gain_BUS[i].setAt(j,0.0)
					}
				);
			}
		);

}, '/all_silent');


ServerMeter(s);