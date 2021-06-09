/*

Some code snippets for monitoring buses
after starting the SPRAWL server.

*/

// Bus monitoring
// (works only for local servers)

{

	ServerMeter(s);

	s.scope(16,~rendering_gain_BUS[2].index, rate: 'control');

	s.scope(16,~rendering_send_BUS.index);

	s.scope(2,~binaural_mix_BUS.index);

	s.scope(16,~binaural_mono_BUS.index);

	s.scope(16,~gain_BUS_pi[1].index, rate: 'control');

	s.scope(8,128);

	s.scope(8,~control_azim_BUS.index, rate: 'control');

}
