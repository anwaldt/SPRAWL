
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Settings
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


// SET TO PI Nr. -1

~my_IDX = 0;

// set to horn file path
~horn_FILE = "/home/anwaldt/Desktop/sprawl_SYSTEM/WAV/99630__tec-studio__foghorn.wav";


s = Server.local(\sprawl_client, NetAddr("127.0.0.1", 57140));


s.options.device = "SPRAWL_remote";

// set number of audio IO:
s.options.numInputBusChannels  = 2;
s.options.numOutputBusChannels = 2;


// define OSC address and port for outgoing messages:
~sendOSC = NetAddr("127.0.0.1", 57121);


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Synthdef for the ship's horn
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


SynthDef( \sampler, {

	arg rate=1, trigger = 0, bufnum=0, startpos = 0;

	Out.ar ([0, 1],
		rate * PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum),
			trigger,
			startpos*BufFrames.kr(bufnum)),
		0)

}).add;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// AFTER SERVER BOOT:
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


s.waitForBoot({


	// read horn file to buffer
	~buffer_1 = Buffer.read(s,~horn_FILE);


	// create a window
	~window = Window.new("SPRAWL Control", Rect(0, 0, 800, 480)).front;


	~window.view.background=Color.black;


	~bus  = Bus.control();  	// create a Bus to store amplitude data

	// an audio signal
	~input = { arg gain = 0;

		var input = SoundIn.ar(0);


		Out.kr(~bus, input);

		// output
		Out.ar(0,input);

		// monitor
		Out.ar(1, input*gain);

	}.play;



	c = StaticText(~window, Rect(190, 20, 300, 20));
	c.string = "Input";
	c.stringColor = Color.grey;
	c.font = Font("Monaco", 22);

	// create indicator
	~indicator = LevelIndicator(~window,Rect(190,60,60,180));
	~indicator.style_(\led).value_(1/3).stepWidth_(4);

	~meter_ROUTINE = Routine({

		inf.do({

			~bus.get({   // get current value from the bus
				arg value;
				{~indicator.value = value.lag(0, 3);     // set Indicator's value
					~indicator.peakLevel = value.lag(0, 3); // set Indicator's peak value

				}.defer(); // schedule in the AppClock
			});

			0.1.wait;s
		});
	});

	~meter_ROUTINE.play;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	d = StaticText(~window, Rect(40, 260, 300, 20));
	d.string = "PreListen";
	d.stringColor = Color.grey;
	d.font = Font("Monaco", 22);



	~slider_PRELISTEN = Slider(~window, Rect(65, 300-15, 40, 190))

	.action_({

		~input.set(\gain,~slider_PRELISTEN.value);
		postln(~slider_PRELISTEN.value);

	});

	~slider_PRELISTEN.background =Color.new255(0, 165, 0);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	d = StaticText(~window, Rect(180, 260, 300, 20));
	d.string = "Binaural";
	d.stringColor = Color.grey;
	d.font = Font("Monaco", 22);


	~slider_BINAURAL = Slider(~window, Rect(200, 300-15, 40, 190))

	.action_({
		~sendOSC.sendMsg("/route/binaural", ~my_IDX,  ~slider_BINAURAL.value);
		postln(~slider_BINAURAL.value);
	});

	~slider_BINAURAL.background = Color.new255(0, 165, 0);





	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEND TO MEVEMENTS
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



	a = StaticText(~window, Rect(310, 5, 300, 20));
	a.string = "TO LOWER MOVEMENTS";
	a.stringColor = Color.new255(255, 0, 147);
	a.font = Font("Monaco", 22);

		a = StaticText(~window, Rect(660, 5, 300, 20));
	a.string = "UPPER";
	a.stringColor =Color.new255(77, 220, 147);
	a.font = Font("Monaco", 22);

	~sliders_MOVEMENTS = Array.fill(10,
		{arg i;

			// define coordinates
			var tmp;
			var txt;
			var x = 300+(i*50);


			Slider(~window, Rect(x, 35, 40, 190));





		}
	);



	for (0, 9, {arg i;


		var x = 300+(i*50);
		/*		var txt;

		txt = StaticText(~window, Rect(x+10, 260, 300, 20));
		txt.string = i.asString;
		txt.stringColor = Color.grey;
		txt.font = Font("Monaco", 22);*/

		~sliders_MOVEMENTS[i].background = Color.new255(255, 0, 147);


		if(i>5)
		{
					~sliders_MOVEMENTS[i].background = Color.new255(77, 220, 147);

		};


		~sliders_MOVEMENTS[i].action_(
			{
				postln(~sliders_MOVEMENTS[i].value);
				~sendOSC.sendMsg("/route/speaker", ~my_IDX, i, ~sliders_MOVEMENTS[i].value);
		});

	});




	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEND TO SPEAKERS
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




	a = StaticText(~window, Rect(390, 450, 300, 20));
	a.string = "TO LOWER SPEAKERS";
	a.stringColor = Color.new255(255, 220, 147);
	a.font = Font("Monaco", 22);


	a = StaticText(~window, Rect(710, 450, 300, 20));
	a.string = "UPPER";
	a.stringColor = Color.new255(77, 220, 147);
	a.font = Font("Monaco", 22);



	~sliders_SPEAKERS = Array.fill(10,
		{arg i;

			// define coordinates
			var tmp;
			var txt;
			var x = 300+(i*50);


			Slider(~window, Rect(x, 255, 40, 190));


		}
	);



	for (0, 9, {arg i;


		var x = 300+(i*50);
		var txt;

		txt = StaticText(~window, Rect(x+10, 230, 300, 20));
		txt.string = (i+1).asString;
		txt.stringColor = Color.grey;
		txt.font = Font("Monaco", 22);

		~sliders_SPEAKERS[i].background = Color.new255(255, 220, 147);

		if(i>7)
		{
					~sliders_SPEAKERS[i].background = Color.new255(77, 220, 147);

		};
		~sliders_SPEAKERS[i].action_(
			{
				postln(~sliders_SPEAKERS[i].value);
				~sendOSC.sendMsg("/route/speaker", ~my_IDX, i+10, ~sliders_SPEAKERS[i].value);
		});

	});

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// HORN
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	~trigger_button = Button(~window, Rect( 30, 100 ,130, 130));

	~trigger_button.states = [["HORN", Color.black, Color.red]];

	~single_sample = Synth.new(\sampler,
		[\trigger:0, \bufnum: ~buffer_1]);


	~single_sample.set(\trigger,-1);
	~trigger_button.mouseDownAction = { ~single_sample.set(\trigger,1);
		~single_sample.set(\rate,1)};

	~trigger_button.mouseUpAction = { ~single_sample.set(\trigger,-1);
		~single_sample.set(\rate,0)};

	// ~single_sample.set(\bufnum,~buffers[8].bufnum());






	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// toogle full screen
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	~fullscreen_BUTTON = Button(~window, Rect(0, 0, 100, 50))
	.states_([
		["Fullscreen", Color.black, Color.gray],
		["Quit fullscreen", Color.black, Color.gray]
	]);

	~fullscreen_BUTTON.action = { |view|
		if(view.value == 1) {
			postln("entering fullscreen");
			~window.fullScreen;

		};
		if(view.value == 0) {
			postln("leaving fulllsreen");
			~window.endFullScreen;

		};
	};

});




