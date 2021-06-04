

SprawlSiteSpherical {

	var n_sources = 0;

	var inbus;

	// site position
	var x_pos=0, y_pos=0, z_pos=0;

	// orientation buses for connecting translators and encoders
	var azim, elev, dist;

	var translators;
	var encoders;

	*new { | srv, n |
		^super.new.init(srv, n)
	}

	init { | srv, n |

		fork {

			n_sources = n;
			inbus     = Bus.audio(srv,n);

			azim = Bus.control(srv,n);
			elev = Bus.control(srv,n);
			dist = Bus.control(srv,n);

			srv.sync;

			for(0,n-1, {

				translators = translators.add(
					{
						|
						aS=0, eS=0, dS=0, // sender
						//
						aR=0, eR=0, dR=0, // receiver
						//
						pitch=0, roll=0, yaw=0,       // receiver orientation
						//
						a=0, e=0, d=0 // bus indices for relative spherical position for rendering
						|

						var azim, elev, dist;

						dist = sqrt( dS*dS + dR*dR - 2*(dS*dR) * (sin(aS)*sin(aR) * cos(eS-eR)+cos(aS)*cos(aR)));

						elev = eR-eS;
						azim = aR-aS;

						elev = (elev*cos(roll)) + (azim*sin(roll));
						azim = (azim*cos(roll)) + (elev*sin(roll));

						Out.kr(a,azim);
						Out.kr(e,elev);
						Out.kr(d,dist);

					}.play;
				)
			};);

			srv.sync;

			for(0,n-1, { arg i;
				translators[i].set(\a,azim.index+i);
				translators[i].set(\e,elev.index+i);
				translators[i].set(\d,dist.index+i);
			});


			srv.sync;

			for(0,n-1, {

				encoders = encoders.add(
					{
						|
						in_bus     = 0,
						out_bus    = 0,
						reverb_bus = 0,
						//
						azim    = 1,
						elev    = 1,
						dist    = 1,
						gain    = 0.5,
						reverb  = 0.1
						|

						var sound = gain * In.ar(in_bus);

						var level =  (0.75/(max(0,dist)+1.0))*(0.75/(max(0,dist)+1.0));

						var bform = HOASphericalHarmonics.coefN3D(3, azim, elev) * sound * level;

						Out.ar(out_bus, bform);
						Out.ar(reverb_bus, reverb*sound);
					}.play;

				)
			};);

		};
	}


	////////////////////////////////////////////////////////////////////////
	// Getters and setters
	////////////////////////////////////////////////////////////////////////

	n_sources {^n_sources}

	x_pos {^x_pos}
	x_pos_ { | inVal | x_pos = inVal;}

	y_pos {^y_pos}
	y_pos_ { | inVal | y_pos = inVal;}

	z_pos {^z_pos}
	z_pos_ { | inVal | z_pos = inVal;}

	// only getters for internal control busess
	azim {^azim}
	elev {^elev}
	dist {^dist}

	translators {^translators}

	encoders {^encoders}

	inbus {^inbus}



}



