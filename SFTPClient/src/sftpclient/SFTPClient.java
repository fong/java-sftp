package sftpclient;

import java.io.*; 
import java.net.*; 
import java.nio.file.*;
import java.util.*;

public class SFTPClient {

    /**
     * @param args the command line arguments
     */
    static boolean DEBUG = false;
    
    static String[] sftpCommands;
    static String mode;
    
    static String ip;
    static int port;
    
    static boolean validAuth = false;
    
    static String sendMode = "A";
    static String filename;
    static long fileSize;
    static File ftp = FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile();
    
    static Socket socket;
    static DataOutputStream outToServer;
    static BufferedReader inFromServer;
    static DataOutputStream binToServer;
    static DataInputStream binFromServer;
    static boolean running = true;

    public static void main(String[] args) throws Exception{
        System.out.println("FTP folder: " + ftp.toString());
        new File(ftp.toString()).mkdirs();
        
        // USER=1, ACCT=2, PASS=3, TYPE=4, LIST=5, CDIR=6, KILL=7, NAME=8, DONE=9, RETR=10, STOR=11
        sftpCommands = new String[]{"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL",
                        "NAME", "TOBE", "DONE", "RETR", "SEND", "STOP", "STOR"};
        
        if (args.length == 2){
            ip = args[0];
            port = Integer.parseInt(args[1]);
            
            try{
                socket = new Socket(ip, port);

                outToServer = new DataOutputStream(socket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                binToServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                binFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                
                System.out.println("Client connected to " + ip + " port " + port);
                System.out.println(readFromServer());
                
                while(running){
                    System.out.print("> ");
                    String[] commandArgs = selectMode();
                    if (commandArgs != null) enterMode(commandArgs);
                } 
            } catch (NumberFormatException e) {
               System.out.println("PORT ERROR: Port argument needs to be a number"); 
            } catch (ConnectException e) {
               System.out.println("Connection refused. Server may not be online."); 
            } catch (Exception e){};
        } else {
            System.out.println("ARG ERROR: No arguments. Needs to have 2 arguments: IP PORT");
        }
    }
    
    public static String[] selectMode() throws Exception{
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = br.readLine();
        String[] commands = input.split(" ");
        boolean noCommand = false;
        
        for (String sftpCommand : sftpCommands){
            if (commands[0].equals(sftpCommand)){
                if (DEBUG) System.out.println(sftpCommand);
                mode = sftpCommand;
                noCommand = true;
                return commands;
            }
        }
        
        if (!noCommand){
            System.out.println("INPUT ERROR: Command not recognised");
            System.out.println("Commands available: "
                    + "\"USER\", \"ACCT\", \"PASS\", \"TYPE\", \"LIST\","
                    + "\"CDIR\", \"KILL\", \"NAME\", \"DONE\", \"RETR\", \"STOR\"");
        }
        return null;
    }
    
    public static void enterMode(String[] commandArgs) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"
        if (null != mode)
        
        switch (mode) {
            case "USER":
                auth("USER",commandArgs);
                break;
            case "ACCT":
                auth("ACCT",commandArgs);
                break;
            case "PASS":
                auth("PASS",commandArgs);
                break;
            case "TYPE":
                type(commandArgs);
                break;
            case "LIST":
                list(commandArgs);
                break;
            case "CDIR":
                cdir(commandArgs);
                break;
            case "KILL":
                kill(commandArgs);
                break;
            case "NAME":
                name(commandArgs);
                break;
            case "TOBE":
                tobe(commandArgs);
                break;
            case "DONE":
                try {
                    sendToServer("DONE");
                    System.out.println(readFromServer());
                    socket.close();
                } catch (Exception e){
                   System.out.println("Unable to close connection with server or connection already closed");
                }
                running = false;
                break;
            case "RETR":
                retr(commandArgs);
                break;
            case "SEND":
                send();
                break;
            case "STOP":
                stopRetr();
                break;
            case "STOR":
                stor(commandArgs);
                break;
            default:
                break;
        }
    }
    
    public static void auth(String mode, String[] commandArgs) throws Exception{
        if (commandArgs.length != 2){
            String argsError = null;
            
            switch (mode) {
                case "USER":
                    argsError = "USER user-id";
                    break;
                case "ACCT":
                    argsError = "ACCT account";
                    break;
                case "PASS":
                    argsError = "PASS password";
                    break;
                default:
                    break;
            }
            
            System.out.println("ARG ERROR: found " + (commandArgs.length-1) +
                    " arguments, 1 needed. Command format: " + argsError);
        } else {                               
            sendToServer(mode + " " + commandArgs[1]);
            validAuth = true;
            System.out.println(readFromServer());
        }
    }
    
