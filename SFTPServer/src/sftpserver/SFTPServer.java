/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*; 
import java.net.*; 

/**
 *
 * @author tofupudding
 */
public class SFTPServer {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        boolean authOK = false;
        Auth auth = new Auth();
        
        while (!authOK){
            System.out.println("Please enter path to authentication file (auth.txt): ");
            System.out.print("> ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String filePath = br.readLine(); 
            authOK = auth.setAuthPath(filePath);
        }
                        
        String clientSentence; 
	String capitalizedSentence; 
	
	ServerSocket welcomeSocket = new ServerSocket(6789); 
        
        System.out.println("Socket Started...");
        
	while(true) { 
	    
            Socket connectionSocket = welcomeSocket.accept(); 
	    
	    BufferedReader inFromClient = 
		new BufferedReader(new
		    InputStreamReader(connectionSocket.getInputStream())); 
	    
	    DataOutputStream  outToClient = 
		new DataOutputStream(connectionSocket.getOutputStream()); 
	    
	    clientSentence = inFromClient.readLine(); 
	    
            String response = auth.user(clientSentence.split(" ")[1]);
            
	    capitalizedSentence = response + '\n'; 
	    
	    outToClient.writeBytes(capitalizedSentence); 
        } 
    }  
}
