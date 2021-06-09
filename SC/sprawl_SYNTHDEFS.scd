

/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////



SynthDef(\simple_input,
	{
		|
		in_bus      = nil,
		out_bus     = nil,
		reverb_bus  = nil,
		//
		gain        = 1,
		reverb_gain = 0.2
		|

		var in;

		in = SoundIn.ar(in_bus);

		Out.ar(out_bus,    in*gain);
		Out.ar(reverb_bus, in*gain*reverb_gain);

}).add;


if( ~nChannels!=nil && ~nVirtualSources!=nil && ~nSystems!=nil, {



	SynthDef(\input_module,
		{
			|
			input_bus           = 0,
			control_bus_pi      = 0,
			output_bus_pi       = 0,
			control_bus_spatial = 0,
			output_bus_spatial  = 0,
			n_systems           = 16
			|

			var input, output, gain;

			for (0, ~nChannels-1,
				{ arg from_chanCNT;

					input = SoundIn.ar(input_bus+from_chanCNT);


					// send to spatial

					for (0, ~nVirtualSources-1,
						{ arg srcCNT;

							// get the gain value from control bus:
							gain = In.kr((control_bus_spatial + (~nChannels*srcCNT)) +  from_chanCNT);

							// apply gain
							output = input*gain;

							// audio output to dedicated bus
							Out.ar(output_bus_spatial+ srcCNT, output);

						}
					);


					// send to system

					for (0, ~nSystems-1,
						{ arg sysCNT;


							for (0, ~nChannels-1,
								{ arg to_chanCNT;

									// get the gain value from control bus:
									gain = In.kr((control_bus_pi + (sysCNT*~nChannels*~nChannels) + from_chanCNT + (~nChannels*to_chanCNT)));

									// apply gain
									output = input*gain;

									// audio output to dedicated bus
									Out.ar(output_bus_pi+ (~nChannels*sysCNT) +  to_chanCNT, output);

								}
							);
						}
					);



			});

	}).add;


});



SynthDef(\hoa_mono_encoder_3,
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

		var level =  (1/(max(0,0.2*dist)+1.0))*(1/(max(0,0.2*dist)+1.0));

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

		Out.ar(output, input);

}).add;




if(~nSystems!=nil, {

SynthDef(\binaural_send,
	{
		|
		binaural_bus = nil,
		gain_bus     = nil
		output_bus   = nil,
		mono_bus     = nil
		|

		var input, gain, output, mono;

		for (0, ~nSystems-1,
			{arg sysCNT;

				input = In.ar(binaural_bus,2);
				gain  = In.kr(gain_bus+sysCNT);

				mono = In.kr(mono_bus+sysCNT);
				mono = max(0,min(1,mono));

				// left
				Out.ar(output_bus+(2*sysCNT),    ((input[0]*gain) + (input[1]*gain*mono)) * (1-(0.75*mono)));

				// right
				Out.ar(output_bus+(2*sysCNT)+1,  (input[1]*gain*(1-mono)));

		});


}).add;

});

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