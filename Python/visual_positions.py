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

SCALE = 50
DEFAULT_SIZE = 20

# OSC dispatcher must be global
dispatcher = dispatcher.Dispatcher()

# global queue to communicate positions changes between threads
changes = Queue()

# add custom function to easily draw circles
def _create_circle(self, x, y, r=20, **kwargs):
  return self.create_oval(x - r, y - r, x + r, y + r, **kwargs)

tk.Canvas.create_circle = _create_circle

class Canvas(tk.Frame):
  players = [[0 + OFFSET, 0 + OFFSET, 0] for _ in range(16)]

  def __init__(self):
    super().__init__()

    self.pack(fill=tk.BOTH, expand=1)
    canvas = tk.Canvas(self)
    canvas.pack(fill=tk.BOTH, expand=1)
    self.run(canvas)

  def check_range(self, x, min_value, max_value):
    if x <= min_value:
      return min_value
    if x >= max_value:
      return max_value
    
    return x

  def draw_grid(self, canvas):
    w = canvas.winfo_width()
    h = canvas.winfo_height()

    for i in range(0, w, SCALE):
      canvas.create_line([(i, 0), (i, h)], tag='grid_line')

    for i in range(0, h, SCALE):
      canvas.create_line([(0, i), (w, i)], tag='grid_line')


  def draw_players(self, canvas):
    while not changes.empty():
      [index, x, y, elev] = changes.get()
      self.players[index] = [x + canvas.winfo_width() // 2, 
                             y + canvas.winfo_height() // 2,
                             self.check_range(DEFAULT_SIZE + elev, 10, 30)]

    for i, [x, y, r] in enumerate(self.players):
      canvas.create_circle(x, y, r,fill='pink', tag='player')
      canvas.create_text(x, y, text=i, tag='player')

    
  def run(self, canvas):
    # TODO: only redraw grid after window resize
    #       otherwise only delete 'players' tag
    canvas.delete('all')
    
    self.draw_grid(canvas)
    self.draw_players(canvas)
    self.after(10, self.run, canvas)


def osc_listen(ip, port):
  dispatcher.map("/source/aed", update_pos)
  server = osc_server.ThreadingOSCUDPServer((ip, port), dispatcher)
  print("listening on {}".format(server.server_address))
  server.serve_forever()

# callback, run when new OSC position signal is received
def update_pos(_addr, index, azim, elev, dist):
  dist = float(dist) * SCALE

  x = float(dist) * math.cos(float(azim))
  y = float(dist) * math.sin(float(azim))
  changes.put([int(index), x, y, float(elev)])
  

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
