# SystemD User Services

SPRAWL offers following SystemD User Services:

* jack
* jack-dummy
* jacktrip
* aj-snapshot
* jack-matchmaker
* sc-sprawl
* multi-site-server

The SC Sprawl and Multi Site Server services rely on the SPRAWL repo to be found
under ~/SPRAWL/.

## Installation

The `install.sh` script in the systemd directory installs all service files to
`/usr/lib/systemd/user/`. So all service files should be user services.

## Configuration

All SPRAWL related configuration is located in `~/.sprawl/`.
`jack.conf` respectively `jack-dummy.conf` are the corresponding jack configurations.
There the sampling rate and period size are defined.

Snapshots for aj-snapshots can be saved under `~/.sprawl/snapshots`.
The aj-snapshot service uses `~/.sprawl/aj-snapshot.xml`.
Usually this file is just a symbolic link to a file found in the snapshots directory.

