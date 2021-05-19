## running the GUI

* the OSC server must be sending the OSC position data through `websocket_server.py`

* connect to the server using `websocket_client.py`

* run `python3 visual_positions.py`

* if your websocket client isn't sending on port `5003`, pass the correct port using `python3 visual_positions.py -p <PORT>`
