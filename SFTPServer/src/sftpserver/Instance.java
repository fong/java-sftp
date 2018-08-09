/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
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
    String directory = "/";
    protected static Auth auth;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    
    String[] clientCmd; 
    String capitalizedSentence;
    
    // boolean userVerification = false;
    // boolean accountVerification = false;
    // boolean passwordVerification = false;
    
    Instance(Socket socket, String root, String authFile){
        this.root = root;
        this.socket = socket;
        Instance.auth = new Auth(authFile);
    }
    
    @Override
    public void run(){
        while(true){
            try {
                socket.setReuseAddress(true);

                inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
                outToClient = new DataOutputStream(socket.getOutputStream()); 

                clientCmd = inFromClient.readLine().split(" "); 
                System.out.println(Arrays.toString(clientCmd));

                String response = mode(clientCmd, socket);
                capitalizedSentence = response + '\n';
                outToClient.writeBytes(capitalizedSentence);
            } catch (Exception e){
                System.err.println(e);
                e.printStackTrace();
                break;
            }
            
            String line;
            while (true) {
                try {
                    line = inFromClient.readLine();
                    if ((line == null) || line.equals("DONE")) {
                        socket.close();
                        return;
                    } else {
                        outToClient.writeBytes(line + "\n\r");
                        outToClient.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
    
    public String mode(String[] commandArgs, Socket socket) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"        
        switch (commandArgs[0]) {
            case "USER":
                String res = auth.user(commandArgs[1], socket);
                System.out.println(res);
                return res;
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
            case "DONE":
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
        
        directory = "/";
        list = args[1];
        String response = null;
        long totalFileSize = 0;
        int nFiles = 0;
        int nDirectories = 0;
        
        if (args.length == 3){  
            directory = "/" + args[2];
        }
                
        if ("F".equals(list)){
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory))){
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
            //Path dirPath = Paths.get(root + directory);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root + directory))){
                outToClient.writeBytes("+" + directory + "\n");
                outToClient.writeBytes(String.format("%-54s%-4s%-10s%-30s", "|Name", "|R/W","|Size", "|Date") + "|\n");
                
                String line = "";
                for (int w = 0; w <= 98; w++){
                    if (w == 0 || w == 54 || w == 58 || w == 68 || w == 98){
                        line += "|";
                    } else {        
                        line += "-";
                    }
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
                    response = "";
                    response += String.format("%-50s", "|" + file.getName());
                    response += String.format("%-4s", dir);
                    response += String.format("%-4s", "|" + rw);
                    response += "|" + String.format("%9s", file.length()/1000 + "kB");
                    response += String.format("%-30s", "|" + new Date(file.lastModified()));
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
        return "\0";
    }
}
