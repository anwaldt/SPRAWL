~mute_all_ap = {~gain_BUS_pi.do({arg b, i; b.value = 0;})};


~mute_all_spatial = {~rendering_gain_BUS.do({arg b, i; b.value = 0;})};


~default_spatial_routing = {


	// set all spatial sends 0
	~rendering_gain_BUS.do({arg b, i; b.value = 0;});

	// per default each access points is routed to two sources
	for(0, ~nSystems -1,
		{ arg sysIDX;
			for (0, ~nChannels -1,
				{arg chanIDX;

					~rendering_gain_BUS[sysIDX].setAt((2*~nChannels*sysIDX) + ((1+~nChannels)*chanIDX), 1);

				}
			);
		}
	);
};