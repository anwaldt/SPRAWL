#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 25 13:06:51 2021

@author: anwaldt
"""

# client.py


import argparse
import socket
import threading
import time

from pythonosc import osc_server, dispatcher
from pythonosc import udp_client
from typing import List, Any


class Client:
    
    def __init__(self, server, server_port, client_port, listen_port):
        
 
        self.osc_client  = udp_client.SimpleUDPClient("127.0.0.1", client_port)             
        
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((server, server_port))
        
    
        self.threads = list()    
    
        self.dispatcher  = dispatcher.Dispatcher()  
        self.dispatcher.set_default_handler(self.default_handler)
        self.server = osc_server.ThreadingOSCUDPServer(( "0.0.0.0", listen_port), self.dispatcher)   
        
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
        
        data = self.socket.recv(1024)  
        if data:
            print("Connected to server!")
        
        while 1:


            self.socket.settimeout(10e5)
                        
            data = self.socket.recv(1024)  
            if not data: break
            
            string = data.decode("utf8")
            
            path = ''
            args = []
            messages = []
            for s in string.split(' '):
                if path == '':
                    path = s
                elif len(s.split('/')) > 1:
                    # more than on message
                    #print('splitting: ', s.split('/')[0])
                    
                    args.append(s.split('/')[0])
                    
                    #print(path, '---', args)
                    messages.append([path, args])
                    
                    path = s[len(s.split('/')[0]):]
                    #print(path) 
                    args = []
                else:
                    args.append(s)
                    

            messages.append([path, args])

            #path = string.split(" ")[0]
            #args = string.split(" ")[1:len(string)-1]
            #print('messages: ', messages)            
            for m in messages:
                self.osc_client.send_message(m[0], m[1])
                #time.sleep(0.01)
                #print('message: ', m)
            #self.osc_client.send_message(path, args)

            print(string)


if __name__ == "__main__":
    
    parser = argparse.ArgumentParser()
    
    parser.add_argument("-s", "--server-address", dest = "server_address", default = 'localhost', help="Server address for the remote TCP connection.")
    parser.add_argument("-p", "--server-port",    dest = "server_port",    default = 5000,        help="Server port  for the remote TCP connection.", type=int)
    parser.add_argument("-c", "--client-port",    dest = "client_port",    default = 5003,        help="Port for sending local OSC messages.", type=int)
    parser.add_argument("-l", "--listen-port",    dest = "listen_port",    default = 9495,        help="Port for receiving local OSC messages.", type=int)

    args = parser.parse_args()
        
    c = Client(args.server_address, args.server_port, args.client_port, args.listen_port)
    
    