    public static void type(String[] commandArgs) throws Exception{
        if (commandArgs.length == 2){                        
            sendToServer("TYPE " + commandArgs[1]);
            String serverResponse = readFromServer();

            switch (serverResponse.substring(0, 1)) {
                case "+":
                    sendMode = commandArgs[1];
                    System.out.println(serverResponse);
                    break;
                default:
                    System.out.println(serverResponse);
                    break;
            }
        } else {
            System.out.println("ARG ERROR: Invalid arguments. Command format: TYPE { A | B | C }");
        }   
    }
    
    public static void list(String[] commandArgs) throws Exception {
        if ((commandArgs.length >= 2) && ("F".equals(commandArgs[1]) || "V".equals(commandArgs[1]))){ 
            if (commandArgs.length == 2){    
                sendToServer("LIST " + commandArgs[1]);
            } else {
                String resp = " ";
                for (int i = 2; i < commandArgs.length; i++){
                    resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
                }
                sendToServer("LIST " + commandArgs[1] + resp);
            }
            System.out.println(readFromServer());
        } else {
            System.out.println("ARG ERROR: Invalid arguments. Command format: LIST { F | V } directory-path");
        }
    }
    
    public static void cdir(String[] commandArgs) throws Exception {
        String resp = "";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
        sendToServer("CDIR " + resp);
        System.out.println(readFromServer());
    }
    
    public static void kill(String[] commandArgs) throws Exception {
        if (commandArgs.length < 2){
            System.out.println("ARG ERROR: Not enough arguments. KILL <filename> required");
        } else {
            String resp = "";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            sendToServer("KILL " + resp);
            System.out.println(readFromServer());
        }  
    }
    
    public static void name(String[] commandArgs) throws Exception {
        if (commandArgs.length < 2){
            System.out.println("ARG ERROR: Not enough arguments. NAME <current-filename> required");
        } else {
            String resp = "";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            sendToServer("NAME " + resp);
            System.out.println(readFromServer());
        }
    }
    
    public static void tobe(String[] commandArgs) throws Exception {
        if (commandArgs.length < 2){
            System.out.println("ARG ERROR: Not enough arguments. TOBE <new-filename> required");
        } else {
            String resp = "";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            sendToServer("TOBE " + resp);
            System.out.println(readFromServer());
        }
    }
    
    public static void retr(String[] commandArgs) throws Exception {
        if (commandArgs.length < 2){
            System.out.println("ARG ERROR: Not enough arguments. RETR <filename> required");
        } else {
            String resp = "";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            String[] x = commandArgs[commandArgs.length-1].split("/");
            filename = x[x.length-1];
            sendToServer("RETR " + resp);
            String serverResponse = readFromServer();
            if ("-".equals(serverResponse.substring(0,1))){
                System.out.println(serverResponse);
            } else {
                fileSize = Integer.parseInt(serverResponse);
                System.out.println("File Size:  "+ fileSize);
                System.out.println("Use SEND to retrieve file or STOP to cancel.");
            }
        }
    }
        
    public static void send(){
        sendToServer("SEND ");
        try {
            File file = new File(ftp.getPath() + "/" + filename); 
            Long timeout = new Date().getTime() + fileSize/32 + 32; //offset if filesize is 1 byte
            if ("A".equals(sendMode)) {
                try (BufferedOutputStream bufferedStream = new BufferedOutputStream(new FileOutputStream(file, false))) {
                    for (int i = 0; i < fileSize; i++) {
                        if (new Date().getTime() >= timeout){
                            System.out.println("Transfer taking too long. Timed out after " + fileSize/32 + " seconds.");
                            return;
                        }
                        bufferedStream.write(inFromServer.read());
                    }
                    bufferedStream.flush();
                    bufferedStream.close();
                }
            } else {
                try (FileOutputStream fileStream = new FileOutputStream(file, false)) {
                    int e;
                    byte[] bytes = new byte[(int) fileSize];
                    while (true) {
                        e = binFromServer.read(bytes);
                        if (new Date().getTime() >= timeout){
                            System.out.println("Transfer taking too long. Timed out after " + fileSize/32 + " seconds.");
                            return;
                        }
                        fileStream.write(bytes, 0, e);
                        if (e < 8192){
                            break;
                        }
                    }
                    fileStream.flush();
                    fileStream.close();
                }
            }
        } catch (FileNotFoundException n){
            System.out.println("ERROR: Local FTP directory does not exist.");
        } catch (SocketException s){
            System.out.println("Server connection was closed before file finshed transfer.");
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
        } 
    }
    
