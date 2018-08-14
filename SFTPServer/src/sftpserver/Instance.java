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
    
    String dirpath = "";
    String filepath = "";
    
    boolean tobe = false;
        
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
            sendToClient("+Welcome to Eugene's SFTP RFC913 Server\0");
        } catch (Exception e) {
            
        }
        
        while(running){
            try {
                clientCmd = readFromClient().split(" "); 
                
                if (clientCmd[0].equals("DONE")){
                    sendToClient("+Closing connection...\0");
                    socket.close();
                    running = false;
                } else {
                    String response = mode(clientCmd, socket);
                    sendToClient(response + '\0');
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
                return kill(commandArgs);
            case "NAME":
                return name(commandArgs);
            case "TOBE":
                return tobe(commandArgs);
            case "RETR":
                return retr(commandArgs);
            case "STOR":
                return stor(commandArgs);
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
            StringBuilder response = new StringBuilder();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory + listDirectory))){
                response.append("+").append(directory).append("\r\n");
                for (Path filePath: stream) {
                    response.append(filePath.getFileName()).append("\r\n");
                }
                sendToClient(response.toString());
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                sendToClient("-" + x.toString());
            }
        } else if ("V".equals(list)){
            StringBuilder response = new StringBuilder();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory + listDirectory))){
                response.append("+").append(directory).append("\r\n");
                response.append(String.format("%-68s%-4s%-10s%-20s%-20s", "|Name", "|R/W","|Size", "|Date", "|Owner")).append("|\r\n");
                
                String line = "";
                for (int w = 0; w <= 122; w++){
                    line = ((w == 0 || w == 68 || w == 72 || w == 82 || w == 102 || w == 122) ? (line += "|") : (line += "-"));
                }
                line += "\r\n";
                response.append(line);
                
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
                    
                    String fileList = "";
                    fileList += String.format("%-64s", "|" + file.getName());
                    fileList += String.format("%-4s", dir);
                    fileList += String.format("%-4s", "|" + rw);
                    fileList += "|" + String.format("%9s", file.length()/1000 + "kB");
                    fileList += String.format("%-20s", "|" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(file.lastModified())));
                    fileList += String.format("%-20s", "|" + owner);
                    fileList += "|\n";
                    response.append(fileList);
        }
                String stats = nFiles + " File(s)\t " +
                                nDirectories + " Dir(s)\t " + totalFileSize/1000 + "kB Total File Size" + "\n";
                response.append(stats);
                sendToClient(response.toString());
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                sendToClient("-" + x.toString());
            }
        }
        return "\0";
    }
    
    public String cdir(String[] args) throws Exception{
        String listDirectory = "";
        if (args.length > 2){
            String response = "";
            for (int i = 1; i < args.length; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            listDirectory = "/" + response;
        } else {
            listDirectory = "/" + args[1];
        }
        
        System.out.println(listDirectory);
        
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
    
    public String kill(String[] args) throws Exception {
        boolean passRestriction;
        if (typeCheck(args)){
            passRestriction = verify(args);
        } else {
            return "\0";
        }
        
        if (passRestriction){
            Path fileToDelete = new File(root + directory + filepath).toPath();
		
            // Delete the file
            try {
                Files.delete(fileToDelete);
                return "+" + fileToDelete.getFileName() +" deleted";
            } catch (NoSuchFileException x) {
                return "-Not deleted because file does not exist in the directory";
            } catch (IOException x) {
                return "-Not deleted because of IO error. It may be protected.";
            }
        } else {
            return "-Not deleted because of folder access privileges";
        }
    }
    
    public String name(String[] args) throws Exception {
        boolean passRestriction;
        
        if (!tobe){
            if (typeCheck(args) && verify(args)){
                tobe = true;
                return "+File exists. Send TOBE <new-name> command.";
            } else if (!typeCheck(args) && verify(args)){
                tobe = false;
                return "-Can't find " + directory + filepath;
            } else if (typeCheck(args) && !verify(args)){
                tobe = false;
                return "-File has resticted access " + directory + filepath;
            } else {
                tobe = false;
                return "-Can't find file and folder has restricted access";
            }
        } else {
            return "+File exists, awaiting TOBE command.";
        }
    }
    
    public String tobe(String[] args) throws Exception {
        if (tobe){
            File newName = new File(root + directory + "/" + args[1]);
            File oldName = new File(root + directory + filepath);
            if (newName.isFile()) return "-File wasn't renamed because it already exists.";
                // Delete the file
            String filename = oldName.toString();
            oldName.renameTo(newName);
            tobe = false;
            return "+" + filename + " renamed to " + newName.getName();
        } else {
            return "-No file selected";
        }
    }
    
    public boolean done() throws Exception{
        sendToClient("+Closing connection...\n");
        socket.close();
        return false;
    }
    
    public String retr(String[] args){
        return "\0";
    }
    
    public String stor(String[] args){
        return "\0";
    }
    
    private String readFromClient() {
        String text = "";
        int character = 0;

        while (true){
            try {
                character = inFromClient.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (character == 0) {
                break;
            }
            text = text.concat(Character.toString((char)character));
        }
        return text;
    }
    
    private void sendToClient(String text){
        try {
            outToClient.writeBytes(text.concat(Character.toString('\0')));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private boolean typeCheck(String[] args) throws Exception{
        if (args.length > 2){
            String response = "";
            for (int i = 1; i < args.length-1; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            dirpath = "/" + response; 
            String[] t = args[args.length-1].split("/");
            dirpath += t[0];
            filepath = dirpath + "/" + t[1];
        } else {
            dirpath = "/";
            filepath = "/" + args[1];
        }
        
        File dir = new File(root + directory + dirpath);
        File file = new File(root + directory + filepath);
        
        if (!dir.isDirectory()){
            sendToClient("-Can't connect to directory because: " + directory + dirpath + " is not a directory.");
            return false;
        }
        if (!file.isFile()){
            sendToClient("-Can't connect to directory because: " + directory + filepath + " is not a file.");
            return false;
        }   
        return true;
    }
    
    private boolean verify(String[] args) throws Exception{        
        System.out.println(root + directory + dirpath + "/.restrict");
        File file = new File(root + directory + dirpath + "/.restrict");
        BufferedReader reader = null;
        String text;
        String[] restrict;
        String[] restrictedAccounts = null;
        String restrictedPassword = "";
        
        try {
            reader = new BufferedReader(new FileReader(file));           
            
            while ((text = reader.readLine()) != null) {
                restrict = text.split(" ", -1);
                restrictedAccounts = restrict[0].split("\\|");
                restrictedPassword = restrict[1];

                for (String restrictedAccount : restrictedAccounts){
                    if (Auth.account.equals(restrictedAccount) && Auth.password.equals(restrictedPassword)){
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unrestricted Folder");
            return true;
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
        return false;
    }
}
