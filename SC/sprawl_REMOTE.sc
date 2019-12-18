

// minimize audio IO:
s.options.numInputBusChannels  = 1;
s.options.numOutputBusChannels = 1;

// define for outgoing messages:
~sendOSC = NetAddr("127.0.0.1", 57120);

// we are defining a first simple synth:
SynthDef(\sine,
	{
		|f = 100, a=0.5|

		var out;

		out = SinOsc.ar(f,0,a);

		Out.ar(0, out);
}).add;



Server.default.waitForBoot({


	// ~sine = Synth.new(\sine,

	// create a window
	~window = Window.new("SPRAWL Control", Rect(0, 0, 600, 300)).front;

	~myID = 0;

	~bus  = Bus.control();  	// create a Bus to store amplitude data

	// an audio signal
	~input = {

		var input = SoundIn.ar(0);

		Out.kr(~bus, input);  // write amplitude data to control bus
		Out.ar(0,input);   // write sound to output bus
	}.play;

	~slider = Slider(~window, Rect(200, 60, 20, 150))
      .action_({
          c.value_(a.value)
          });

	// create indicator
	~indicator = LevelIndicator(~window,Rect(10,10,40,200));

	~meter_ROUTINE = Routine({

		inf.do({

			~bus.get({   // get current value from the bus
				arg value;
				{~indicator.value_(value);     // set Indicator's value
					~indicator.peakLevel_(value); // set Indicator's peak value
				}.defer(); // schedule in the AppClock
			});

			0.1.wait;
		});
	});

~meter_ROUTINE.play;

	~buttons = Array.fill(16,
		{arg i;

			// define coordinates
			var x = 80+((i%4)*50);
			var y = 0 + (round((i+2)/4,1)*50);

			Button(~window, Rect( x, y ,40, 40));
		}
	);


	for (0, 15, {arg i;

		~buttons[i].states = [[i.asString, Color.black, Color.red],
			[i.asString, Color.white, Color.green]];

		~buttons[i].action = {
			~sendOSC.sendMsg("/route",i, 0 ,~buttons[i].value);


		};

	});




});




