~default_routing = {


	// set all spatial sends 0
	~rendering_gain_BUS.do({arg b, i; b.value = 0;});

	// per default each access points is routed to two sources
	for(0, ~nSystems -1,
		{ arg sysIDX;
			for (0, ~nChannels -1,
				{arg chanIDX;

					~rendering_gain_BUS[sysIDX].setAt((2*sysIDX) + chanIDX, 1);

				}
			);
		}
	);
};