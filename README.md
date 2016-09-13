# GoBackN-Protocol
Reliable Data Transfer: The Go Back N Protocol 

 
 *
 * File:   README
 * Author: Hooman Hejazi
 * Go-Back-N Protocol
 * Created on November 10, 2015, 9:42 AM
 *
 */


  == README ==



  How To Run The Program?
  -----------------------
 

  Note that    

  1.   The network emulator must be running before the receiver program.

  2.   The Receiver program must be running before the Sender program.



  How To Compile The Program?
  ---------------------------

  The programs can be compiled and linked automatically using the supplied makefile. To do so, Sender.java, 
  Receiver.java, and packet.java must be present in the same directory as the makefile.
  
  1.  In termianl navigate to the directory where 'makefile' is located. Then simply use the command <make>. 
      For example, the program can be linked and compiled by typing the following command: 
      make

  2. Alternatively, in termianl use the command <make> followed by the file name 'makefile' with the ‘-f’ or ‘--file’ option, as follows:
     make -f makefile  


  Make Version:     GNU Make 3.81
  Compiler Version:   Compiler javac 1.8.0_31 


  How Was The Program Tested?
  ---------------------------

  Test 1: Using Two Different Machine on student.cs.uwaterloo.ca Environment 

      Machine: ubuntu1204-004
      OS: Linux ubuntu1204-004 3.13.0-57-generic #95~precise1-Ubuntu SMP Mon Jun 22 09:43:07 UTC 2015 x86_64 x86_64 x86_64 GNU/Linux
      IP Address: 129.97.167.42


    Methodology:

    - The Network Emulator was launched as follow:
      ./nEmulator-linux386 57110 localhost 57111 57112 localhost 57113 200 0.2 0
 
    - The Receiver program was launched as follow:
      java Receiver localhost 57112 57111 output

    - The Sender program printed was launched as follow:
      java Sender localhost 57110 57113 test

    - The content of test file can be fund at the end of this document. 
    - The output of the receiver program matched the content of the test file.
    - All generated logs were examined.
    - The test was passed successfully. 

    - Steps 1 to 3 were repeated 10 times with different parameters and test files. 
    - All tests were passed succcessfully.
    
    Conclusion: 
    
    Running the programs on two different machines for testing was successful.


  Contents of the test file
  -------------------------
0th line of the file is here.
1th line of the file is here.
2th line of the file is here.
3th line of the file is here.
4th line of the file is here.
5th line of the file is here.
6th line of the file is here.
7th line of the file is here.
8th line of the file is here.
9th line of the file is here.
10th line of the file is here.
...
999th line of the file is here.

