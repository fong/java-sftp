package sftpserver;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.*;

/*
 * Auth.txt File Format
 * USER {ACCOUNT|} PASSWORD
 * example 1: "username  "      (note the double space as there is no account or password)
 * example 2: "username  password"      (note the double space as there is no userDetails)
 * example 3: "username userDetails password"
 * example 4: "username userDetails1|userDetails2|userDetails3 password"
 */

/**
 *  TO DO: USER CLASS
 * @author tofutaco
 */
public class Auth {
    
    String authFile;
    Boolean userVerification = false;
    Boolean accountVerification = false;
    Boolean passwordVerification = false;
    
    List<String[]> activeUsers = new ArrayList<>();
    String[] userDetails = {"", "", "", "", "", "", "", ""}; // ip, port, userVerification, account, password 
    
    public Auth(){

    }
    
    public boolean setAuthPath(String filePathString){
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
    
    public String user(String userText, Socket connectionSocket) throws Exception{
        File file = new File("auth.txt");
        BufferedReader reader = null;
        String text;
        String response = null;
        
        System.out.println("USER");
        
        userVerification = false;
        accountVerification = false;
        passwordVerification = false;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            
            while ((text = reader.readLine()) != null) {
                System.out.println(text);
                String temp = ((((InetSocketAddress) connectionSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/","") +
                        " " + ((InetSocketAddress) connectionSocket.getRemoteSocketAddress()).getPort() + " " + text);
                
                userDetails = temp.split(" ", -1);
                if (userDetails[2].equals(userText)){
                    userVerification = true;
                    activeUsers.add(userDetails);
                    System.out.println(activeUsers);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found");
        } catch (IOException e) {
            System.out.println("IO Exception");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println("IO Exception on file close");
            }
        }
                
        if (!userVerification){
            System.out.println("NO USER FOUND");
            return "-Invalid user-id, try again";
        } else {
            if ("".equals(userDetails[3]) && "".equals(userDetails[3])){
                System.out.println("No Account/Password required");
                accountVerification = true;
                passwordVerification = true;
                response = "!" + userDetails[0] + " logged in";
            } else if (userDetails[3] != null || userDetails[4] != null){
                System.out.println("Account/Password required");
                response = "+User-id valid, send account and password";
            }
        }
        return response;
    }
    
    public String account(String accountText, Socket connectionSocket) throws Exception {
        
        return "0";
    }
}
