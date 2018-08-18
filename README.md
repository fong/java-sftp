# RFC 913 - Simple File Transfer Protocol
 **Java**
 
 Eugene Fong (efon103)
 
 [github.com/fong/java-sftp](https://github.com/fong/java-sftp)

#### List of Working Features
 |Command|USER|ACCT|PASS|TYPE|LIST|CDIR|KILL|NAME|DONE|RETR|STOR|
 |:-----:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
 |Client |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |
 |Server |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |
 
 ## Table of Contents
 1. [Introduction](#introduction)
 2. [List of Components](#list-of-components)
    1. [SFTPServer](#sftpserver)
    2. [SFTPClient](#sftpclient)
    3. [Authentication File](#authentication-file)
    4. [FTP Folder](#ftp-folder)
    5. [Restricted Folders](#restricted-folders)
3. [How to setup SFTPServer and run](#how-to-setup-sftpserver-and-run)
4. [How to setup SFTPClient and run](#how-to-setup-sftpclient-and-run)
 
## Introduction

This Java implementation of simple file transfer follows closely to the specification described in [RFC 913](https://tools.ietf.org/html/rfc913). It supports additional features such as restricted access folders with the use of a ```.restrict``` file containing the data of all privileged accounts of that particular folder.

## List of Components

#### SFTPServer
This is the program used to host the SFTP server. It requires two arguments to run, the designated port of the server, and the location of the authentication file (in txt format). The location of this executable will also serve as the location of the server FTP folder ```/ftp```, which will be automatically generated if it does not exist. The SFTPServer is multi-threaded and runs by each *Instance* of a socket connection. This allows multiple client connections.

#### SFTPClient
This is the client used to access the SFTP server. It requires two arguments to run, the IP address of the server to connect and the PORT of the server to connect.  The location of this executable will also serve as the location of the client FTP folder ```/ftp```, which will be automatically generated if it does not exist.

#### Authentication File
The authentication file is a text file which determines which accounts are **USER** only, **USER** and **PASSWORD**, **USER** and **ACCOUNT**, and **USER**, **PASSWORD**, and **ACCOUNT**. This may or may not match the authentication required by restricted folders.

#### FTP Folder ```/ftp```
This is the root of the FTP server. Clients cannot access directories or folders above this level to prevent manipulation of system or private files.

#### Restricted Folders
Folders in the FTP folder can be restricted with the use of a ```.restrict``` file containing all valid accounts and passwords to the folder. **Note 1:** If there is no ```.restrict``` file, the folder will be public. **Note 2** If there is a ```.restrict``` file with no accounts and passwords, the folder will be inaccessible by any client.

## How to setup SFTPServer and run
1. Create you authentication text file. This an be done with any text editor. The format of the text file is ```USER ACCT PASS```. Each parameter is separated by a space. If there is no parameter (i.e. no account or no password), ensure that there is still a space in between. If multiple accounts are required separate each account with a ```|```. Each user is declared on a new line. Save it as a .txt file (e.g. ```auth.txt```).

   Example of an authentication text file:
   ``` 
   //Do not leave comments in actual authentication file. This is just an example.
   //USER only, note the two spaces at the end of the line 
   USER1  

   //USER and ACCOUNT, note the last space
   USER2 ACC1 

   //USER and PASSWORD, note the double space in between
   USER3  PW1

   //USER, ACCOUNT, and PASSWORD
   USER4 ACC2 PW2

   //USER, multiple ACCOUNTS, and PASSWORD.
   //Note that PASSWORD is tied to USER and not ACCOUNT (as account is used for billing purposes only).
   USER5 ACC1|ACC2|ACC3 PW3
   ```

2. Compile SFTPServer by opening the folder ```SFTPServer\src\sftpserver``` in the command prompt. Use the command ```javac *.java``` to compile to class files.
3. Place your authentication file in your ```sftpserver``` folder.
4. From the ```sftpserver``` folder, execute your SFTPServer with the command ```java -cp ../ sftpserver.SFTPServer {PORT} {AUTHENTICATION-FILE}```. For example, ```java -cp ../ sftpserver.SFTPServer 15100 auth.txt```.
5. Your SFTP server is now live. An ```/ftp``` folder will be created if there was no folder on execution.
6. Add files and Folder to the ```ftp``` directory.
7. If you would like to create a folder with restricted access, add a ```.restrict``` file to the directory you want restricted. Similar to the authentication file, this is a text file. However, the format is as follows: ```ACCOUNT PASSWORD```. Multiple accounts are separated by a ```|```. For Example, ```ACC1|ACC2 PASS```. If the client's credentials match the account/s and password, they are given access to the folder.

## How to setup Client and run
1. Compile SFTPClient by opening the folder ```SFTPClient\src\sftpclient``` in the command prompt. Use the command ```javac *.java``` to compile to class files.
2. From the ```sftpclient``` folder, execute your SFTPClient with the command ```java -cp ../ sftpclient.SFTPClient {IP} {PORT}```. For example, ```java -cp ../ sftpserver.SFTPServer localhost 15100```. If the server is not online, or unable to be connected, a ```Connection refused. Server may not be online.``` error will appear.

## Use Cases:

### ***USER***, ***ACCT*** and ***PASS*** Commands

### ***TYPE*** Command
##### *Default Example*
This example illustates a normal use case of the TYPE command. It can switch between ASCII (A), Binary (B), and Continuous (C).
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> TYPE B
+Using Binary mode
> TYPE C
+Using Continuous mode
> TYPE A
+Using Ascii mode
```

##### *Error Cases*
This is the response from the server if the type is not A, B or C. File transfer type is not changed on the client or the server.
```
> TYPE D
-Type not valid
```

### ***LIST*** Command
##### *Default Examples*
This is an example of the basic ```LIST F``` command, it only shows the names of the files and directories.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> LIST F
+/
client-20180816.txt
client.txt
CS725ASSIGNMENT1.pdf
Folder
g4144.jpg
jdk.exe
mainscreen.png
Restricted Folder
test.txt

> 
```

This is an example of the basic ```LIST V``` command, it shows the names and metadata of the files and directories. It also displays the file type (i.e. ASC for ASCII files, BIN for Binary files, DIR for Directories).
```
> LIST V
+
|Name                                               |R/W|Size     |Date                |Owner             |
|---------------------------------------------------|---|---------|--------------------|------------------|
|client-20180816.txt                            ASC |R/W|     4 kB|16/08/18 9:28:05 PM |tofurky\tofutaco  |
|client.txt                                     ASC |R/W|     0 kB|16/08/18 8:49:03 PM |tofurky\tofutaco  |
|CS725ASSIGNMENT1.pdf                           BIN |R/W|   115 kB|31/07/18 4:32:04 PM |tofurky\tofutaco  |
|Folder                                         DIR |R/W|     0 kB|18/08/18 11:40:18 PM|tofurky\tofutaco  |
|g4144.jpg                                      BIN |R/W|     4 kB|2/04/17 1:59:19 AM  |tofurky\tofutaco  |
|jdk.exe                                        BIN |R/W|145071 kB|26/04/18 12:01:42 AM|tofurky\tofutaco  |
|mainscreen.png                                 BIN |R/W|    13 kB|13/04/17 3:24:23 PM |tofurky\tofutaco  |
|Restricted Folder                              DIR |R/W|     4 kB|18/08/18 11:43:24 PM|tofurky\tofutaco  |
|test2.txt                                      ASC |R/W|     0 kB|8/08/18 8:02:07 PM  |tofurky\tofutaco  |
7 File(s)	 2 Dir(s)	 145208kB Total File Size
```

##### *Other Cases*
You can list other folders by adding an additional argument to the directory after the list type. This works for both ```LIST F``` and 
```
> LIST F Folder
+/Folder
goodbyeworld.txt
RFC 913 - Simple File Transfer Protocol.html
```

##### *Error Cases*

***You do not have permission to view the folder***: If the folder has ```.restrict``` file and your account is not inside, the folder is inaccessible.

```
> LIST F Restricted Folder
-Folder is restricted you do not have sufficient permissions to view
```

***Folder does not exist*** or ***Any other folder exception***: As per specification, an "error message from the remote systems directory command" is presented
```
> LIST F FakeFolder
-java.nio.file.NoSuchFileException: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPServer\ftp\FakeFolder
```

### ***CDIR*** Command

##### *Default Examples*
***Unrestricted Folder***: This example illustrates a normal use case of the CDIR command. At the end of these commands, the working directory is changed, as shown by the second ```LIST F``` command.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> LIST F
+
client-20180816.txt
client.txt
CS725ASSIGNMENT1.pdf
Folder
g4144.jpg
jdk.exe
mainscreen.png
Restricted Folder
test2.txt

> CDIR Folder
!Changed working dir to /Folder
> LIST F
+/Folder
goodbyeworld.txt
RFC 913 - Simple File Transfer Protocol.html

> 
```
***Restricted Folder, account not permitted***: If the folder is restricted, the current account and password is revoked and the user will be required to enter a new account and password. Working folder directory is not changed until account and password is correct.
```
> CDIR Restricted Folder
+directory ok, send account/password
> ACCT acc2
+account ok, send password
> PASS pw2
!Changed working dir to /Restricted Folder
> LIST F
+/Restricted Folder
.restrict
auckland.PNG
mypasswords.txt
todelete - Copy.txt
vanilla-tilt.js

> 
```

***Restricted Folder, account is permitted***: If the user's account and password matches the credentials in the folder, the working directory is switched without the need to re-enter account and password.

```
> USER ACCUSR
+User-id valid, send account and password
> ACCT acc2
+Account valid, send password
> PASS pw2
!Account valid, logged-in
> CDIR Restricted Folder
!Changed working dir to /Restricted Folder
> LIST F
+/Restricted Folder
.restrict
auckland.PNG
mypasswords.txt
todelete - Copy.txt
vanilla-tilt.js

> 
```

***Other Cases***
To go back to the root of the FTP server, send CDIR without arguments
```
> CDIR Folder
!Changed working dir to /Folder
> LIST F
+/Folder
goodbyeworld.txt
RFC 913 - Simple File Transfer Protocol.html

> CDIR
!Changed working dir to /
> 
```

To access a folder within a folder, include the folder path from the root of the FTP server.
```
> CDIR Folder/Another Folder
!Changed working dir to /Folder/Another Folder
> 
```

##### *Error Cases*
***Folder does not exist***: An error is presented and the working directory does not change.
```
> CDIR Missing Folder
-Can't connect to directory because: /Missing Folder is not a directory.
> 
```

### ***KILL*** Command

##### *Default Example*
This example illustrates a normal use case of the KILL command. At the end of these commands, the file todelete.txt is deleted from the system.
```
> LIST F
+/Restricted Folder
.restrict
auckland.PNG
mypasswords.txt
todelete.txt
vanilla-tilt.js

> KILL todelete.txt
+todelete.txt deleted
> LIST F
+/Restricted Folder/
.restrict
auckland.PNG
mypasswords.txt
vanilla-tilt.js

> 
```

##### *Error cases*
***File is protected***: If the file is set to read-only, the file cannot be deleted and the following response will occur.
```
+/Restricted Folder/
.restrict
auckland.PNG
mypasswords.txt
vanilla-tilt.js

> KILL auckland.png
-Not deleted because of IO error. It may be protected.
> LIST F
+/Restricted Folder/
.restrict
auckland.PNG
mypasswords.txt
vanilla-tilt.js

> 
```

***File does not exist***: If the file does not exist, the following server response will occur.
```
> KILL nofile.exe
-PATH ERROR: /Restricted Folder/nofile.exe is not a file.
```

***User does not have access privileges to folder***: If the user account does not have the correct permission, the following server response will occur. To be able to delete the file, the user must use ```CDIR``` to the restricted directory and enter their usernames and passwords. If you view the folder from the system file explorer, you can see that the file has not been deleted.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> KILL Restricted Folder/vanilla-tilt.js
-Not deleted because of folder access privileges
> 
```

### ***NAME*** Command
##### *Default Example*
This example illustrates a normal use case of the NAME command. At the end of these commands, the file test.txt is renamed to test2.txt within the system. This can be verified in the system file explorer.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> LIST F
+/
client-20180816.txt
client.txt
CS725ASSIGNMENT1.pdf
Folder
g4144.jpg
jdk.exe
mainscreen.png
Restricted Folder
test.txt

> NAME test.txt
+File exists. Send TOBE <new-name> command.
> TOBE test2.txt
+test.txt renamed to test2.txt
> 
```

##### *Other Cases*

***Renaming a file in another folder***: This snippet illustates the response if the file is in another folder.  At the end of these commands, the file helloworld.txt is renamed to goodbyeworld.txt within the system.  This can be verified in the system file explorer.

```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> NAME Folder/helloworld.txt
+File exists. Send TOBE <new-name> command.
> TOBE Folder/goodbyeworld.txt
+helloworld.txt renamed to goodbyeworld.txt
> 
```

***Renaming a file with commands in between***: The TOBE command is not bound to the NAME command. If the user wants to use other commands, they can do so. For example, if the user was to change the name, but is uncertain what other files are in the folder, they can use the ```LIST``` command before using the TOBE command.
```
> NAME test2.txt
+File exists. Send TOBE <new-name> command.
> LIST F
+/
client-20180816.txt
client.txt
CS725ASSIGNMENT1.pdf
Folder
g4144.jpg
jdk.exe
mainscreen.png
Restricted Folder
test2.txt

> TOBE test.txt
+test2.txt renamed to test.txt
> 
```

##### *Error Cases*
***New filename already exists***: This snippet illustrates the response from the server if the file already exists. The filename will not be changed and can be verified in the system file explorer.
```
> NAME test2.txt
+File exists. Send TOBE <new-name> command.
> TOBE test2.txt
-File wasn't renamed because it already exists.
```

***File does not exist***: This snippet illustrates the response from the server if the file does not exist.
```
> NAME notarealfile.txt
-PATH ERROR: //notarealfile.txt is not a file.
```

***File has restricted access***:  This snippet illustrates the response from the server the client does not have permissions to change files in the folder. The filename will not be changed.
```
> NAME Restricted Folder/mypasswords.txt
-File has resticted access //Restricted Folder/mypasswords.txt
```

### ***DONE*** Command
##### Default Examples
***No bandwidth used***: Done is used to signal the server that the client is closing down the connection. Both client and server close the socket, and the thread on the server stops. This frees up system resources such as CPU and Memory for other clients to use.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> DONE
+Closing connection. A total of 0kB was transferred
```

***Bandwidth used***: If the user has done any file transfers (either uploading or downloading) it is recorded and displayed when the user uses the ```DONE``` command.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> RETR test.txt
File Size:  39
Use SEND to retrieve file or STOP to cancel.
> SEND
> TYPE B
+Using Binary mode
> RETR mainscreen.png
File Size:  13380
Use SEND to retrieve file or STOP to cancel.
> SEND
> RETR g4144.jpg
File Size:  4245
Use SEND to retrieve file or STOP to cancel.
> SEND
> DONE
+Closing connection. A total of 17kB was transferred
```

### ***STOR*** Command

##### *Default Examples*

***Store with new generations***: This example illustates the file ```avalon-mm.png``` being transfer the first time without an existing file. The subsequent additions of ```avalon-mm.png``` create new file generations, based on the format *YYYYMMddHHmmss* (Year, Month, Day, Hour, Seconds). You can verify each stage with the use of ```LIST F``` or ```LIST V```.
```
FTP folder: C:\Users\tofutaco\Documents\COMPSYS725\java-sftp\SFTPClient\ftp
Client connected to localhost port 11510
+Welcome to Eugene's SFTP RFC913 Server
> USER ONLYUSER
!ONLYUSER logged in
> CDIR Folder
!Changed working dir to /Folder
> STOR NEW avalon-mm.png
+Using Binary mode
+File does not exist, will create new file. Sending SIZE 30073
+ok, waiting for file
Sending file...
+Saved /avalon-mm.png
> LIST F
+/Folder/
Another Folder
avalon-mm.png
goodbyeworld.txt
RFC 913 - Simple File Transfer Protocol.html

> STOR NEW avalon-mm.png
+Using Binary mode
+File exists, will create new generation of file. Sending SIZE 30073
+ok, waiting for file
Sending file...
+Saved /avalon-mm-20180819023951.png
> STOR NEW avalon-mm.png
+File exists, will create new generation of file. Sending SIZE 30073
+ok, waiting for file
Sending file...
+Saved /avalon-mm-20180819024005.png
> LIST F
+/Folder/
Another Folder
avalon-mm-20180819023951.png
avalon-mm-20180819024005.png
avalon-mm.png
goodbyeworld.txt
RFC 913 - Simple File Transfer Protocol.html

> 
```