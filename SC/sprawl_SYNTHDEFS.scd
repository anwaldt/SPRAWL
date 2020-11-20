

/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////

SynthDef(\input_module,
	{
		|
		input_bus           = 0,
		control_bus_pi      = 0,
		output_bus_pi       = 0,
		control_bus_speaker = 0,
		output_bus_speaker  = 0,
		n_systems           = 16
		|

		var input, output, gain;

		for (0, 15,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_bus_pi + cnt);

				// get the audio input from hardware input:
				input = SoundIn.ar(input_bus, 1);

				// apply gain
				output = input*gain;

				// audio output to dedicated bus
				Out.ar(output_bus_pi + cnt, output);

			}
		);


		for (0, 15,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_bus_speaker + cnt);

				// get the audio input from hardware input:
				input = SoundIn.ar(input_bus, 1);

				// apply gain
				output = input*gain;

				// audio output to dedicated bus
				Out.ar(output_bus_speaker + cnt, output);

			}
		);


}).add;




SynthDef(\binaural_mono_encoder_3,
	{
		|
		in_bus  = nil,
		out_bus = 0,
		azim    = 0,
		elev    = 0,
		dist    = 3,
		gain    = 0.5
		|

		var sound = gain * In.ar(in_bus);
		var level = (1.0/(dist+1.0))*(1.0/(dist+1.0));
		var bform = HOASphericalHarmonics.coefN3D(3, azim, elev) * sound * level;

		Out.ar(out_bus, bform);

}).add;


SynthDef(\hoa_binaural_decoder_3,
	{
		|
		in_bus  = 0,
		out_bus = 0
		|

		var sig = HOABinaural.ar(3, In.ar(in_bus,16));
		Out.ar(out_bus, sig);

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
