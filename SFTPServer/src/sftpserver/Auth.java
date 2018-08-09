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
    
    protected static String authFile;
    protected static Boolean userVerification = false;
    protected static Boolean accountVerification = false;
    protected static Boolean passwordVerification = false;
    
    protected static String user; // userVerification, account, password
    protected static String[] accounts;
    protected static String password; // userVerification, account, password

    public Auth(String authFile){
        Auth.authFile = authFile;
    }
    
    public String user(String userText) throws Exception{
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
                String temp = text;
                
                String[] userDetails = temp.split(" ", -1);
                user = userDetails[0];
                accounts = userDetails[1].split("\\|");
                password = userDetails[2];

                if (user.equals(userText)){
                    userVerification = true;
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
        //accountVerification = false;
        if ("".equals(accounts[0]) && !passwordVerification){
            accountVerification = true;
            return "+Account valid, send password";
        }
        
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
        System.out.println(user);
        System.out.println(Arrays.toString(accounts));
        System.out.println(password);
         
        if ("".equals(password) && !accountVerification){
            passwordVerification = true;
            return "+Send account";
        }
         
        //passwordVerification = false;
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
