# Makefile for Socket Programming

# File Name: makefile
# Author: Hooman Hejazi
# Created on November 10, 2015, 9:42 AM

# Compiler javac 1.8.0_31 
# Operating System Mac OS X 10.11

# GNU Make 3.81
# This program built for i386-apple-darwin11.3.0


# Contract: 
# Note that Sender.java and Receiver.java and packet.java must be present in the same directory as the makefile 

# Instructions: 
# In termianl use the command <make> followed by file name 'makefile' with the ‘-f’ or ‘--file’ option, as follows:
# make -f makefile  

JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Sender.java \
	Receiver.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
