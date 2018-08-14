package sftpclient;

import java.io.*; 
import java.net.*; 
import java.nio.file.*;

/**
 *
 * @author tofupudding
 */

public class SFTPClient {

    /**
     * @param args the command line arguments
     */
    static String[] sftpCommands;
    static String mode;
    
    static String ip;
    static int port;
    
    static boolean validAuth = false;
    
    static String type;
    static String filename;
    static long fileSize;
    static File ftp = FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile();
    
    static Socket socket;
    static DataOutputStream outToServer;
    static BufferedReader inFromServer;
    static boolean running = true;

    public static void main(String[] args) throws Exception{
        // TODO code application logic here
        //SFTPClient client = new SFTPClient();
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
                outToServer =  new DataOutputStream(socket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));   
                
                System.out.println("Client connected to " + ip + " port " + port);
                System.out.println(readFromServer());
                
                while(running){
                    System.out.print("> ");
                    String[] commandArgs = selectMode();
                    if (commandArgs != null) enterMode(commandArgs);
                } 
            }
            catch (NumberFormatException e) {
               System.out.println("PORT ERROR: Port argument needs to be a number"); 
            } catch (ConnectException e) {
               System.out.println("Connection refused. Server may not be online."); 
            };
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
                System.out.println(sftpCommand);
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
                sendToServer("DONE");
                System.out.println(readFromServer());
                socket.close();
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
//            String serverResponse = readFromServer();
//            try {
//                if ("!".equals(serverResponse.substring(0, 1))) {
//                    validAuth = true;
//                    System.out.println("login valid");
//                    System.out.println(serverResponse);
//                } else {
//                    System.out.println(serverResponse);
//                }
//            } catch (Exception e){
//                e.printStackTrace();
//                System.out.println(serverResponse);
//            }
        }
    }
    
    public static void type(String[] commandArgs) throws Exception{
        if (commandArgs.length == 2){                        
            sendToServer("TYPE " + commandArgs[1]);
            String serverResponse = readFromServer();

            switch (serverResponse.substring(0, 1)) {
                case "+":
                    type = commandArgs[1];
                    System.out.println(serverResponse);
                    break;
                default:
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
            fileSize = Integer.parseInt(readFromServer());
            System.out.println(fileSize);
        }
    }
        
    public static void send(){
        sendToServer("SEND ");
        try {
            System.out.println(fileSize);
            File file = new File(ftp.getPath() + "/" + filename);
            FileOutputStream fileStream = new FileOutputStream(file, false);
            BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream);
            
            for (int i = 0; i < fileSize; i++) {
                bufferedStream.write(inFromServer.read());
            }
            System.out.println("Closing File");  
            bufferedStream.close();
            fileStream.close();
        } catch (FileNotFoundException n){
            System.out.println("ERROR: Local FTP directory does not exist.");
        } catch (Exception e) {
            e.printStackTrace();
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
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
            sendToServer("STOR " + resp);
            System.out.println(readFromServer());
        }
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
        System.out.println("IN: " + text);
        return text;
    }
    
    private static void sendToServer(String text){
        try {
            System.out.println("OUT: " + text + "\0");
            outToServer.writeBytes(text + "\0");
        } catch (IOException lineErr) {
        }
    }
}
