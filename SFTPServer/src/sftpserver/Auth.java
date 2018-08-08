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
    
    String user; // userVerification, account, password
    String[] accounts;
    String password; // userVerification, account, password
    String ip;

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
    
    public String user(String userText, Socket socket) throws Exception{
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
                String temp = text;
                
                String[] userDetails = temp.split(" ", -1);
                user = userDetails[0];
                accounts = userDetails[1].split("\\|");
                password = userDetails[2];
                
                if (user.equals(userText)){
                    userVerification = true;
                    ip = getIP(socket);
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
            return "-Invalid user-id, try again";
        } else {
            if ("".equals(password)){
                passwordVerification = true;
            }
            if (accounts.length <= 1){
                accountVerification = true;
            }
                
            if (passwordVerification && accountVerification){
                response = "!" + user + " logged in";
            } else {
                response = "+User-id valid, send account and password";
            }   
        }
        return response;
    }
    
    public String acct(String accountText) throws Exception {
        accountVerification = false;
        for (String account: accounts){
            if (account.equals(accountText)){
                accountVerification = true;
                if (passwordVerification){
                    return "!Account valid, logged-in";
                } else {
                    return "+Account valid, send password";
                }
            }
        }
        return "-Invalid account, try again";
    }
    
     public String pass(String passText) throws Exception {
         passwordVerification = false;
        if (password.equals(passText)){
            passwordVerification = true;
            if (accountVerification){          
                return "!Account valid, logged-in";
            } else {
                return "+Send account";
            }
        } else {
            return "-Wrong password, try again";
        }
    }
    
    public String getIP(Socket socket){
        return (((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
    }
}
