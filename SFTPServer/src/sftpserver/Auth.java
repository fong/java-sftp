package sftpserver;

import java.io.*;
import java.util.Arrays;

/*
 * Auth.txt File Format
 * USER {ACCOUNT|} PASSWORD
 * example 1: "username  "      (note the double space as there is no account or password)
 * example 2: "username  password"      (note the double space as there is no userDetails)
 * example 3: "username userDetails password"
 * example 4: "username userDetails1|userDetails2|userDetails3 password"
 */

/**
 *
 * @author tofutaco
 */
public class Auth {
    
    String authFile;
    Boolean userVerification = false;
    Boolean accountVerification = false;
    Boolean passwordVerification = false;
    
    String[] userDetails = {"", "", ""};
    
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
    
    public String user(String userText) throws Exception{
        File file = new File("auth.txt");
        BufferedReader reader = null;
        String text;
        
        System.out.println("USER");
        
        userVerification = false;
        accountVerification = false;
        passwordVerification = false;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            
            while ((text = reader.readLine()) != null) {
                System.out.println(text);
                userDetails = text.split(" ", -1);
                if (userDetails[0].equals(userText)){
                    userVerification = true;
                    System.out.println(Arrays.toString(userDetails));
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
        
        String response = null;
        
        if (!userVerification){
            System.out.println("NO USER FOUND");
            return "-Invalid user-id, try again";
        } else {
            if ("".equals(userDetails[1]) && "".equals(userDetails[2])){
                System.out.println("No Account/Password required");
                accountVerification = true;
                passwordVerification = true;
                response = "!" + userDetails[0] + " logged in";
            } else if (userDetails[1] != null || userDetails[2] != null){
                System.out.println("Account/Password required");
                response = "+User-id valid, send account and password";
            }
        }
        return response;
    }
}