    public static void stopRetr(){
        sendToServer("STOP ");
        System.out.println(readFromServer());
    }
    
    public static void stor(String[] commandArgs) throws Exception {
        if (commandArgs.length < 2){
            System.out.println("ARG ERROR: Not enough arguments. STOR <new-filename> required");
        } else {
            String resp = "";
            for (int i = 2; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            
            File file = new File(ftp.getPath() + "/" + resp);

            if (isBinary(file) && "A".equals(sendMode)){
                //System.out.println("-File is Binary. Switching to TYPE B");
                sendToServer("TYPE B");
                String serverResponse = readFromServer();
                if ("+".equals(serverResponse.substring(0, 1))){
                    sendMode = "B";
                    System.out.println(serverResponse);
                } else {
                    System.out.println(serverResponse);
                    System.out.println("STOR canceled.");
                    return;
                }
            } else if (!isBinary(file) && ("B".equals(sendMode) || "C".equals(sendMode))){
                System.out.println("-File is ASCII. Current TYPE is B or C. Please switch to A");
                sendToServer("TYPE A");
                String serverResponse = readFromServer();
                if ("+".equals(serverResponse.substring(0, 1))){
                    sendMode = "A";
                    System.out.println(serverResponse);
                } else {
                    System.out.println(serverResponse);
                    System.out.println("STOR canceled.");
                    return;
                }
            }
            
            sendToServer("STOR " + commandArgs[1] + " " + resp);
            String serverResponse = readFromServer();
            
            if ("+".equals(serverResponse.substring(0,1))){
                System.out.println(serverResponse + ". Sending SIZE " + file.length());
                sendToServer("SIZE " + file.length());
                serverResponse = readFromServer();
                System.out.println(serverResponse);
                
                if ("+".equals(serverResponse.substring(0,1))){
                    if (DEBUG) System.out.println("Sending file...");

                    byte[] bytes = new byte[(int) file.length()];

                    try {
                        // System.out.println(sendMode);
                        if ("A".equals(sendMode)){
                            // Read and send by byte
                            try (BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(file))) {
                                outToServer.flush();
                                // Read and send by byte
                                int p = 0;
                                while ((p = bufferedStream.read(bytes)) >= 0) {
                                    outToServer.write(bytes, 0, p);
                                }
                                bufferedStream.close();
                                outToServer.flush();
                            } catch (IOException e){
                                socket.close();
                            }
                        } else {
                            try (FileInputStream fileStream = new FileInputStream(file)) {
                                binToServer.flush();
                                int e;
                                while ((e = fileStream.read()) >= 0) {
                                  //System.out.println("Writing: " + e);
                                  binToServer.write(e);
                                }
                                fileStream.close();
                                binToServer.flush();
                            } catch (IOException e){
                                socket.close();
                            }
                        }
                        serverResponse = readFromServer();
                        System.out.println(serverResponse);
                    } catch (Exception e) {
                        if (DEBUG) e.printStackTrace();
                    }
                } else {
                    System.out.println(serverResponse);
                }
            } else {
                System.out.println(serverResponse);
            }
        }
    }

    private static boolean isBinary(File file){
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            int size = in.available();
            if(size > 32) size = 32;
            byte[] data = new byte[size];
            in.read(data);
            in.close();

            int ascii = 0;
            int other = 0;

            for(int i = 0; i < data.length; i++) {
                byte b = data[i];
                if( b < 0x09 ) return true;

                if( b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D ) ascii++;
                else if( b >= 0x20  &&  b <= 0x7E ) ascii++;
                else other++;
            }

            if( other == 0 ) return false;

            return 100 * other / (ascii + other) > 95;
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(Instance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException io){
            
        }
        return false;
    }
    
    private static String readFromServer() {
        String text = "";
        int c = 0;

        while (true){
            try {
                c = inFromServer.read();
                if ((char) c == '\0' && text.length() > 0) {
                    break;
                }
            } catch (IOException lineErr) {
                // catch empty strings, but do nothing
            }
            if (c != '\0') text = text + (char) c;
        }
        if (DEBUG) System.out.println("IN: " + text);
        return text;
    }
    
    private static void sendToServer(String text) {
        try {
            if (DEBUG) System.out.println("OUT: " + text + "\0");
            outToServer.writeBytes(text + "\0");
        } catch (IOException i) {
            try {
                socket.close();
            } catch (IOException e){ }
        }
    }
}
