#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 25 13:06:51 2021

@author: anwaldt
"""

# client.py

import socket
import threading

from pythonosc import osc_server, dispatcher
from pythonosc import udp_client
from typing import List, Any


class Client:
    
    def __init__(self):
        
        self.osc_client  = udp_client.SimpleUDPClient("127.0.0.1", 9495)             
        
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(("85.214.78.6", 5000))
    
        self.threads = list()    
    
        self.dispatcher  = dispatcher.Dispatcher()  
        self.dispatcher.set_default_handler(self.default_handler)
        self.server = osc_server.ThreadingOSCUDPServer(( "0.0.0.0", 9494), self.dispatcher)   
        
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.deamon = True
        self.server_thread.start() 
        self.threads.append(self.server_thread)
    
        self.socket_thread = threading.Thread(target=self.receive_routine)
        self.socket_thread.deamon = True
        self.socket_thread.start()   
        self.threads.append(self.socket_thread)

        
        
        
    def default_handler(self, address: str, *osc_arguments: List[Any]) -> None:
    
        l = len(osc_arguments)
         
         
 
        data = address

        for i in range(l):
            thisarg = osc_arguments[i]
            res = isinstance(thisarg, str)
            if res == False:
                thisarg = str(thisarg)
            data=data+" "+thisarg
        
        self.socket.send(data.encode())
 

    
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


