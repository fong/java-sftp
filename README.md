# RFC 913 - Simple File Transfer Protocol
 **Java**
 
 Eugene Fong (efon103)
 
 [github.com/fong/java-sftp](https://github.com/fong/java-sftp)

## List of Working Features
 |Command|USER|ACCT|PASS|TYPE|LIST|CDIR|KILL|NAME|DONE|RETR|STOR|
 |:-----:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
 |Client |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |
 |Server |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |Yes |
 
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

## How to setup SFTPServer and run:
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

## How to setup Client and run:
1. Compile SFTPClient by opening the folder ```SFTPClient\src\sftpclient``` in the command prompt. Use the command ```javac *.java``` to compile to class files.
2. From the ```sftpclient``` folder, execute your SFTPClient with the command ```java -cp ../ sftpclient.SFTPClient {IP} {PORT}```. For example, ```java -cp ../ sftpserver.SFTPServer localhost 15100```. If the server is not online, or unable to be connected, a ```Connection refused. Server may not be online.``` error will appear.