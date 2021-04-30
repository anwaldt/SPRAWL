import math
import threading
import tkinter as tk
from queue import Queue

from pythonosc import dispatcher
from pythonosc import osc_server

LISTEN_IP = '127.0.0.1'
LISTEN_PORT = 5003

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

    for i, [x, y] in enumerate(self.players):
      canvas.create_circle(x, y)
      canvas.create_text(x, y, text=i)

    self.after(10, self.draw_players, canvas)


# listen for OSC signals
def osc_listen(ip, port):
  dispatcher.map("/source/aed", update_pos)
  server = osc_server.ThreadingOSCUDPServer((ip, port), dispatcher)
  print("listening on {}".format(server.server_address))
  server.serve_forever()

# callback, run when new OSC position signal is received
def update_pos(_addr, index, azim, _elev, dist):
  #print('DIST + AZIM ', dist, azim)
  x = float(dist) * math.cos(float(azim))
  y = float(dist) * math.sin(float(azim))
  #print(x,y)
  changes.put([int(index), x, y])
  


if __name__ == "__main__":
  # listen for new signals in seperate thread
  listener = threading.Thread(target=osc_listen, args=(LISTEN_IP, LISTEN_PORT))
  listener.start()

  # run gui in main thread
  window = tk.Tk()
  canvas = Canvas()
  
  size_str = '{}x{}+300+300'.format(SCREEN_SIZE, SCREEN_SIZE)
  window.geometry(size_str)
  window.mainloop()
