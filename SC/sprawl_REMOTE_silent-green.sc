

~my_IDX = 1;

// minimize audio IO:
s.options.numInputBusChannels  = 2;
s.options.numOutputBusChannels = 2;

// define for outgoing messages:
~sendOSC = NetAddr("191.168.0.1", 57120);

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";

~buffer_1 = Buffer.read(s,"/home/anwaldt/Desktop/sprawl_SYSTEM/WAV/99630__tec-studio__foghorn.wav");




SynthDef( \sampler, {

	arg rate=1, trigger = 0, bufnum=0, startpos = 0;

	Out.ar (0,
		rate * PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum),
			trigger,
			startpos*BufFrames.kr(bufnum)),
		0)

}).add;



Server.default.waitForBoot({


	~sample_PATH = PathName.new(~root_DIR++"../WAV/SHIP_HORNS");

	~sample_PATH.filesDo
	{
		|afile| // loop argument
		var tmp_path = afile.pathOnly.asSymbol++afile.fileName.asSymbol;
		~sample_BUFFERS = ~sample_BUFFERS.add(Buffer.readChannel(s,tmp_path,0,-1,0));
	};

	"Read "++~sample_BUFFERS.size()++" samples!";


	s.sync;

	// ~sine = Synth.new(\sine,

	// create a window
	~window = Window.new("SPRAWL Control", Rect(0, 0, 800, 480)).front;

	~window.view.background=Color.black;

	//~window.fullScreen;

	~myID = 0;

	~bus  = Bus.control();  	// create a Bus to store amplitude data

	// an audio signal
	~input = { arg gain = 1;

		var input = SoundIn.ar(0);


		Out.kr(~bus, input);

		// output
		Out.ar(0,input);

		// monitor
		Out.ar(1, input*gain);

	}.play;



	c = StaticText(~window, Rect(50, 20, 300, 20));
	c.string = "Input";
	c.stringColor = Color.grey;
	c.font = Font("Monaco", 22);

	// create indicator
	~indicator = LevelIndicator(~window,Rect(70,60,40,180));

	~meter_ROUTINE = Routine({

		inf.do({

			~bus.get({   // get current value from the bus
				arg value;
				{~indicator.value_(10*value);     // set Indicator's value
					~indicator.peakLevel_(value); // set Indicator's peak value
				}.defer(); // schedule in the AppClock
			});

			0.1.wait;
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



	/*a = StaticText(~window, Rect(500, 10, 300, 20));
	a.string = "TO MOVEMENTS";
	a.stringColor = Color.grey;
	a.font = Font("Monaco", 22);*/

	~sliders_MOVEMENTS = Array.fill(10,
		{arg i;

			// define coordinates
			var tmp;
			var txt;
			var x = 300+(i*50);


			Slider(~window, Rect(x, 50, 40, 190));





		}
	);



	for (0, 9, {arg i;


		var x = 300+(i*50);
		var txt;

		txt = StaticText(~window, Rect(x+10, 260, 300, 20));
		txt.string = i.asString;
		txt.stringColor = Color.grey;
		txt.font = Font("Monaco", 22);

		~sliders_MOVEMENTS[i].background = Color.new255(255, 0, 147);

		~sliders_MOVEMENTS[i].action_(
			{
				postln(~sliders_MOVEMENTS[i].value);
				~sendOSC.sendMsg("/route/speakers", ~my_IDX, i, ~sliders_MOVEMENTS[i].value);
		});

	});




	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEND TO SPEAKERS
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



	/*	a = StaticText(~window, Rect(500, 200, 300, 20));
	a.string = "TO SPEAKERS";
	a.stringColor = Color.grey;
	a.font = Font("Monaco", 22);*/



	~sliders_SPEAKERS = Array.fill(10,
		{arg i;

			// define coordinates
			var tmp;
			var txt;
			var x = 300+(i*50);


			Slider(~window, Rect(x, 280, 40, 190));


		}
	);



	for (0, 9, {arg i;


		var x = 300+(i*50);
		var txt;

		txt = StaticText(~window, Rect(x+10, 260, 300, 20));
		txt.string = i.asString;
		txt.stringColor = Color.grey;
		txt.font = Font("Monaco", 22);

		~sliders_SPEAKERS[i].background = Color.new255(255, 220, 147);

		~sliders_SPEAKERS[i].action_(
			{
				postln(~sliders_SPEAKERS[i].value);
				~sendOSC.sendMsg("/route/speakers", ~my_IDX, i+10, ~sliders_SPEAKERS[i].value);
		});

	});

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// HORN
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	~trigger_button = Button(~window, Rect( 150, 100 ,100, 100));

	~trigger_button.states = [["HORN", Color.black, Color.red]];

	~single_sample = Synth.new(\sampler,
		[\trigger:0, \bufnum: ~sample_BUFFERS[~my_IDX]]);


	~single_sample.set(\trigger,-1);
	~trigger_button.mouseDownAction = { ~single_sample.set(\trigger,1);
		~single_sample.set(\rate,1)};

	~trigger_button.mouseUpAction = { ~single_sample.set(\trigger,-1);
		~single_sample.set(\rate,0)};

	// ~single_sample.set(\bufnum,~buffers[8].bufnum());





});





