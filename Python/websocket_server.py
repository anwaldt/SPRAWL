#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 25 15:03:02 2021

@author: anwaldt
"""

import argparse
import sys
import signal
import socket

from pythonosc import osc_server, dispatcher
from pythonosc import udp_client
import threading
import time
from typing import List, Any




class Connection:

    def __init__(self, conn, addr, cl):

        self.connection = conn
        self.address    = addr
        self.clients    = cl
        
        self.is_connected = True
        
        self.echo_thread = threading.Thread(target=self.echo)
        self.echo_thread.deamon = True
        self.echo_thread.start()    
        
    def echo(self):
    
        while self.is_connected == True:
    
            self.connection.settimeout(10e5)
            
            data = self.connection.recv(1024)
            
            if not data:                
                print('Disconnected!')
                self.is_connected = False
                
                
            else:
                for c in self.clients:            
                    c.connection.sendall(data)
            
        
class TcpOscEcho():
    
    def __init__(self, tcp_port, osc_port):
                
        self.HOST = ''                 # Symbolic name meaning the local host
        self.PORT = tcp_port               # The port used by the server
        self.serv_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serv_sock.bind((self.HOST, self.PORT))
        self.serv_sock.listen(1)
        
        
        self.serv_sock.settimeout(10e5)
        o = self.serv_sock.getsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE)
        
        self.osc_clients    = list()
        self.clients        = list()
        self.threads        = list()

        self.dispatcher  = dispatcher.Dispatcher()       
        self.dispatcher.set_default_handler(self.default_handler)
    
        self.server = osc_server.ThreadingOSCUDPServer(( "0.0.0.0", osc_port), self.dispatcher) 
        
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.deamon = True
        self.server_thread.start() 
        self.threads.append(self.server_thread)

        self.socket_thread = threading.Thread(target=self.connect_sockets)
        self.socket_thread.deamon = True
        self.socket_thread.start()   
        self.threads.append(self.socket_thread)

        self.discon_thread = threading.Thread(target=self.disconnect_sockets)
        self.discon_thread.deamon = True
        self.discon_thread.start()   
        self.threads.append(self.discon_thread)          
        
        # join the threads so they are stopped on exit
        #       for t in self.threads:  
        #         t.join()
            
    def default_handler(self, address: str, *osc_arguments: List[Any]) -> None:

         print("RECEIVED")

         l = len(osc_arguments)
         
         for c in self.clients:
 
                data = address
                
                for i in range(l):
                    thisarg = osc_arguments[i]
                    res = isinstance(thisarg, str)
                    if res == False:
                        thisarg = str(thisarg)
                    data=data+" "+thisarg
                
                print(c.is_connected)

                if(c.is_connected):
                    c.connection.send(data.encode())
                    
    
    
   
    def connect_sockets(self):
        
        while 1:

            
            print("Connected to "+str(len(self.clients))+" clients!")

            print("Ready for new connection")
            
            conn, addr = self.serv_sock.accept()
            
            self.clients.append(Connection(conn, addr, self.clients))
            
            print ("Client %s connected" %str(addr))

 
    def disconnect_sockets(self):
         
        while 1:
            
            l1 = len(self.clients)
            for c in self.clients[:]:
                if c.is_connected == False:                    
                    self.clients.remove(c)
                            
            l2 = len(self.clients)   
            
            # only print info if a client has been removed
            if(l1!=l2):
                print("Connected to "+str(len(self.clients))+" clients!")
            
            time.sleep(1)
            
            
    def signal_handler(self,signal, frame):
        print("exiting")
        sys.exit(0)
                
    def keyboardInterruptHandler(self, signal, frame):
        
        print("KeyboardInterrupt (ID: {}) has been caught. Cleaning up...".format(signal))

        for t in self.threads:
            print("killing")
            t.stop()

        exit(0)
        
if __name__ == "__main__":

    parser = argparse.ArgumentParser()        
    
    parser.add_argument("-o", "--osc-port",    dest = "osc_port", default = 5005, help="Port for receiving local OSC messages.")
    parser.add_argument("-t", "--tcp-port",    dest = "tcp_port", default = 5000, help="Port for the remote TCP connection.")

    args = parser.parse_args()
    
    p = TcpOscEcho(args.tcp_port, args.osc_port)
    
    signal.signal(signal.SIGINT, p.signal_handler)

    signal.signal(signal.SIGTERM, TcpOscEcho.keyboardInterruptHandler)
    
