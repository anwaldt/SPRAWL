
~ambi_BUS[0].scope;



~hoa_outputs.do({arg e; e.free()});


// Set buses manually for testing

~source_gain_BUS.setAll(1);
~source_gain_BUS.setAt(0,2);

~source_x_BUS.scope;
~source_x_BUS.setAt(0,5);
~source_x_BUS.setAll(1);

~source_y_BUS.scope;
~source_y_BUS.setAt(0,0);
~source_y_BUS.setAll(-0.2);

~source_z_BUS.setAt(0,0);
~source_z_BUS.setAll(0.5);

~site_x_BUS.scope;
~site_x_BUS.setAll(1);
~site_x_BUS.setAt(0,0);

~site_y_BUS.setAt(0,0);

~site_z_BUS.setAt(0,0);

~site_pitch_BUS.setAt(0,0);
~site_roll_BUS.setAt(0,1);
~site_yaw_BUS.setAt(0,0);

~sites[0].azim().scope;
~sites[0].elev().scope;
~sites[0].dist().scope;

~sites[0].azim().setAt(0,1);

// continous observation of the relative spherical positions
~rout.stop;
~rout = Routine{
	{
		postln(~sites[0].azim().get;);
		postln(~sites[0].elev().get;);
		postln(~sites[0].dist().get;);
		0.3.sleep;
}.loop;}.play;

