SprawlOutput {

	var node, nChan, gain;

	*new { | srv, grp, nChan, in_bus, out_bus |
		^super.new.init(srv, grp, nChan, in_bus, out_bus )
	}

	init { | srv, grp, nChan, in_bus, out_bus |

		Routine({
		node = {Out.ar(out_bus,In.ar(in_bus,nChan))}.play();

		srv.sync;

		node.moveToTail(grp);
		}).play;
	}


}
