

s.boot;

~buffer_1 = Buffer.read(s,"../WAV/252866__skitchscharff__queen-mary-2-ny-3.wav");


~buffer_1.numChannels()
~buffer_1.numFrames();

// s
~buffer_1.bufnum();

{PlayBuf.ar(2, ~buffer_1.bufnum(), BufRateScale.kr(~buffer_1.bufnum()))}.play;








(

~myPath = PathName.new("/home/anwaldt/Desktop/sprawl_SYSTEM/WAV/");

~myPath.filesDo
{
 |afile| // loop argument
 var tmp_path = afile.pathOnly.asSymbol++"/"++afile.fileName.asSymbol;
 ~buffers = ~buffers.add(Buffer.read(s,tmp_path));
};


~buffers.size();
~buffers[12].bufnum();

SynthDef( \sampler, {

		arg rate=1, trigger = -1, bufnum=0, startpos = 0;

		Out.ar (0,
			 rate * PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), trigger, startpos*BufFrames.kr(bufnum)),0)

}).add;


~single_sample = Synth.new(\sampler,
	[\trigger:-1, \bufnum: ~buffers[12].bufnum()]);


~single_sample.set(\trigger,-1);

~single_sample.set(\bufnum,~buffers[12].bufnum());

)









(


~window = Window.new("HORN", Rect(0, 0, 600, 300)).front;

~trigger_button = Button(~window, Rect( 250, 100 ,100, 100));

~trigger_button.states = [["HORN", Color.black, Color.red]];

)




(

~trigger_button.mouseDownAction = { ~single_sample.set(\trigger,1);
	                                ~single_sample.set(\rate,1)
};

~trigger_button.mouseUpAction = { ~single_sample.set(\trigger,-1);
	                              ~single_sample.set(\rate,0)
};

)




(

	~slider = Slider(~window, Rect(100, 200, 400, 100))
      .action_({
	 ~single_sample.set(\startpos,~slider.value);
          });

)