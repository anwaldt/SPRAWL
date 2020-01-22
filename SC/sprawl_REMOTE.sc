



// minimize audio IO:
s.options.numInputBusChannels  = 1;
s.options.numOutputBusChannels = 1;

// define for outgoing messages:
~sendOSC = NetAddr("127.0.0.1", 57120);




Server.default.waitForBoot({


	// ~sine = Synth.new(\sine,

	// create a window
	~window = Window.new("SPRAWL Control", Rect(0, 0, 800, 480)).front;

	~window.view.background=Color.black;

	//~window.fullScreen;

	~myID = 0;

	~bus  = Bus.control();  	// create a Bus to store amplitude data

	// an audio signal
	~input = {

		var input = 4* SoundIn.ar(0);

		Out.kr(~bus, input);  // write amplitude data to control bus
		Out.ar(0,input);   // write sound to output bus
	}.play;



    c = StaticText(~window, Rect(10, 270, 300, 20));
	c.string = "Input";
	c.stringColor = Color.grey;
	c.font = Font("Monaco", 22);

	// create indicator
	~indicator = LevelIndicator(~window,Rect(20,300,40,150));

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



	d = StaticText(~window, Rect(100, 260, 300, 20));
	d.string = "GAIN";
	d.stringColor = Color.grey;
	d.font = Font("Monaco", 22);



	~slider2 = Slider(~window, Rect(110, 300-15, 40, 190))

	.action_({
		c.value_(a.value)
	});

		~slider2.background =Color.new255(255, 165, 0);


		d = StaticText(~window, Rect(200, 260, 300, 20));
	d.string = "Binaural";
	d.stringColor = Color.grey;
	d.font = Font("Monaco", 22);


		~slider3 = Slider(~window, Rect(220, 300-15, 40, 190))

	.action_({
		c.value_(a.value)
	});

		~slider3.background = Color.new255(255, 20, 147);


	a = StaticText(~window, Rect(10, 10, 300, 20));
	a.string = "Monitor Access Points";
	a.stringColor = Color.grey;
	a.font = Font("Monaco", 22);

	~buttons = Array.fill(16,
		{arg i;

			// define coordinates
			var x = 10+((i%4)*50);
			var y = 0 + (round((i+2)/4,1)*50);

			Button(~window, Rect( x, y ,40, 40));
		}
	);



	~slider = Slider(~window, Rect(220, 50, 40, 190))

	.action_({
		c.value_(a.value)
	});

	~slider.background = Color.new255(255, 20, 147);

	b = StaticText(~window, Rect(530, 30, 200, 40));
	b.string = "XY POS";
	b.stringColor = Color.grey;
	b.font = Font("Monaco", 33);

t = Slider2D(~window, Rect(400-5, 75, 400, 400))
        .x_(0.5) // initial location of x
        .y_(1)   // initial location of y
        .action_({|sl|
            [\sliderX, sl.x, \sliderY, sl.y].postln;
        });


	for (0, 15, {arg i;

		~buttons[i].states = [[i.asString, Color.black, Color.gray],
			[i.asString, Color.white, Color.new255(255, 20, 147)]];

		~buttons[i].action = {
			~sendOSC.sendMsg("/route",i, 0 ,~buttons[i].value);


		};

	});




});




