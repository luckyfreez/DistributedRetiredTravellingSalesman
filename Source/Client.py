'''
Client for the flight scheduler
(c) 2014 by Daniel Seita and Lucky Zhang
'''

import xmlrpclib, sys

# Helper method, checks for the number of arguments and if dates look reasonable. Does nothing else, though.
def check_input(s):
    user_request_split = user_request.split(" ")
    num_args = len(user_request_split)
    if num_args < 5:
        print "Not enough strings in the input -- use exactly two dates and at least three cities."
        return False
    elif (len(user_request_split[0].split("/")) != 3 or len(user_request_split[1].split("/")) != 3):
        print "Dates need to be in MM/DD/YYYY format."
        return False
    return True

# Assume for now that the server is at localhost
server = xmlrpclib.Server("http://localhost:8000")

# Introductory message
print "\n*************************** DANIEL AND LUCKY'S FLIGHT SCHEDULER ***************************\n"
print "Instructions: find a series of flights by listing the start date, end date, and cities."
print ""
print "Specific formatting requirements:\n\t(1) Start and end dates must be the first and second arguments, respectively\
        \n\t(2) Separate all arguments by at least one whitespace\n\t(3) Use MM/DD/YYYY format for dates (including leading zeros!)\
        \n\t(4) Spell city names (or 3-letter abbreviation) correctly\n\t(5) Use at least three cities"
print ""
print "Example request: \"06/11/2014 06/16/2014 ORD BOS SEA\""
print "This searches flights from June 11 to 16 (in 2014) that touch Chicago, Boston, and Seattle."
print "\nOptional extensions to add at the end of the request:\
        \n\t(1) Put a number to indicate the minimum days between flights"
print "\nTo exit, type in \"q\"."
print "\n*******************************************************************************************\n"

# The while loop that drives everything
while True:
    user_request = raw_input(">>> ")
    if user_request == "q":
        sys.exit()
    elif check_input(user_request):
        print "Input is in the correct format. Now solving ...\n"
        print server.masterServer.startProblem(user_request)
        print ""
    else:
        print "Input is not in the right format. Please try again."
