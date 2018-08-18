/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*; 
import java.net.*; 
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author tofupudding
 */
public class SFTPServer {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    
    static String mode;
    static String list;
    static String root;
    static String directory = "/";
    static String authFile;
    // static Auth auth = new Auth();
    static BufferedReader inFromClient;
    static DataOutputStream outToClient;
    
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        boolean authOK = false;
        
//      // CLI authentication text file  
//        while (!authOK){
//            System.out.println("Please enter path to authentication file (auth.txt): ");
//            System.out.print("> ");
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            String filePath = br.readLine(); 
//            authOK = auth.setAuthPath(filePath);
//        }
    
        // Program argument authentication text file
        if (args.length == 2){
            if (!setAuthPath(args[1])){
                return;
            }
        } else {
            System.out.println("ARG ERROR: Wrongs arguments. Needs to have 2 arguments: PORT AUTH-FILE");
            return;
        }
	
        System.out.println("Creating /ftp folder...");
        new File(FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile().toString()).mkdirs();
        System.out.println("/ftp folder ok!");

	ServerSocket welcomeSocket = new ServerSocket(Integer.parseInt(args[0])); 
        System.out.println("Socket Started...");
        
	while(true) {
            Socket socket = welcomeSocket.accept();
            new Instance(socket, authFile).start();
        } 
    }
    
        
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
