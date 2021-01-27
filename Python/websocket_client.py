#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 25 13:06:51 2021

@author: anwaldt
"""

# client.py

import socket
from pythonosc import udp_client, dispatcher
from pythonosc import osc_server

class Client:
    
    def __init__(self):
        
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(("85.214.78.6", 5000))
    
    
        self.receive_routine()
        
    def foo_handler(unused_addr, idx, value):          
             print(value)
             
       
    def receive_routine(self):    
        
        while 1:
            
            
            data = self.socket.recv(4096)  
            if not data: break
            result_string = data.decode("utf8")  
        
            print(format(result_string))


if __name__ == "__main__":

    c = Client()


