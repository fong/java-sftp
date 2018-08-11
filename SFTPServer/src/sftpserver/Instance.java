/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.DateFormat;
import java.util.*;

/**
 *
 * @author tofupudding
 */
public class Instance extends Thread{
    
    protected Socket socket;
    
    String mode;
    String list;
    String root;
    public static String restrictedDirectory = "/";
    public static String directory = "/";
    public static boolean cdirRestricted = false;
    protected static Auth auth;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    
    String[] clientCmd; 
    String capitalizedSentence;
        
    Instance(Socket socket, String root, String authFile){
        this.root = root;
        this.socket = socket;
        Instance.auth = new Auth(authFile);
    }
    
    @Override
    public void run(){
        boolean running = true;
        
        try {
            socket.setReuseAddress(true);
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));               
            outToClient = new DataOutputStream(socket.getOutputStream());
            outToClient.writeBytes("+Welcome to Eugene's SFTP RFC913 Server\n");
        } catch (Exception e) {
            
        }
        
        while(running){
            try {
                clientCmd = inFromClient.readLine().split(" "); 
                
                if (clientCmd[0].equals("DONE")){
                    outToClient.writeBytes("+Closing connection...\n");
                    socket.close();
                    running = false;
                } else {
                    String response = mode(clientCmd, socket);
                    outToClient.writeBytes(response + '\n');
                }
            } catch (Exception e){
                //e.printStackTrace();
                //break;
            }
        }
        System.out.println("Closed Thread");
    }
    
    public String mode(String[] commandArgs, Socket socket) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"

//        if (!"USER".equals(commandArgs[1]) || !"ACCT".equals(commandArgs[1]) || !"PASS".equals(commandArgs[1])){
//            if (!Auth.userVerification && !Auth.accountVerification && !Auth.passwordVerification){
//                return "-Not Logged In";
//            }
//        }
        
        switch (commandArgs[0]) {
            case "USER":
                return auth.user(commandArgs[1]);
            case "ACCT":                  
                return auth.acct(commandArgs[1]);
            case "PASS":
                return auth.pass(commandArgs[1]);
            case "TYPE":
                return type(commandArgs[1]);
            case "LIST":
                return list(commandArgs);
            case "CDIR":
                return cdir(commandArgs);
            case "KILL":
                break;
            case "NAME":
                break;
            case "RETR":
                break;
            case "STOR":
                break;
            default:
                break;
        }
        return "COMMAND ERROR: Server recieved " + Arrays.toString(commandArgs);
    }
    
    // TYPE { A | B | C }        
    public String type(String type){
        if (null == type){
            return "-Type not valid";
        } else switch (type) {
            case "A":
                type = "A";
                return "+Using Ascii mode";
            case "B":
                type = "B";
                return "+Using Binary mode";
            case "C":
                type = "C";
                return "+Using Continuous mode";
            default:
                return "-Type not valid";
        }
    }
    
    // LIST { F | V } directory-path
    public String list(String[] args) throws IOException{
        
        String listDirectory = "/";
        list = args[1];
        long totalFileSize = 0;
        int nFiles = 0;
        int nDirectories = 0;
        
        if (args.length > 2){
            String response = "";
            for (int i = 2; i < args.length; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            listDirectory = "/" + response;
        }
                
        if ("F".equals(list)){
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory + listDirectory))){
                outToClient.writeBytes("+" + directory + "\n");
                for (Path filePath: stream) {
                    outToClient.writeBytes(filePath.getFileName() + "\n");
                }                
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                outToClient.writeBytes("-" + x.toString() + "\n");
            }
        } else if ("V".equals(list)){            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory + listDirectory))){
                outToClient.writeBytes("+" + directory + "\n");
                outToClient.writeBytes(String.format("%-68s%-4s%-10s%-20s%-20s", "|Name", "|R/W","|Size", "|Date", "|Owner") + "|\n");
                
                String line = "";
                for (int w = 0; w <= 122; w++){
                    line = ((w == 0 || w == 68 || w == 72 || w == 82 || w == 102 || w == 122) ? (line += "|") : (line += "-"));
                }
                line += "\n";
                outToClient.writeBytes(line);
                
                for (Path filePath: stream) {
                    File file = new File(filePath.toString());
                    String rw = "";
                    String dir = "";
                           
                    if (file.isDirectory()){
                        dir = "DIR";
                        nDirectories++;
                    }
                    if (file.isFile()) {
                        totalFileSize += file.length();
                        nFiles++;
                    }
                    if (file.canRead()) {rw += "R";}
                    if (file.canWrite()){
                        if ("R".equals(rw)){rw += "/";}
                        rw += "W";
                    }
                    String owner = "";
                    try {
                        FileOwnerAttributeView attr = Files.getFileAttributeView(file.toPath(), FileOwnerAttributeView.class);
                        owner = attr.getOwner().getName();
                    } catch (IOException e) {	
                        e.printStackTrace();
                    }
                    
                    String response = "";
                    response += String.format("%-64s", "|" + file.getName());
                    response += String.format("%-4s", dir);
                    response += String.format("%-4s", "|" + rw);
                    response += "|" + String.format("%9s", file.length()/1000 + "kB");
                    response += String.format("%-20s", "|" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(file.lastModified())));
                    response += String.format("%-20s", "|" + owner);
                    response += "|\n";
                    outToClient.writeBytes(response);
                }
                String stats = nFiles + " File(s)\t " +
                                nDirectories + " Dir(s)\t " + totalFileSize/1000 + "kB Total File Size" + "\n";
                outToClient.writeBytes(stats);
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                outToClient.writeBytes("-" + x.toString() + "\n");
            }
        }
        return "\0";
    }
    
    public String cdir(String[] args) throws Exception{
        String listDirectory = "";
        if (args.length > 2){
            String response = "";
            for (int i = 2; i < args.length; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            listDirectory = "/" + response;
        }
        
        File file = new File(root + listDirectory);
        if (!file.isDirectory()){
            return "-Can't connect to directory because: " + listDirectory + " is not a directory.";
        }
        
        file = new File(root + listDirectory + "/.restrict");
        BufferedReader reader = null;
        String text;
        String[] restrict;
        String[] restrictedAccounts = null;
        String restrictedPassword = "";
        Boolean passRestriction = false;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            
            while ((text = reader.readLine()) != null) {
                restrict = text.split(" ", -1);
                restrictedAccounts = restrict[0].split("\\|");
                restrictedPassword = restrict[1];

                for (String restrictedAccount : restrictedAccounts){
                    if (Auth.account.equals(restrictedAccount) && Auth.password.equals(restrictedPassword)){
                        passRestriction = true;
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unrestricted Folder");
            passRestriction = true;
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
        if (!passRestriction){
            Auth.accounts = restrictedAccounts;
            Auth.password = restrictedPassword;
            Auth.accountVerification = false;
            Auth.passwordVerification = false;
            restrictedDirectory = listDirectory;
            cdirRestricted = true;
            return "+directory ok, send account/password";
        } else {
            directory = ("/".equals(listDirectory)) ? ("/") : (listDirectory);
            return "!Changed working dir to " + listDirectory;
        }
    }
    
    public boolean done() throws Exception{
        outToClient.writeBytes("+Closing connection...\n");
        socket.close();
        return false;
    }
}
