/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
        
    public static void main(String[] args) throws Exception{
        // TODO code application logic here
        //SFTPClient client = new SFTPClient();
        
        // USER=1, ACCT=2, PASS=3, TYPE=4, LIST=5, CDIR=6, KILL=7, NAME=8, DONE=9, RETR=10, STOR=11
        sftpCommands = new String[]{"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"};
        
        if (args.length == 2){
            try {
                ip = args[0];
                port = Integer.parseInt(args[1]);
                System.out.println("Client will connect to " + ip + " port " + port);
                        
                while(true){
                    System.out.print("> ");
                    String[] commandArgs = selectMode();
                    enterMode(commandArgs);
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
            }
        }
        
        if (!noCommand){
            System.out.println("INPUT ERROR: Command not recognised");
            System.out.println("Commands available: "
                    + "\"USER\", \"ACCT\", \"PASS\", \"TYPE\", \"LIST\","
                    + "\"CDIR\", \"KILL\", \"NAME\", \"DONE\", \"RETR\", \"STOR\"");
        }        
        return commands;
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
                auth("AUTH",commandArgs);
                break;
            case "TYPE":
                if (validAuth){
                    type(commandArgs);
                } else {
                    System.out.println("AUTH ERROR: Not logged in");
                }
                break;
            case "LIST":
                break;
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
            try (Socket clientSocket = new Socket(ip, port)) {
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                
                outToServer.writeBytes(mode + " " + commandArgs[1] + '\n');
                String serverResponse = inFromServer.readLine();
                
                switch (serverResponse.substring(0, 1)) {
                    case "!":
                        validAuth = true;
                        System.out.println(serverResponse.substring(1, serverResponse.length()));
                        break;
                    case "+":
                    case "-":
                        System.out.println(serverResponse.substring(1, serverResponse.length()));
                        break;
                    default:
                        System.out.println("SERVER ERROR: Unknown response. Server returned: " + serverResponse);
                        break;
                }
            } 
        }
    }
    
    public static void type(String[] commandArgs) throws Exception{
        if ("A".equals(commandArgs[1]) || "B".equals(commandArgs[1]) || "C".equals(commandArgs[1])){
            try (Socket clientSocket = new Socket(ip, port)) {
                DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                
                outToServer.writeBytes(mode + " " + commandArgs[1] + '\n');
                String serverResponse = inFromServer.readLine();
                
                switch (serverResponse.substring(0, 1)) {
                    case "+":
                    case "-":
                        System.out.println(serverResponse.substring(1, serverResponse.length()));
                        break;
                    default:
                        System.out.println("SERVER ERROR: Unknown response. Server returned: " + serverResponse);
                        break;
                }
            } 
        } else {
            System.out.println("ARG ERROR: Invalid arguments. Command format: TYPE { A | B | C }");
        }   
    }
}