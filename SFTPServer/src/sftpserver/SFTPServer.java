package sftpserver;

import java.io.*; 
import java.net.*; 
import java.nio.file.*;

/**
 * SFTPServer
 * This is an implementation of the RFC 913 Simple File Transfer Protocol
 * @author Eugene Fong (efon103)
 */
public class SFTPServer {

    /**
     * @param port - Server Port
     * @param auth_file - Location of Authentication File
     * @throws java.lang.Exception
     */
    static boolean DEBUG = false; //set true is debugging
    
    static String authFile;
    
    public static void main(String[] args) throws Exception {    
        // Check program arguments for port and location of authentication text file
        if (args.length == 2){
            if (!setAuthPath(args[1])){
                return;
            }
        } else {
            System.out.println("ARG ERROR: Wrongs arguments. Needs to have 2 arguments: PORT AUTH-FILE");
            return;
        }
	
        //create ftp folder if it doesn't exist
        System.out.println("Creating /ftp folder...");
        new File(FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile().toString()).mkdirs();
        System.out.println("/ftp folder ok!");

	ServerSocket welcomeSocket = new ServerSocket(Integer.parseInt(args[0])); 
        System.out.println("Socket Started...");
        
	while(true) {
            Socket socket = welcomeSocket.accept();
            new Instance(socket, authFile).start(); //run an SFTP instance for each client connection
        } 
    }
    
    //setAuthPath checks if the authentication file is valid before the server starts letting clients connect.
    public static boolean setAuthPath(String filePathString){
        File f = new File(filePathString);
        if(f.exists() && !f.isDirectory()) { 
            authFile = filePathString;
            System.out.println("Found authentication file!");
            return true;
        } else {
            authFile = null;
            System.out.println("No authentication file found. Are you sure you have the right path?");
            return false;
        }
    }
}
