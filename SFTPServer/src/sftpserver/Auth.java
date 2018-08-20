package sftpserver;

import java.io.*;

/*
 * Auth.txt File Format
 * USER {ACCOUNT|} PASSWORD
 * example 1: "username  "      (note the double space as there is no account or password)
 * example 2: "username  password"      (note the double space as there is no userDetails)
 * example 3: "username userDetails password"
 * example 4: "username userDetails1|userDetails2|userDetails3 password"
 */

/**
 * Auth authenticates users
 * @author Eugene Fong (efon103)
 */
public class Auth {
    
    protected static String authFile;
    protected static Boolean userVerification = false;
    protected static Boolean accountVerification = false;
    protected static Boolean passwordVerification = false;
    
    protected static String user;
    protected static String account = "";
    protected static String[] accounts;
    protected static String password;

    public Auth(String authFile){
        Auth.authFile = authFile;   //locate authentication file
    }
    
    // USER {username} Command
    public String user(String userText) throws Exception{
        File file = new File("auth.txt");
        BufferedReader reader = null;
        String text;
        String response = null;
                
        userVerification = false;
        accountVerification = false;
        passwordVerification = false;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            // Scan file for credentials
            while ((text = reader.readLine()) != null) {
                String temp = text;
                String[] userDetails = temp.split(" ", -1);
                user = userDetails[0];
                accounts = userDetails[1].split("\\|");
                password = userDetails[2];
                //if username matches, the accounts and passwords are recorded and loop is broken
                if (user.equals(userText)){
                    userVerification = true;
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            if (SFTPServer.DEBUG) System.out.println("File Not Found in class AUTH, method USER");
        } catch (IOException e) {
            if (SFTPServer.DEBUG) System.out.println("IO Exception in class AUTH, method USER");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                if (SFTPServer.DEBUG) System.out.println("IO Exception on file close");
            }
        }
        //if no user found, return invalid user string
        if (!userVerification){
            return "-Invalid user-id, try again";
        } else {
            if ("".equals(password)){
                passwordVerification = true;    //if there is no password
            }
            if (accounts.length <= 1){
                accountVerification = true;     //if there is no account
            }
            //output messages corresponding to status
            if (passwordVerification && accountVerification){
                response = "!" + user + " logged in";
            } else if (passwordVerification && !accountVerification){
                response = "+User-id valid, send account";
            } else if (!passwordVerification && accountVerification){
                response = "+User-id valid, send password";  
            } else {
                response = "+User-id valid, send account and password";
            }   
        }
        return response;
    }
    
    //ACCT {account} Command
    public String acct(String accountText) throws Exception {
            //if there is no account
            if ("".equals(accounts[0]) && !passwordVerification){
                accountVerification = true;
                return (!Instance.cdirRestricted) ? ("+Account valid, send password") : ("+account ok, send password"); //If CDIR is flagged output different message
            }
            //Search accounts for valid account
            for (String account: accounts){
                if (account.equals(accountText)){
                    Auth.account = account;
                    accountVerification = true;
                    if (passwordVerification){
                        if (Instance.cdirRestricted) { //if CDIR was flagged change directory
                            Instance.directory = Instance.restrictedDirectory;
                            Instance.cdirRestricted = false;
                            return "!Changed working dir to " + Instance.restrictedDirectory;
                        } else {
                            return ("!Account valid, logged-in"); //if password not required log in
                        }
                    } else { //If CDIR is flagged output different message and if password required
                        return (!Instance.cdirRestricted) ? ("+Account valid, send password") : ("+account ok, send password");
                    }
                }
            }
            return (!Instance.cdirRestricted) ? ("-Invalid account, try again") : ("-invalid account"); // invalid account
    }
    
    // PASS {password} Command
    public String pass(String passText) throws Exception {
        //if there is no password
        if ("".equals(password) && !accountVerification){
            passwordVerification = true;
            return (!Instance.cdirRestricted) ? ("+Send account") : ("+password ok, send account");
        }
         
        if (password.equals(passText)){
            passwordVerification = true;
            if (accountVerification){
                if (Instance.cdirRestricted) {
                    Instance.directory = Instance.restrictedDirectory;
                    Instance.cdirRestricted = false;
                    return "!Changed working dir to " + Instance.restrictedDirectory; //if CDIR was flagged change directory
                } else {
                    return ("!Account valid, logged-in"); //if password not required log in
                }
            } else { //If CDIR is flagged output different message and if account required
                return (!Instance.cdirRestricted) ? ("+Send account") : (" +password ok, send account");
            }
        } else {
            return (!Instance.cdirRestricted) ? ("-Wrong password, try again") : ("-invalid password"); // invalid password
        }
    }
     
    // verified() is a helper function to check if user has met all checks
    public boolean verified(){
        return userVerification && accountVerification && passwordVerification;
    }
}
