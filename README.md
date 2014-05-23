Distributed Retired Traveling Salesman
======================================

I will try to make it as clear as possible how to use this code. If you have questions, contact me (Daniel Seita) at takeshidanny@gmail.com

(1) Clone the respository and go into the "Source" directory.

(2) Type "make" in the command line, which will invoke our Makefile. This should run without any problems. (If it does, you are probably missing a file somewhere.)

(3) We must also start the slave servers. Before starting any servers do this, edit the "slave.conf" file to add or remove as many servers as you wish. For obvious reasons, you must have the address of at least one slave server in the configuration file. Since the master runs on port 8000, it is logical to start the slave servers at 8001 and up. For instance, if the configuration file consists entirely of the following three lines:

http://localhost:8001
http://localhost:8002
http://localhost:8003

Then this will cause the master to start three new threads for price checking, all on the current machine. We recommend sticking to one machine to start out.

(5) Now, wait for all the Firefox windows to start opening. Do NOT start the client code until all slave servers have Firefox open.

(3) Next, we must start the servers. To start the master server, type "make run_master_print" (we have a version that does not print, but we recommend you always use the one that prints extra information). The master server runs on port 8000. You are free to change it, but we recommend sticking with the default.

(6) Once the slave servers all have Firefox open, then type in "python Client.py" on the command line, and you will receive instructions on how to use the code. Have fun!

Notes: the python code is only going to output the best flight route (assuming the request is valid). The slave servers print out messages as they retrieve flight prices. The master server will print a lot of information that may be of interest to an astute reader, such as the number of nodes expanded in the tree, the time, etc.
