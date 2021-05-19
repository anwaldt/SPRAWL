# ideas for this project: 
#   * visualize the position of every player (in 2 Dimensions)
#   * update this in real time based on OSC data
#   * allow players to change positions by dragging the points

# x = r * sin(incl) * cos(azim)
# y = r * sin(incl) * sin(azim)

import tkinter as tk
import random

def _create_circle(self, x, y, r=20, **kwargs):
    return self.create_oval(x - r, y - r, x + r, y + r, **kwargs)
tk.Canvas.create_circle = _create_circle

class Canvas(tk.Frame):
    def __init__(self):
        super().__init__()
        players = [(random.randint(0, 300), random.randint(0, 300)) for _ in range(10)]
        self.draw_players(players)

    def draw_players(self, players):
        self.master.title('my canvas')
        self.pack(fill=tk.BOTH, expand=1)

        canvas = tk.Canvas(self)
        for (x, y) in players:
            canvas.create_circle(x, y)

        canvas.pack(fill=tk.BOTH, expand=1)




def click(event):
    print('clicked at', event.x, event.y)

def main():
    window = tk.Tk()
    canvas = Canvas()

    # capture all click events
    window.bind('<Button-1>', click)

    window.geometry("400x250+300+300")
    window.mainloop()

if __name__ == '__main__':
    main()