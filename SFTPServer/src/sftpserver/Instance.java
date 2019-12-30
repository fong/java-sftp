package sftpserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Instance
 * This a thread of SFTPServer which handles a new socket connection.
 * @author Eugene Fong (efon103)
 */

public class Instance extends Thread{
    
    protected Socket socket;
    
    boolean running = true;

    String mode;                //Client command
    String sendMode = "A";      //File Type, A=ASCII, B=Binary, C=Continuous
    
    // Directory variables invoke
    private static final File root = FileSystems.getDefault().getPath("ftp/").toFile().getAbsoluteFile();
    public static String restrictedDirectory = "/";
    public static String directory = "";
    public static boolean cdirRestricted = false;
    String dirpath = "";
    String filepath = "";

    //User authentication
    protected static Auth auth;
    
    //Data Streams for ASCII and Binary
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    DataInputStream binFromClient;
    DataOutputStream binToClient;
        
    //Flags for TOBE and RETR
    boolean name = false;
    boolean retr = false;
    
    String storMode = "";           // Store Mode (i.e. NEW | OLD | APP)
    long fileLength;                // File length to store
    
    long totalIO = 0;               // total IO transferred counter
        
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
        } catch (Exception e) {}
        
        while(running){
            try {
                String[] clientCmd = readFromClient().split(" ");       //read command from client
                if (clientCmd[0].equals("DONE")){                       //if command is DONE, close thread
                    sendToClient("+Closing connection. A total of " + totalIO/1000 + "kB was transferred.\0");
                    socket.close();
                    running = false;
                    break;
                } else {
                    mode(clientCmd);                            //Otherwise, determine command and run
                }
            } catch (Exception e){}
        }
        if (SFTPServer.DEBUG) System.out.println("Closed Thread");
    }
    
    //mode detects command and routes it the arguments to the correct function
    public void mode(String[] commandArgs) throws Exception{
        //"USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR"
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
                if (auth.verified()){           // Ensure that client is verified for commands
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
                break;
        }
    }
    
    // TYPE { A | B | C } Command     
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
    
    // LIST { F | V } directory-path Command
    public void list(String[] args) throws Exception{
        //Detect directory is there is whitespace (e.g. ftp/Restricted Folder/)
        if (args.length > 2){
            String response = "";
            for (int i = 2; i < args.length; i++){
                 response += args[i];
                 response = (i == (args.length - 1))? (response += ""): (response += " ");
            }
            dirpath = "/" + response;
        }
        
        //Verify that user has permissions to access folder
        if (!verify()){
           dirpath = "";
           sendToClient("-Folder is restricted, you do not have sufficient permissions to view");
           return;
        }
        
        
        if ("F".equals(args[1])){
            //LIST F
            StringBuilder response = new StringBuilder();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root.toString() + directory + dirpath))){
                response.append("+").append(directory).append(dirpath).append("\r\n");
                for (Path filePath: stream) {
                    response.append(filePath.getFileName()).append("\r\n");
                }
                sendToClient(response.toString());
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                if (SFTPServer.DEBUG) System.err.println(x);
                dirpath = "";
                sendToClient("-" + x.toString());
            }
        } else if ("V".equals(args[1])){
            //LIST V
            StringBuilder response = new StringBuilder();
            //Directory Stats
            long totalFileSize = 0;
            int nFiles = 0;
            int nDirectories = 0;
            //Header
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(root.toString() + directory + dirpath))){
                response.append("+").append(directory).append(dirpath).append("\r\n");
                response.append(String.format("%-68s%-4s%-10s%-21s%-30s", "|Name", "|R/W","|Size", "|Date", "|Owner")).append("|\r\n");
                //Separator
                String line = "";
                for (int w = 0; w <= 133; w++){
                    line = ((w == 0 || w == 68 || w == 72 || w == 82 || w == 103 || w == 133) ? (line += "|") : (line += "-"));
                }
                line += "\r\n";
                response.append(line);
                //Files
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
                        if (SFTPServer.DEBUG) e.printStackTrace();
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
                //Footer
                String stats = nFiles + " File(s)\t " +
                                nDirectories + " Dir(s)\t " + totalFileSize/1000 + "kB Total File Size" + "\n";
                response.append(stats);
                sendToClient(response.toString());
            } catch (IOException | DirectoryIteratorException | InvalidPathException x) {
                if (SFTPServer.DEBUG) System.err.println(x);
                sendToClient("-" + x.toString());
            }
        } else {
            sendToClient("-LIST type invalid. LIST F and LIST V available");
        }
        dirpath = "";   //Reset current directory path
    }
    
    // CDIR {directory} Command
    public void cdir(String[] args) throws Exception{
        String listDirectory = "";
        //Detect directory is there is whitespace (e.g. ftp/Restricted Folder/)
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
        // Test if it is a directory
        File file = new File(root.toString() + listDirectory);
        if (!file.isDirectory()){
            sendToClient("-Can't connect to directory because: " + listDirectory + " is not a directory.");
            return;
        }
        
        //check if it is restricted access (e.g. account required)
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
            if (SFTPServer.DEBUG) System.out.println("Unrestricted Folder");
            passRestriction = true;
        } catch (IOException e) {
            if (SFTPServer.DEBUG) System.out.println("IO Exception");
        } finally {
           try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                if (SFTPServer.DEBUG) System.out.println("IO Exception on file close");
            }
        }
        //Block access if not permitted otherwise change directory
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
    
    // KILL {filename} Command
    public void kill(String[] args) throws Exception {
        //NOTE: must keep as separate boolean tc as typeCheck should run only once. Due to a message sending to client when type fails
        boolean tc = typeCheck(args);
        if (tc && verify()){ //check valid location/file and authentication
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
        } else if (tc && !verify()) {   //reject if invalid access
            sendToClient("-Not deleted because of folder access privileges");
        }
    }
    
    // NAME {current-filename} Command
    public void name(String[] args) throws Exception {  
        // Check if NAME has not already been used and waiting for TOBE command
        if (!name){
            //NOTE: must keep as separate boolean tc as typeCheck should run only once. Due to a message sending to client when type fails
            boolean tc = typeCheck(args);
            if (tc && verify()){    //if verified, await TOBE
                name = true;
                sendToClient("+File exists. Send TOBE <new-name> command.");
            } else if (tc && !verify()){    //if not verified, cancel
                name = false;
                sendToClient("-File has resticted access " + directory + filepath);
            } else {
                name = false;
            }
        } else {
            sendToClient("+File exists, awaiting TOBE command.");
        }
    }
    
    // TOBE {new-filename} Command
    public void tobe(String[] args) throws Exception {
        if (name){      //Ensure NAME has been used and permitted
            File newName = new File(root.toString() + directory + "/" + args[1]);
            File oldName = new File(root.toString() + directory + filepath);
            String[] filename = filepath.split("/");    //save old filename
            //Don't change anything if filename already exists, but do not cancel NAME operation
            if (newName.isFile()) {
                sendToClient("-File wasn't renamed because it already exists. Use LIST F/V to check folder and then use TOBE {new-filename} to rename.");
                return;
            }
            oldName.renameTo(newName);
            name = false;
            sendToClient("+" + filename[filename.length - 1] + " renamed to " + newName.getName());
        } else {    //Reject if no previous NAME command used
            sendToClient("-No file selected");
        }
    }
    
    // RETR {filename} Command
    public void retr(String[] args) throws Exception {
        // Check if NAME has not already been used and waiting for TOBE command
        boolean tc = typeCheck(args);
        if (tc && verify()){    //if verified, check if mode matches filetype. If ok, await SEND.
            File file = new File(root.toString() + directory + filepath);
            boolean isBin = isBinary(file);
            if (isBin && "A".equals(sendMode)){
                sendToClient("-File is Binary. Current TYPE is A. Please switch to B or C");
            } else if (!isBin && ("B".equals(sendMode) || "C".equals(sendMode))){
                sendToClient("-File is ASCII. Current TYPE is B or C. Please switch to A");
            } else {
                sendToClient(Long.toString(file.length()));
                retr = true;
                fileLength = file.length();
            }
        } else if (tc && !verify()){
            sendToClient("-You are not permitted to access this folder");
        }
    }
    
    // SEND Command
    public void send(){
        if (!retr) {    //check if retr is activated
            sendToClient("-No File Selected");
        } else {
            byte[] bytes = new byte[(int) fileLength];

            try {
                if (SFTPServer.DEBUG) System.out.println(sendMode);
                File file = new File(root.toString() + directory + filepath);
                if ("A".equals(sendMode)){      //ASCII
                    try (BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(file))) {
                        outToClient.flush();
                        // Read and send by byte
                        int p = 0;
                        while ((p = bufferedStream.read(bytes)) >= 0) {
                            //if (SFTPServer.DEBUG) System.out.println("Writing: "+ p); //WARNING PrintLning will take a very long time for large files
                            outToClient.write(bytes, 0, p);
                        }
                        bufferedStream.close();
                        outToClient.flush();
                        totalIO += fileLength;  //add to transaction counter
                    } catch (IOException e){
                        socket.close();
                        running = false;
                    }
                } else {              //Binary or Continuous
                    try (FileInputStream fileStream = new FileInputStream(file)) {
                        binToClient.flush();
                        int e;
                        while ((e = fileStream.read()) >= 0) {
                          //if (SFTPServer.DEBUG) System.out.println("Writing: "+ e); //WARNING PrintLning will take a very long time for large files
                          binToClient.write(e);
                        }
                        fileStream.close();
                        binToClient.flush();
                        totalIO += fileLength; //add to transaction counter
                    } catch (IOException e){
                        socket.close();
                        running = false;
                    }
                }
                retr = false;
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }
    
    // stopRetr resets retr boolean
    public void stopRetr(){
        if (retr) {
            retr = false;
            sendToClient("+ok, RETR aborted");
        } else {
            sendToClient("-no RETR started. Nothing to stop");
        }      
    }
    
    // STOR { NEW| OLD | APP } {filename} Command
    public void stor(String[] args) throws Exception{
        if (args.length > 3){   //make sure commands are at least 3
            //Detect directory is there is whitespace (e.g. ftp/Restricted Folder/)
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
        //check if directory is real
        if (!dir.isDirectory()){
            sendToClient("-PATH ERROR: " + directory + dirpath + " is not a directory.");
            return;
        }
        //check if client has permission to access folder
        if (!verify()){
            sendToClient("-ACCESS ERROR: You do not have permissions to store in this folder.");
            return;
        }
        //set store mode between new generation (NEW), overwrite (OLD), and append (APP)
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
        
        // Await for SIZE or STOP
        OUTER:
        while (true) {
            if (null == resp[0]) {
                sendToClient("-Invalid Client Response. Awaiting SIZE <number-of-bytes-in-file>. Send STOP to stop transfer.");
            } else {
                switch (resp[0]) {
                    case "SIZE":
                        try {
                            fileSize = Integer.parseInt(resp[1]);
                            if (SFTPServer.DEBUG) System.out.println("File: " + fileSize + "/Directory: " + dir.getFreeSpace());
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
        if ("NEW".equals(storMode)){
            while (file.isFile()) {
                String[] filename = filepath.split("\\.");
                SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
                filename[0] = filename[0] + "-" + DATE_FORMAT.format(new Date());
                filepath = filename[0] + "." + filename[1];
                file = new File(root.toString() + directory + filename[0] + "." + filename[1]);
            };
        }
        receiveFile(fileSize);
    }
    
    /*
     * -------------------------------------------------------------------------
     * HELPER FUNCTIONS
     */
    
    //receiveFile receives an incoming file depending on file transfer type
    private void receiveFile(Integer fileSize){
        try {
            File file = new File(root.toString() + directory + filepath);
            Long timeout = new Date().getTime() + (fileSize/8 + 8)*1000; //offset if filesize is 1 byte
            if ("A".equals(sendMode)) {
                try (BufferedOutputStream bufferedStream = new BufferedOutputStream(new FileOutputStream(file, "APP".equals(storMode)))) {
                    for (int i = 0; i < fileSize; i++) {
                        if (new Date().getTime() >= timeout){   //close stream if file transfer times out
                            System.out.println("Transfer taking too long. Timed out after " + (fileSize/8 + 8)/60000 + " seconds.");
                            bufferedStream.flush();
                            bufferedStream.close();
                            return;
                        }
                        bufferedStream.write(inFromClient.read());
                    }
                    bufferedStream.flush();
                    bufferedStream.close();
                    totalIO += fileSize; //add to transaction counter
                }
            } else {
                try (FileOutputStream fileStream = new FileOutputStream(file, "APP".equals(storMode))) {
                    int e;
                    int i = 0;
                    byte[] bytes = new byte[(int) fileSize];
                    while (i < fileSize) {
                        e = binFromClient.read(bytes);
                        if (new Date().getTime() >= timeout){   //close stream if file transfer times out
                            System.out.println("Transfer taking too long. Timed out after " + (fileSize/8 + 8)/60000 + " seconds.");
                            fileStream.flush();
                            fileStream.close();
                            return;
                        }
                        fileStream.write(bytes, 0, e);
                        i+=e;
                    }
                    fileStream.flush();
                    fileStream.close();
                    totalIO += fileSize; //add to transaction counter
                }
            }
            sendToClient("+Saved " + filepath);
        } catch (FileNotFoundException f){
            if (SFTPServer.DEBUG) System.out.println("-Couldn't save because Local FTP directory does not exist.");
        } catch (IOException i){
            if (SFTPServer.DEBUG) System.out.println("-Couldn't save because you do not have permission to write to directory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //isBinary compares the amount of ASCII character to non-ASCII characters to determine if the file is binary
    private boolean isBinary(File file){
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            int size = in.available();
            if(size > 32) size = 32;    //check only the first few bytes
            byte[] data = new byte[size];
            in.read(data);
            in.close();

            int ascii = 0;
            int binary = 0;
            
            for(int i = 0; i < data.length; i++) {
                byte b = data[i];
                if( b < 0x09 ) return true;
                
                if( b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D ) ascii++;
                else if( b >= 0x20  &&  b <= 0x7E ) ascii++;
                else binary++;
            }

            if( binary == 0 ) return false;

            return 100 * binary / (ascii + binary) > 95;
        } catch (FileNotFoundException ex) {
        } catch (IOException io){}
        return false;
    }
    
    //readFromClient reads ASCII messages from client
    private String readFromClient() {
        String text = "";
        int c = 0;

        while (true){
            try {
                c = inFromClient.read();
            } catch (IOException e) {
                try {   // close thread and socket if server is closed
                    socket.close();
                    running = false;
                    break;
                } catch (IOException s){
                    if (SFTPServer.DEBUG) System.out.println("Socket could not be closed");
                }
            }
            if ((char) c == '\0' && text.length() > 0) break;                    //if null, terminate
            if ((char) c != '\0') text = text + (char) c;   //otherwise add to text buffer
        }
        if (SFTPServer.DEBUG) System.out.println("IN: " + text);
        return text;
    }
    
    //sendToClient sends ASCII messages to client
    private void sendToClient(String text) {
        try {
            if (SFTPServer.DEBUG) System.out.println("OUT: " + text + "\0");
            outToClient.writeBytes(text + "\0");
        } catch (IOException lineErr) {
            try {   // if IOError close socket as client is already closed
                socket.close();
                running = false;
            } catch (IOException ex) {}
        }
    }
    
    // typeCheck checks whether both directory and file is valid
    private boolean typeCheck(String[] args) throws Exception{
        //Detect directory is there is whitespace (e.g. ftp/Restricted Folder/)
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
    
    // verify checks if there is a .restrict file and if the user credentials match
    private boolean verify() throws Exception{        
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
                // loop all valid accounts and password combinations
                for (String restrictedAccount : restrictedAccounts){
                    //User account and password match .restrict file 
                    if (Auth.account.equals(restrictedAccount) && Auth.password.equals(restrictedPassword)){
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            if (SFTPServer.DEBUG) System.out.println("Unrestricted Folder");
            return true;
        } catch (IOException e) {
            if (SFTPServer.DEBUG) System.out.println("IO Exception");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                if (SFTPServer.DEBUG) System.out.println("IO Exception on file close");
            }
        }
        return false;
    }
}
