package sftpclient;

import java.io.*; 
import java.net.*; 

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
    
    static Socket socket;
    static DataOutputStream outToServer;
    static BufferedReader inFromServer;
    static boolean running = true;

    public static void main(String[] args) throws Exception{
        // TODO code application logic here
        //SFTPClient client = new SFTPClient();
        
        // USER=1, ACCT=2, PASS=3, TYPE=4, LIST=5, CDIR=6, KILL=7, NAME=8, DONE=9, RETR=10, STOR=11
        sftpCommands = new String[]{"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"};
        
        if (args.length == 2){
            ip = args[0];
            port = Integer.parseInt(args[1]);
            
            try{
                socket = new Socket(ip, port);
                outToServer =  new DataOutputStream(socket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));   
                System.out.println("Client connected to " + ip + " port " + port);
                System.out.println(inFromServer.readLine());
                
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
                if (validAuth){
                    type(commandArgs);
                } else {
                    System.out.println("AUTH ERROR: Not logged in");
                }
                break;
            case "LIST":
                if (validAuth){
                    list(commandArgs);
                } else {
                    System.out.println("AUTH ERROR: Not logged in");
                }
                break;
            case "CDIR":
                if (validAuth){
                    cdir(commandArgs);
                } else {
                    System.out.println("AUTH ERROR: Not logged in");
                }
                break;
            case "KILL":
                break;
            case "NAME":
                break;
            case "DONE":
                outToServer.writeBytes("DONE" + '\n');
                System.out.println(inFromServer.readLine());
                running = false;
                break;
            case "RETR":
                break;
            case "STOR":
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
            outToServer.writeBytes(mode + " " + commandArgs[1] + '\n');
            String serverResponse = inFromServer.readLine();

            if ("!".equals(serverResponse.substring(0, 1))) {
                validAuth = true;
                System.out.println(serverResponse);
            } else {
                System.out.println(serverResponse);
            }
        }
    }
    
    public static void type(String[] commandArgs) throws Exception{
        if (commandArgs.length == 2){                        
            outToServer.writeBytes("TYPE " + commandArgs[1] + '\n');
            String serverResponse = inFromServer.readLine();

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
                outToServer.writeBytes("LIST " + commandArgs[1] + '\n');
            } else {
                String resp = " ";
                for (int i = 2; i < commandArgs.length; i++){
                    resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
                }
                outToServer.writeBytes("LIST " + commandArgs[1] + resp + '\n');
            }

            String serverResponse = "";

            while (!"\0".equals(serverResponse)){
                serverResponse = inFromServer.readLine();
                System.out.println(serverResponse);
            };
        } else {
            System.out.println("ARG ERROR: Invalid arguments. Command format: LIST { F | V } directory-path");
        }
    }
    
    public static void cdir(String[] commandArgs) throws Exception {
        String resp = " ";
            for (int i = 1; i < commandArgs.length; i++){
                resp = (i == commandArgs.length-1) ? (resp += commandArgs[i]) : (resp += commandArgs[i] + " ");
            }
        outToServer.writeBytes("CDIR " + resp + '\n');
        System.out.println(inFromServer.readLine());
    }
}
