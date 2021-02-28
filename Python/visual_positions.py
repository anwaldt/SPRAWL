"""Small example OSC server

This program listens to several addresses, and prints some information about
received packets.
"""
import argparse
import math
import threading
import tkinter as tk
import random
from queue import Queue

from pythonosc import dispatcher
from pythonosc import osc_server

LISTEN_IP = '127.0.0.1'
LISTEN_PORT = 9595

SCREEN_SIZE = 400
OFFSET = SCREEN_SIZE // 2

# OSC dispatcher must be global
dispatcher = dispatcher.Dispatcher()

# global queue to communicate positions changes between threads
changes = Queue()

# add custom function to easily draw circles
def _create_circle(self, x, y, r=20, **kwargs):
  return self.create_oval(x - r, y - r, x + r, y + r, **kwargs)
tk.Canvas.create_circle = _create_circle

class Canvas(tk.Frame):
  players = [[0 + OFFSET, 0 + OFFSET] for _ in range(16)]

  def __init__(self):
    super().__init__()

    self.pack(fill=tk.BOTH, expand=1)
    canvas = tk.Canvas(self)
    canvas.pack(fill=tk.BOTH, expand=1)
    self.draw_players(canvas)

  def draw_players(self, canvas):
    canvas.delete('all')
    
    while not changes.empty():
      [index, x, y] = changes.get()
      self.players[index] = [x + OFFSET, y + OFFSET]

    for [x, y] in self.players:
      canvas.create_circle(x, y)

    self.after(10, self.draw_players, canvas)


# listen for OSC signals
def osc_listen(ip, port, changes):
  dispatcher.map("/source/aed", update_pos)
  server = osc_server.ThreadingOSCUDPServer((ip, port), dispatcher)
  print("Serving on {}".format(server.server_address))
  server.serve_forever()

# callback, run when new OSC position signal is received
def update_pos(addr, index, azim, elev, dist):
  x = dist * math.cos(azim)
  y = dist * math.sin(azim)

  # print(x, y)
  changes.put([index, x, y])


if __name__ == "__main__":
  # listen for new signals in seperate thread
  listener = threading.Thread(target=osc_listen, args=(LISTEN_IP, LISTEN_PORT, changes))
  listener.start()

  # run gui in main thread
  window = tk.Tk()
  canvas = Canvas()
  
  size_str = '{}x{}+300+300'.format(SCREEN_SIZE, SCREEN_SIZE)
  window.geometry(size_str)
  window.mainloop()
