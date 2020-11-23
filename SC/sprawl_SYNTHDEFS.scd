

/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////

SynthDef(\input_module,
	{
		|
		input_bus           = 0,
		control_bus_pi      = 0,
		output_bus_pi       = 0,
		control_bus_rendering = 0,
		output_bus_rendering  = 0,
		n_systems           = 16
		|

		var input, output, gain;

						// get the audio input from hardware input:
				input = SoundIn.ar(input_bus, 1);

		for (0, ~nSystems-1,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_bus_pi + cnt);

				// apply gain
				output = input*gain;

				// audio output to dedicated bus
				Out.ar(output_bus_pi + cnt, output);

			}
		);


		for (0, ~nOutputs-1,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_bus_rendering + cnt);

				// apply gain
				output = input*gain;

				// audio output to dedicated bus
				Out.ar(output_bus_rendering + cnt, output);

			}
		);


}).add;




SynthDef(\binaural_mono_encoder_3,
	{
		|
		in_bus     = 0,
		out_bus    = 0,
		reverb_bus = 0,
		//
		azim    = 0,
		elev    = 0,
		dist    = 1,
		gain    = 0.5,
		reverb  = 0.1
		|

		var sound = gain * In.ar(in_bus);

		var level =  (0.75/(max(0,dist)+1.0))*(0.75/(max(0,dist)+1.0));

		var bform = HOASphericalHarmonics.coefN3D(3, azim, elev) * sound * level;

		Out.ar(out_bus, bform);
		Out.ar(reverb_bus, reverb*sound);

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

		Out.ar(output, input*0.5);

}).add;


	SynthDef(\convolve,
		{
			|
			bufnum_1 = nil,
			bufnum_2 = nil,
			inbus_1  = 0,
			inbus_2  = 1,
			outbus_1 = 0,
			outbus_2 = 1,
			fftsize  = 1024
			|

			var input1 =   In.ar(inbus_1);
			var input2 =   In.ar(inbus_2);

			Out.ar(outbus_1, PartConv.ar(input1, fftsize, bufnum_1, 0.05));
			Out.ar(outbus_2, PartConv.ar(input2, fftsize, bufnum_2, 0.05));
		}
	).add;