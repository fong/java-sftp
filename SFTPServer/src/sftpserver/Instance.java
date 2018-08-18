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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tofupudding
 */
public class Instance extends Thread{
    
    protected Socket socket;
    
    boolean running = true;

    String mode;
    String sendMode = "A";
    String list;
    
    private static final File root = FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile();
    public static String restrictedDirectory = "/";
    public static String directory = "/";
    public static boolean cdirRestricted = false;
    
    protected static Auth auth;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    
    DataInputStream binFromClient;
    DataOutputStream binToClient;
    
    String[] clientCmd; 
    String capitalizedSentence;
    
    String dirpath = "";
    String filepath = "";
    
    boolean tobe = false;
    boolean retr = false;
    String storMode = "";
    long fileLength;
        
    Instance(Socket socket, String authFile){
        this.socket = socket;
        Instance.auth = new Auth(authFile);
    }
    
    @Override
    public void run(){
        
        try {
            socket.setReuseAddress(true);
            binToClient = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            binFromClient = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

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
                    mode(clientCmd, socket);
                }
            } catch (Exception e){
                //e.printStackTrace();
                //break;
            }
        }
        System.out.println("Closed Thread");
    }
    
    public void mode(String[] commandArgs, Socket socket) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"

//        if (!"USER".equals(commandArgs[1]) || !"ACCT".equals(commandArgs[1]) || !"PASS".equals(commandArgs[1])){
//            if (!Auth.userVerification && !Auth.accountVerification && !Auth.passwordVerification){
//                return "-Not Logged In";
//            }
//        }
        
        switch (commandArgs[0]) {
            case "USER":
                sendToClient(auth.user(commandArgs[1]));
                break;
            case "ACCT":                  
                sendToClient(auth.acct(commandArgs[1]));
                break;
            case "PASS":
                sendToClient(auth.pass(commandArgs[1]));
                break;
            case "TYPE":
                if (auth.verified()){
                    type(commandArgs[1]);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "LIST":
                if (auth.verified()){
                    list(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "CDIR":
                if (auth.verified()){
                    cdir(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "KILL":
                if (auth.verified()){
                    kill(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "NAME":
                if (auth.verified()){
                    name(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "TOBE":
                if (auth.verified()){
                    tobe(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "RETR":
                if (auth.verified()){
                    retr(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "SEND":
                if (auth.verified()){
                    send();
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "STOP":
                if (auth.verified()){
                    stopRetr();
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            case "STOR":
                if (auth.verified()){
                    stor(commandArgs);
                } else {
                    sendToClient("-Cannot use " + commandArgs[0] + ". Not logged in");
                }
                break;
            default:
                //if (commandArgs[0].length() == 4) sendToClient("COMMAND ERROR: Server recieved " + Arrays.toString(commandArgs));
                break;
        }
    }
    
    // TYPE { A | B | C }        
    public void type(String inMode){
        if (null == inMode){
            sendToClient("-Type not valid");
        } else switch (inMode) {
            case "A":
                sendMode = "A";
                sendToClient("+Using Ascii mode");
                break;
            case "B":
                sendMode = "B";
                sendToClient("+Using Binary mode");
                break;
            case "C":
                sendMode = "C";
                sendToClient("+Using Continuous mode");
                break;
            default:
                sendToClient("-Type not valid");
                break;
        }
    }
    
    // LIST { F | V } directory-path
    public void list(String[] args) throws IOException{
        
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
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root.toString() + directory + listDirectory))){
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
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root.toString() + directory + listDirectory))){
                response.append("+").append(directory).append("\r\n");
                response.append(String.format("%-68s%-4s%-10s%-21s%-30s", "|Name", "|R/W","|Size", "|Date", "|Owner")).append("|\r\n");
                
                String line = "";
                for (int w = 0; w <= 133; w++){
                    line = ((w == 0 || w == 68 || w == 72 || w == 82 || w == 103 || w == 133) ? (line += "|") : (line += "-"));
                }
                line += "\r\n";
                response.append(line);
                
                for (Path filePath: stream) {
                    File file = new File(filePath.toString());
                    String rw = "";
                    String fileType = "";
                           
                    if (file.isDirectory()){
                        fileType = "DIR";
                        nDirectories++;
                    }
                    if (file.isFile()) {
                        if (isBinary(file)){
                            fileType = "BIN";
                        } else {
                            fileType = "ASC";
                        }
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
                    fileList += String.format("%-4s", fileType);
                    fileList += String.format("%-4s", "|" + rw);
                    fileList += "|" + String.format("%9s", file.length()/1000 + " kB");
                    fileList += String.format("%-21s", "|" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(file.lastModified())));
                    fileList += String.format("%-30s", "|" + owner);
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
    }
    
    public void cdir(String[] args) throws Exception{
        String listDirectory = "";
        if (args.length > 2){
            String response = "";
            for (int i = 1; i < args.length; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            listDirectory = "/" + response;
        } else if (args.length == 2){
            listDirectory = "/" + args[1];
        } else {
            listDirectory = "/";
        }

        File file = new File(root.toString() + listDirectory);
        if (!file.isDirectory()){
            sendToClient("-Can't connect to directory because: " + listDirectory + " is not a directory.");
            return;
        }
        
        file = new File(root.toString() + listDirectory + "/.restrict");
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
            sendToClient("+directory ok, send account/password");
        } else {
            directory = ("/".equals(listDirectory)) ? ("/") : (listDirectory);
            sendToClient("!Changed working dir to " + listDirectory);
        }
    }
    
    public void kill(String[] args) throws Exception {
        boolean passRestriction;
        if (typeCheck(args)){
            passRestriction = verify(args);
        } else {
            return;
        }
        
        if (passRestriction){
            Path fileToDelete = new File(root.toString() + directory + filepath).toPath();
            // Delete the file
            try {
                Files.delete(fileToDelete);
                sendToClient("+" + fileToDelete.getFileName() + " deleted");
            } catch (NoSuchFileException x) {
                sendToClient("-Not deleted because file does not exist in the directory");
            } catch (IOException x) {
                sendToClient("-Not deleted because of IO error. It may be protected.");
            }
        } else {
            sendToClient("-Not deleted because of folder access privileges");
        }
    }
    
    public void name(String[] args) throws Exception {
        boolean passRestriction;
        
        if (!tobe){
            if (typeCheck(args) && verify(args)){
                tobe = true;
                sendToClient("+File exists. Send TOBE <new-name> command.");
            } else if (!typeCheck(args) && verify(args)){
                tobe = false;
                sendToClient("-Can't find " + directory + filepath);
            } else if (typeCheck(args) && !verify(args)){
                tobe = false;
                sendToClient("-File has resticted access " + directory + filepath);
            } else {
                tobe = false;
                sendToClient("-Can't find file and folder has restricted access");
            }
        } else {
            sendToClient("+File exists, awaiting TOBE command.");
        }
    }
    
    public void tobe(String[] args) throws Exception {
        if (tobe){
            File newName = new File(root.toString() + directory + "/" + args[1]);
            File oldName = new File(root.toString() + directory + filepath);
            if (newName.isFile()) {
                sendToClient("-File wasn't renamed because it already exists.");
                return;
            }
            
            String filename = oldName.toString();
            oldName.renameTo(newName);
            tobe = false;
            sendToClient("+" + filename + " renamed to " + newName.getName());
        } else {
            sendToClient("-No file selected");
        }
    }
    
    public boolean done() throws Exception {
        sendToClient("+Closing connection...\n");
        socket.close();
        return false;
    }
    
    public void retr(String[] args) throws Exception {
        if (typeCheck(args) && verify(args)){
            File file = new File(root.toString() + directory + filepath);
            sendToClient(Long.toString(file.length()));
            retr = true;
            fileLength = file.length();
        } else if (typeCheck(args) && !verify(args)){
            sendToClient("-You are not permitted to access this folder");
        } else {
            sendToClient("-File doesn't exist");
        }
    }
    
    public void send(){
        if (!retr) {
            sendToClient("-No File Selected");
        } else {
            byte[] bytes = new byte[(int) fileLength];

            try {
                System.out.println(sendMode);
                File file = new File(root.toString() + directory + filepath);
                if ("A".equals(sendMode)){
                    // Read and send by byte
                    try (BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(file))) {
                        outToClient.flush();
                        // Read and send by byte
                        int p = 0;
                        while ((p = bufferedStream.read(bytes)) >= 0) {
                            outToClient.write(bytes, 0, p);
                        }
                        bufferedStream.close();
                        outToClient.flush();
                    } catch (IOException e){
                        socket.close();
                    }
                } else {
                    try (FileInputStream fileStream = new FileInputStream(file)) {
                        binToClient.flush();
                        int e;
                        while ((e = fileStream.read()) >= 0) {
                          //System.out.println("Writing: " + e);
                          binToClient.write(e);
                        }
                        fileStream.close();
                        binToClient.flush();
                    } catch (IOException e){
                        socket.close();
                    }
                }     
                
                retr = false;
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }
    
    public void stopRetr(){
        if (retr) {
            retr = false;
            sendToClient("+ok, RETR aborted");
        } else {
            sendToClient("-no RETR started. Nothing to stop");
        }      
    }
    
    public void stor(String[] args) throws Exception{
        if (args.length > 3){
            String response = "";
            for (int i = 2; i < args.length-1; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            dirpath = "/" + response; 
            String[] t = args[args.length-1].split("/");
            dirpath += t[0];
            filepath = dirpath + "/" + t[1];
        } else {
            dirpath = "/";
            filepath = "/" + args[2];
        }
        
        File dir = new File(root.toString() + directory + dirpath);
        File file = new File(root.toString() + directory + filepath);
        
        if (!dir.isDirectory()){
            sendToClient("-PATH ERROR: " + directory + dirpath + " is not a directory.");
            return;
        }
        
        if (!verify(args)){
            sendToClient("-ACCESS ERROR: You do not have permissions to store in this folder.");
            return;
        }

        switch (args[1]){
            case "NEW":
                if (file.isFile()){
                    storMode = "NEW";
                    sendToClient("+File exists, will create new generation of file");
                } else {
                    storMode = "CREATE";
                    sendToClient("+File does not exist, will create new file");
                }
                break;
            case "OLD":
                if (file.isFile()){
                    storMode = "OLD";
                    sendToClient("+Will write over old file");
                } else {
                    storMode = "CREATE";
                    sendToClient("+Will create new file");
                }
                break;
            case "APP":
                if (file.isFile()){
                    storMode = "APP";
                    sendToClient("+Will append to file");
                } else {
                    storMode = "CREATE";
                    sendToClient("+Will create file");
                }
                break;
            default:
                sendToClient("-Invalid Mode Argument. Received " + args[1] + ". Require: STOR { NEW | OLD | APP } file-spec");
                return;
        }
        
        String[] resp = readFromClient().split(" ");
        Integer fileSize;
        
        OUTER:
        while (true) {
            if (null == resp[0]) {
                sendToClient("-Invalid Client Response. Awaiting SIZE <number-of-bytes-in-file>. Send STOP to stop transfer.");
            } else {
                switch (resp[0]) {
                    case "SIZE":
                        try {
                            fileSize = Integer.parseInt(resp[1]);
                            System.out.println("File: " + fileSize + "/Directory: " + dir.getFreeSpace());
                            if (dir.getFreeSpace() > fileSize) {
                                sendToClient("+ok, waiting for file");
                                break OUTER;
                            } else {
                                sendToClient("-Not enough room, don't send it");
                                return;
                            }
                        }catch (NumberFormatException e){
                            sendToClient("-Invalid SIZE Argument. Could not convert " + resp[1] + " to a number.");
                        }
                    case "STOP":
                        storMode = "";
                        sendToClient("-Stopping transfer");
                        return;
                    default:
                        sendToClient("-Invalid Client Response. Awaiting SIZE <number-of-bytes-in-file>. Send STOP to stop transfer.");
                        break;
                }
            }
        }
        //find a different filename to save file as
        System.out.println(storMode);
        if ("NEW".equals(storMode)){
            while (file.isFile()) {
                System.out.println("Searching new Filename");
                String[] filename = filepath.split("\\.");
                System.out.println(Arrays.toString(filename));
                SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
                filename[0] = filename[0] + "-" + DATE_FORMAT.format(new Date());
                System.out.println(filename[0] + "." + filename[1]);
                filepath = filename[0] + "." + filename[1];
                file = new File(root.toString() + directory + filename[0] + "." + filename[1]);
            };
            System.out.println("???");
        }
        
        receiveFile(fileSize);
    }
    
    private void receiveFile(Integer fileSize){
        try {
            File file = new File(root.toString() + directory + filepath);
            //byte[] b = new byte[fileSize];
            
            if ("A".equals(sendMode)) {
                try (BufferedOutputStream bufferedStream = new BufferedOutputStream(new FileOutputStream(file, ("APP".equals(storMode))?(false):(true)))) {
                    for (int i = 0; i < fileSize; i++) {
                        bufferedStream.write(inFromClient.read());
                    }
                    System.out.println("Closing ASCII File");
                    bufferedStream.flush();
                    bufferedStream.close();
                } catch (IOException e){
                    socket.close();
                }
            } else {
                try (FileOutputStream fileStream = new FileOutputStream(file, ("APP".equals(storMode))?(false):(true))) {
                    int e;
                    while((e = binFromClient.read()) !=1) {
                        fileStream.write(e);
                    }
                    System.out.println("Closing Binary File");
                    fileStream.flush();
                    fileStream.close();
                } catch (IOException e){
                    socket.close();
                }
            }
            sendToClient("+Saved " + filepath);
        } catch (FileNotFoundException f){
            System.out.println("-Couldn't save because Local FTP directory does not exist.");
        } catch (IOException i){
            System.out.println("-Couldn't save because you do not have permission to write to directory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isBinary(File file){
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            int size = in.available();
            if(size > 1024) size = 1024;
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
    
    private String readFromClient() {
        String text = "";
        int c = 0;

        while (true){
            try {
                c = inFromClient.read();
            } catch (IOException e) {
                try {
                    socket.close();
                    break;
                } catch (IOException s){
                    System.out.println("Socket could not be closed");
                }
            }
            if ((char) c == '\0') {
                break;
            }
            if (c != '\0') text = text + (char) c;
        }
        //System.out.println(text);
        return text;
    }
    
    private void sendToClient(String text) {
        try {
            System.out.println("OUT: " + text + "\0");
            outToClient.writeBytes(text + "\0");
        } catch (IOException lineErr) {
            try {
                // catch empty strings, but do nothing
                socket.close();
            } catch (IOException ex) {
            }
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
        
        File dir = new File(root.toString() + directory + dirpath);
        File file = new File(root.toString() + directory + filepath);
        
        if (!dir.isDirectory()){
            sendToClient("-PATH ERROR: " + directory + dirpath + " is not a directory.");
            return false;
        }
        if (!file.isFile()){
            sendToClient("-PATH ERROR: " + directory + filepath + " is not a file.");
            return false;
        }
        return true;
    }
    
    private boolean verify(String[] args) throws Exception{        
        System.out.println(root.toString() + directory + dirpath + "/.restrict");
        File file = new File(root.toString() + directory + dirpath + "/.restrict");
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
