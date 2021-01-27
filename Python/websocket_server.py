#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 25 15:03:02 2021

@author: anwaldt
"""

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Jan 24 23:56:53 2021

@author: anwaldt
"""

import sys
import signal
import socket

from pythonosc import osc_server
from pythonosc import udp_client, dispatcher
import threading
import time
from typing import List, Any

class Connection:

    def __init__(self, conn, addr):

        self.connection = conn
        self.address = addr
        
class tcp_back:
    
    def __init__(self):
                
        self.HOST = ''                 # Symbolic name meaning the local host
        self.PORT = 5000               # The port used by the server
        self.serv_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serv_sock.bind((self.HOST, self.PORT))
        self.serv_sock.listen(1)

        self.osc_clients = list()
        self.clients = list()

        self.threads = list()

        self.dispatcher  = dispatcher.Dispatcher()       
        self.dispatcher.map("/foo",        self.foo_handler)
        self.dispatcher.set_default_handler(self.default_handler)
    
        self.server = osc_server.ThreadingOSCUDPServer(( "0.0.0.0", 9494), self.dispatcher)       
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.deamon = True
        self.server_thread.start() 
        self.threads.append(self.server_thread)

        self.socket_thread = threading.Thread(target=self.socket_loop)
        self.socket_thread.deamon = True
        self.socket_thread.start()   
        self.threads.append(self.socket_thread)

        #self.send_thread = threading.Thread(target=self.send_loop)
        #self.send_thread.deamon = True
        #self.threads.append(self.send_thread)
        #self.send_thread.start()               
        
        # join the threads so they are stopped on exit
 #       for t in self.threads:  
 #         t.join()
            
    def default_handler(self, address: str, *osc_arguments: List[Any]) -> None:

         l = len(osc_arguments)
         
         for c in self.clients:
 
                data = address

                for i in range(l):
                    thisarg = osc_arguments[i]
                    res = isinstance(thisarg, str)
                    if res == False:
                        thisarg = str(thisarg)
                    data=data+" "+thisarg
                
                c.connection.send(data.encode())
 
                
    def foo_handler(self, unused_addr, idx, value):  
        
         for c in self.clients:
 
                data = "/foo/back"+" "+str(cnt)
                
                #c.connection.send(data.encode())
                cnt+=1
                
             
    
    
    
    def send_loop(self):
    
        while 1:

            # print(len(self.osc_clients)," clients connected!")

            cnt = 0

            for c in self.clients:
 
                data = "/foo/back"+" "+str(cnt)
                
                c.connection.send(data.encode())
                cnt+=1
                
            time.sleep(0.1)


            
    
    def socket_loop(self):
 
        
        while 1:

            print("Ready for new connection")
            
            conn, addr = self.serv_sock.accept()

            print(addr[0])
            
            self.clients.append(Connection(conn, addr))
            
            print ("Client %s connected" %str(addr))

 
            
            
    def signal_handler(self,signal, frame):
        print("exiting")
        sys.exit(0)
                
#    def keyboardInterruptHandler(signal, frame):
#        print("KeyboardInterrupt (ID: {}) has been caught. Cleaning up...".format(signal))
#
#        for t in self.threads:
#            print("killing")
#            t.stop()
#
#        exit(0)
        
if __name__ == "__main__":

    
    p = tcp_back()
    signal.signal(signal.SIGINT, p.signal_handler)

    #signal.signal(signal.SIGTERM, tcp_back.keyboardInterruptHandler)
    
