import math
import threading
import argparse
import time
import tkinter as tk
from queue import Queue

from pythonosc import dispatcher
from pythonosc import osc_server, udp_client

LISTEN_IP = '127.0.0.1'
LISTEN_PORT = 5003

SCREEN_SIZE = 400
OFFSET = SCREEN_SIZE // 2

SCALE = 50
DEFAULT_SIZE = 20

CTRL = 0

# OSC dispatcher must be global
dispatcher = dispatcher.Dispatcher()

# global queues to communicate positions changes between threads
changes = Queue()
send = Queue()

# add custom function to easily draw circles
def _create_circle(self, x, y, r=20, **kwargs):
  return self.create_oval(x - r, y - r, x + r, y + r, **kwargs)

tk.Canvas.create_circle = _create_circle

class Canvas(tk.Frame):
  players = [[0 + OFFSET, 0 + OFFSET, 0] for _ in range(16)]

  def __init__(self):
    super().__init__()

    self.pack(fill=tk.BOTH, expand=1)
    self.canvas = tk.Canvas(self)
    self.canvas.pack(fill=tk.BOTH, expand=1)

    self._drag_data = {"x": 0, "y": 0, "item": None}

    # bindings for clicking, dragging and releasing
    self.canvas.tag_bind("token", "<ButtonPress-1>", self.drag_start)
    self.canvas.tag_bind("token", "<ButtonRelease-1>", self.drag_stop)
    self.canvas.tag_bind("token", "<B1-Motion>", self.drag)

    # create controllable source
    if CTRL >= 0:
      self.canvas.create_circle(OFFSET, OFFSET, 20, fill="sky blue", tag="token")

    self.run(self.canvas)

  def check_range(self, x, min_value, max_value):
    if x <= min_value:
      return min_value
    if x >= max_value:
      return max_value
    
    return x

  def draw_grid(self, canvas):
    w = canvas.winfo_width()
    h = canvas.winfo_height()
    grid_color = 'snow3'

    for i in range(0, w, SCALE):
      canvas.create_line([(i, 0), (i, h)], tag='grid_line', fill=grid_color)

    for i in range(0, h, SCALE):
      canvas.create_line([(0, i), (w, i)], tag='grid_line', fill=grid_color)


  def draw_players(self, canvas):
    while not changes.empty():
      [index, x, y, elev] = changes.get()
      self.players[index] = [x + canvas.winfo_width() // 2, 
                             y + canvas.winfo_height() // 2,
                             self.check_range(DEFAULT_SIZE + elev, 10, 30)]

    for i, [x, y, r] in enumerate(self.players):
      if i == CTRL:
        pass
        canvas.create_text(x, y, text=i, tag='ctrl_index')
        # canvas.create_circle(x, y, r, fill="sky blue", tag="token")
      else:
        canvas.create_circle(x, y, r,fill='pink', tag='player')
        canvas.create_text(x, y, text=i, tag='player_index')

    
  def run(self, canvas):
    # TODO: only redraw grid after window resize
    #       otherwise only delete 'players' tag
    canvas.delete('player')
    canvas.delete('player_index')
    canvas.delete('ctrl_index')
    canvas.delete('grid')
    
    self.draw_grid(canvas)
    self.draw_players(canvas)

    canvas.tag_raise('token')
    canvas.tag_raise('ctrl_index')

    self.after(10, self.run, canvas)

  def drag_start(self, event):
    """Beginning drag of an object"""
    # record the item and its location
    self._drag_data["item"] = self.canvas.find_closest(event.x, event.y)[0]
    self._drag_data["x"] = event.x
    self._drag_data["y"] = event.y

  def drag_stop(self, event):
      """End drag of an object"""
      # reset the drag information
      self._drag_data["item"] = None
      self._drag_data["x"] = 0
      self._drag_data["y"] = 0

  def drag(self, event):
      """Handle dragging of an object"""
      # compute how much the mouse has moved
      delta_x = event.x - self._drag_data["x"]
      delta_y = event.y - self._drag_data["y"]
      # move the object the appropriate amount
      self.canvas.move(self._drag_data["item"], delta_x, delta_y)
      # record the new position
      self._drag_data["x"] = event.x
      self._drag_data["y"] = event.y
      # write new position to send queue
      send.put([(event.x - self.canvas.winfo_width() // 2) / SCALE, 
                (event.y - self.canvas.winfo_height() // 2) / SCALE])


def osc_listen(ip, port):
  dispatcher.map("/source/aed", update_pos)
  server = osc_server.ThreadingOSCUDPServer((ip, port), dispatcher)
  print("listening on {}".format(server.server_address))
  server.serve_forever()


def osc_send(ip, port):
  client = udp_client.SimpleUDPClient(ip, port)
  while True:
    [x, y] = send.get()
    dist = math.sqrt(x*x + y*y)
    azim = math.atan2(y, x)
    client.send_message("/source/azim", [CTRL, azim])
    client.send_message("/source/dist", [CTRL, dist])

# callback, run when new OSC position signal is received
def update_pos(_addr, index, azim, elev, dist):
  dist = float(dist) * SCALE

  x = float(dist) * math.cos(float(azim))
  y = float(dist) * math.sin(float(azim))
  changes.put([int(index), x, y, float(elev)])
  

if __name__ == "__main__":
  parser = argparse.ArgumentParser()        
    
  parser.add_argument("-c", "--controlled-source", dest="controlled_source", default="-1", help="Index of the source you want to control")
  parser.add_argument("-i", "--listen-ip", dest="listen_ip", default='127.0.0.1', help="IP of the server sending the position data")
  parser.add_argument("-p", "--listen-port", dest="listen_port", default=5003, help="Port of the server sending the position data")
  
  parser.add_argument("-r", "--recv-ip", dest="recv_ip", default='127.0.0.1')
  parser.add_argument("-t", "--recv-port", dest="recv_port", default=57121)
  
  parser.add_argument("-s", "--scale", dest="scale", default=50, help="set the zoom level")

  args = parser.parse_args()

  CTRL = int(args.controlled_source)
  SCALE = int(args.scale)

  # listen for new signals in seperate thread
  listener = threading.Thread(target=osc_listen, args=(args.listen_ip, args.listen_port))
  listener.start()

  # send position of the source we control
  sender = threading.Thread(target=osc_send, args=(args.recv_ip, int(args.recv_port)))
  sender.start()

  # run gui in main thread
  window = tk.Tk()
  canvas = Canvas()
  
  size_str = '{}x{}+300+300'.format(SCREEN_SIZE, SCREEN_SIZE)
  window.geometry(size_str)
  window.mainloop()
