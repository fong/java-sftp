/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*; 
import java.net.*; 
import java.util.Arrays;

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
    static Auth auth = new Auth();
    
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        boolean authOK = false;
//        
//        while (!authOK){
//            System.out.println("Please enter path to authentication file (auth.txt): ");
//            System.out.print("> ");
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            String filePath = br.readLine(); 
//            authOK = auth.setAuthPath(filePath);
//        }

        if (args.length == 1){
            authOK = auth.setAuthPath(args[0]);
        } else {
            System.out.println("ARG ERROR: No arguments. Needs to have 2 arguments: IP PORT");
        }
                        
        String[] clientCmd; 
	String capitalizedSentence; 
	
	ServerSocket welcomeSocket = new ServerSocket(6789); 
        
        System.out.println("Socket Started...");
        
	while(true) { 
	    
            Socket socket = welcomeSocket.accept();
            socket.setReuseAddress(true);
	    
	    BufferedReader inFromClient = 
		new BufferedReader(new
		    InputStreamReader(socket.getInputStream())); 
	    
	    DataOutputStream  outToClient = 
		new DataOutputStream(socket.getOutputStream()); 
	    
	    clientCmd = inFromClient.readLine().split(" "); 
	    
            String response = enterMode(clientCmd, socket);
                        
	    capitalizedSentence = response + '\n'; 
	    
	    outToClient.writeBytes(capitalizedSentence); 
        } 
    }
    
    public static String enterMode(String[] commandArgs, Socket socket) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"        
        switch (commandArgs[0]) {
            case "USER":
                return auth.user(commandArgs[1], socket);
            case "ACCT":
                if (!auth.getIP(socket).equals(auth.ip)){
                    return "Uh oh! Someone is using the FTP server right now.";
                } else {                    
                    return auth.acct(commandArgs[1]);
                }
            case "PASS":
                if (!auth.getIP(socket).equals(auth.ip)){
                    return "Uh oh! Someone is using the FTP server right now.";
                } else {                    
                    return auth.pass(commandArgs[1]);
                }
            case "TYPE":
                break;
            case "LIST":
                break;
            case "CDIR":
                break;
            case "KILL":
                break;
            case "NAME":
                break;
            case "DONE":
                break;
            case "RETR":
                break;
            case "STOR":
                break;
            default:
                break;
        }
        return Arrays.toString(commandArgs);
    }
}
