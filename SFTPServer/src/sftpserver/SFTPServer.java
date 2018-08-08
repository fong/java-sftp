/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sftpserver;

import java.io.*; 
import java.net.*; 
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;

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
    static String list;
    static String root;
    static String directory = "/";
    static Auth auth = new Auth();
    static BufferedReader inFromClient;
    static DataOutputStream outToClient;
    
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        boolean authOK = false;
        
//      // CLI authentication text file  
//        while (!authOK){
//            System.out.println("Please enter path to authentication file (auth.txt): ");
//            System.out.print("> ");
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            String filePath = br.readLine(); 
//            authOK = auth.setAuthPath(filePath);
//        }
    
        // Program argument authentication text file
        if (args.length == 2){
            authOK = auth.setAuthPath(args[0]);
            root = args[1];
        } else {
            System.out.println("ARG ERROR: No arguments. Needs to have 2 arguments: IP PORT");
        }
                        
        String[] clientCmd; 
	String capitalizedSentence; 
	
	ServerSocket welcomeSocket = new ServerSocket(6789); 
        
        System.out.println("Socket Started...");
        
//        Iterable<Path> dirs = FileSystems.getDefault().getRootDirectories();
//        for (Path name: dirs) {
//            System.err.println(name);
//        }
        
	while(true) { 
	    
            Socket socket = welcomeSocket.accept();
            socket.setReuseAddress(true);
	    
	    inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
	    outToClient = new DataOutputStream(socket.getOutputStream()); 
	    
	    clientCmd = inFromClient.readLine().split(" "); 
            System.out.println(Arrays.toString(clientCmd));
	    
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
                if (!auth.getIP(socket).equals(auth.ip)){
                    return "Uh oh! Someone is using the FTP server right now.";
                } else {                    
                    return type(commandArgs[1]);
                }
            case "LIST":
                if (!auth.getIP(socket).equals(auth.ip)){
                    return "Uh oh! Someone is using the FTP server right now.";
                } else {                    
                    return list(commandArgs);
                }
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
        return "COMMAND ERROR: Server recieved " + Arrays.toString(commandArgs);
    }
    
    // TYPE { A | B | C }        
    public static String type(String type){
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
    public static String list(String[] args) throws IOException{
        
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
            Path dir = Paths.get(root + directory);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)){
                outToClient.writeBytes("+" + directory + "\n");
                for (Path filePath: stream) {
                    outToClient.writeBytes(filePath.getFileName() + "\n");
                }                
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                outToClient.writeBytes("-" + x + "\n");
            }
        } else if ("V".equals(list)){
            Path dirPath = Paths.get(root + directory);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)){
                
                outToClient.writeBytes("+" + directory + "\n");
                
                outToClient.writeBytes(String.format("%-50s%-4s%-4s%-10s%-30s", "|Name", "|DIR", "|R/W","|Size", "|Date") + "|\n");
                
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
                    response += String.format("%-4s", "|" + dir);
                    response += String.format("%-4s", "|" + rw);
                    response += "|" + String.format("%9s", file.length()/1000 + "kB");
                    response += String.format("%-30s", "|" + new Date(file.lastModified()));
                    response += "|\n";
                    outToClient.writeBytes(response);
                }
                String stats = nFiles + " File(s)\t " +
                                nDirectories + " Dir(s)\t " + totalFileSize/1000 + "kB Total File Size" + "\n";
                outToClient.writeBytes(stats);
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                outToClient.writeBytes("-" + x + "\n");
            }
        }
        return "\0";
    }
}
