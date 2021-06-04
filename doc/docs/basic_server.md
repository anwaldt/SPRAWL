## The Basic Server

The basic server works with a single listening position
in the center for all access points. All sound source positions are
thus defined by their absolute position:

![Single Position](graphics/single_position.png)
<!-- {width=100% height=400} -->

----

The basic SC server offers binaural rendering with OSC control.
In the recent version, all access points send and receive two
channels of audio to the server. Signals from the access
points can be routed to specific targets via OSC command.
All access points receive the full binaural mix and selected
sources. The server is per default configured for up to 16 access points,
each with two channels:

Access Point | Sources
---     | ---   
0       | 0,1
1       | 2,3
2       | 4,5
...     | ...   
15      | 30,31

### SC Output Channel Assignment

Channels    | Description
---         | ---   
00-31       | direct to access points
32-63       | binaural to access point
64-65       | main binaural
64-81       | main 3rd order Ambisonics


### OSC Messages

Sending OSC messages can be tested with the PD patches
`control_position.pd` and `control_sends.pd`.
The default server listens to the following messages:


#### Routing

For each access point, the server adds two virtual sound sources.
Individual channels from each access point can be routed to any virtual sound source:

    /route/spatial i i i f

- first argument: send access point index (0...15)
- second argument: send channel to use (0,1)
- third argument: virtual sound source index (0...31)
- fourth argument: gain (0...1)

The default routing can be restored at any time with the OSC message:

    /default_spatial_routing

All spatial routing gains can be set to 0 with the following OSC message:

    /mute_all_spatial


Individual channels from each access point can be routed
to individual channels of other access points:

    /route/pi i i i f

- first argument: send access point index (0...15)
- second argument: send channel to use (0,1)
- third argument: receive access point index (0...15)
- fourth argument: receive channel to use (0,1)
- fifth argument: gain (0...1)

All AP routing gains can be set to 0 with the following OSC message:

    /mute_all_ap



#### Spatial

Source positions are controlled in spherical coordinates.

    /source/azim i f

- first argument: virtual sound source index (0...32)
- second argument: azimuth angle (-pi ... pi)

    /source/elev i f

- first argument: virtual sound source index (0...32)
- second argument: elevation angle (-pi ... pi)

    /source/dist i f

- first argument: virtual sound source index (0...32)
- second argument: distance in meters (0 ... 10)

Sources can be routed to a convolution reverb, which is
independent from the spatial source position:

    /source/reverb i f

- first argument: virtual sound source index (0...32)
- second argument: reverb gain (0 ... 1)

#### Monitoring

For each access point, the gain of the binaural mix can be set:

    /monitor/gain f

- argument: the monitor gain (0...1)

The binaural mix can also be summed to a single channel.
In that case, only the first (left) channel of the acces
point is used for monitoring:

    /monitor/mono f

- argument: (0....1) 0=stereo, 1=mono
