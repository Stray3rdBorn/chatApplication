package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;



public class ServerThread extends Thread {
 
    Socket socketOfServer;      
    BufferedWriter bw;
    BufferedReader br;
    String clientName, clientPass, clientRoom;
    public static Hashtable<String, ServerThread> listUser = new Hashtable<>();
    
    public static final String NICKNAME_EXIST = "This nickname is already login in another place! Please using another nickname";
    public static final String NICKNAME_VALID = "This nickname is OK";
    public static final String NICKNAME_INVALID = "Nickname or password is incorrect";
    public static final String SIGNUP_SUCCESS = "Sign up successful!";
    public static final String ACCOUNT_EXIST = "This nickname has been used! Please use another nickname!";
    
    public JTextArea taServer;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    
    StringTokenizer tokenizer;
    private final int BUFFER_SIZE = 1024;
    
    String senderName, receiverName;    //Names of the two users exchanging info
    static Socket senderSocket, receiverSocket;    
    
    
    UserDatabase userDB;
    
    static boolean isBusy = false;     // used to check whether the server is sending and receiving files or not
    
    public ServerThread(Socket socketOfServer) {
        this.socketOfServer = socketOfServer;
        this.bw = null;
        this.br = null;
        
        clientName = "";
        clientPass = "";
        clientRoom = "";
        
        userDB = new UserDatabase();
        userDB.connect();
    }
    
    public void appendMessage(String message) {
        taServer.append(message);
        taServer.setCaretPosition(taServer.getText().length() - 1);     //Set the cursor position immediately after the inserted text
    }
    
    public String recieveFromClient() {
        try {
            return br.readLine();
        } catch (IOException ex) {
            System.out.println(clientName+" is disconnected!");
        }
        return null;
    }
    
    public void sendToClient(String response) {     //only send messages to clients associated with this thread
        try {
            bw.write(response);
            bw.newLine();
            bw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendToSpecificClient(ServerThread socketOfClient, String response) {     //Only send messages to specific clients
        try {
            BufferedWriter writer = socketOfClient.bw;
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendToSpecificClient(Socket socket, String response) {     //Only send messages to specific clients
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void notifyToAllUsers(String message) {
        
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;
        BufferedWriter writer;
        
        while(clients.hasMoreElements()) {
            st = clients.nextElement();
            writer = st.bw;

            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void notifyToUsersInRoom(String message) {
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;
        BufferedWriter writer;
        
        while(clients.hasMoreElements()) {
            st = clients.nextElement();
            if(st.clientRoom.equals(this.clientRoom)) {     //Send messages to people (st.clientRoom) whose room matches the sender's room (this.clientRoom)
                writer = st.bw;

                try {
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public void notifyToUsersInRoom(String room, String message) {      //send a message to the room
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;
        BufferedWriter writer;
        
        while(clients.hasMoreElements()) {
            st = clients.nextElement();
            if(st.clientRoom.equals(room)) {
                writer = st.bw;

                try {
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public void closeServerThread() {
        try {
            br.close();
            bw.close();
            socketOfServer.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getAllUsers() {
        StringBuffer kq = new StringBuffer();
        String temp = null;
        
        Enumeration<String> keys = listUser.keys();
        if(keys.hasMoreElements()) {
            String str = keys.nextElement();
            kq.append(str);
        }
        
        while(keys.hasMoreElements()) {
            temp = keys.nextElement();
            kq.append("|").append(temp);
        }
        
        return kq.toString();
    }
    
    public String getUsersThisRoom() {
        StringBuffer kq = new StringBuffer();
        String temp = null;
        ServerThread st;
        Enumeration<String> keys = listUser.keys();
        
        while(keys.hasMoreElements()) {
            temp = keys.nextElement();
            st = listUser.get(temp);
            if(st.clientRoom.equals(this.clientRoom))  kq.append("|").append(temp);
        }
        
        if(kq.equals("")) return "|";
        return kq.toString();   
    }
    
    public String getUsersAtRoom(String room) {
        StringBuffer kq = new StringBuffer();
        String temp = null;
        ServerThread st;
        Enumeration<String> keys = listUser.keys();
        
        while(keys.hasMoreElements()) {
            temp = keys.nextElement();
            st = listUser.get(temp);
            if(st.clientRoom.equals(room))  kq.append("|").append(temp);
        }
        
        if(kq.equals("")) return "|";
        return kq.toString();   
    }
    
    public void clientQuit() {
        
        if(clientName != null) {
            
            this.appendMessage("\n["+sdf.format(new Date())+"] Client \""+clientName+"\" is disconnected!");
            listUser.remove(clientName);
            if(listUser.isEmpty()) this.appendMessage("\n["+sdf.format(new Date())+"] No clients are connected to this server right now.\n");
            notifyToAllUsers("CMD_ONLINE_USERS|"+getAllUsers());
            notifyToUsersInRoom("CMD_ONLINE_THIS_ROOM"+getUsersThisRoom());
            notifyToUsersInRoom(clientName+" has left.");
        }
    }
    
    public void changeUserRoom() {      //update the room for the object of this class in listUser
        ServerThread st = listUser.get(this.clientName);
        st.clientRoom = this.clientRoom;
        listUser.put(this.clientName, st);    //st is the serverThread object attached to the client requesting a room change
        
    }
    
    public void removeUserRoom() {
        ServerThread st = listUser.get(this.clientName);
        st.clientRoom = this.clientRoom;
        listUser.put(this.clientName, st);
       
    }
    
    @Override
    public void run() {
        try {
            //Creates incoming and outgoing streams with the client's socket
            bw = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
            
            boolean isUserExist = true;
            String message, sender, receiver, fileName;
            StringBuffer str;
            String cmd, icon;
            while(true) {   //Just wait for the client to send a message and respond
                try {
                    message = recieveFromClient();
                    tokenizer = new StringTokenizer(message, "|");
                    cmd = tokenizer.nextToken();
                    
                    switch (cmd) {
                        case "CMD_CHAT":
                            str = new StringBuffer(message);
                            str = str.delete(0, 9);
                            notifyToUsersInRoom("CMD_CHAT|" + this.clientName+"|"+str.toString());    //this.clientName = name of the client sending the message
                            break;
                            
                        case "CMD_PRIVATECHAT":
                            String privateSender = tokenizer.nextToken();
                            String privateReceiver = tokenizer.nextToken();
                            String messageContent = message.substring(cmd.length()+privateSender.length()+privateReceiver.length()+3, message.length());
                            
                            //ServerThread st_sender = listUser.get(privateSender);
                            ServerThread st_receiver = listUser.get(privateReceiver);
                            //sendToSpecificClient(st_sender, "CMD_PRIVATECHAT|" + privateSender + "|" + messageContent);
                            sendToSpecificClient(st_receiver, "CMD_PRIVATECHAT|" + privateSender + "|" + messageContent);
                            
                            System.out.println("[ServerThread] message = "+messageContent);
                            break;
                            
                        case "CMD_ROOM":
                            clientRoom = tokenizer.nextToken();
                            changeUserRoom();
                            notifyToAllUsers("CMD_ONLINE_USERS|"+getAllUsers());
                            notifyToUsersInRoom("CMD_ONLINE_THIS_ROOM"+getUsersThisRoom());
                            notifyToUsersInRoom(clientName+" has just entered!");
                            break;
                            
                        case "CMD_LEAVE_ROOM":
                            String room = clientRoom;
                            clientRoom = "";    //If clientRoom = null, there will be an error!
                            removeUserRoom();
                            notifyToUsersInRoom(room, "CMD_ONLINE_THIS_ROOM"+getUsersAtRoom(room));
                            notifyToUsersInRoom(room, clientName+" has just left the room!");      
                            
                            break;
                            
                        case "CMD_CHECK_NAME":
                            clientName = tokenizer.nextToken();
                            clientPass = tokenizer.nextToken();
                            isUserExist = listUser.containsKey(clientName);
                            
                            if(isUserExist) {  //nickname already exists, which means someone else is already logged in with that nick
                                sendToClient(NICKNAME_EXIST);
                            }
                            else {  //nickname, no one has logged in yet
                                int kq = userDB.checkUser(clientName, clientPass);
                                if(kq == 1) {
                                    sendToClient(NICKNAME_VALID);
                                    //Then if the name is valid, put that nick in the Hashtable and chat with the client:
                                    this.appendMessage("\n["+sdf.format(new Date())+"] Client \""+clientName+"\" is connecting to the server.");
                                    listUser.put(clientName, this);     //add the name of this object and also add this object to listUser
                                } else sendToClient(NICKNAME_INVALID);
                            }
                            break;
                            
                        case "CMD_SIGN_UP":
                            String name = tokenizer.nextToken();
                            String pass = tokenizer.nextToken();
                            System.out.println("name, pass = "+name+" - "+pass);
                            isUserExist = listUser.containsKey(name);
                            
                            if(isUserExist) {
                                sendToClient(NICKNAME_EXIST);
                            } else {
                                int kq = userDB.insertUser(new User(name, pass));
                                if(kq > 0) {
                                    sendToClient(SIGNUP_SUCCESS);
                                } else sendToClient(ACCOUNT_EXIST);
                            }
                            break;
                            
                        case "CMD_ONLINE_USERS":
                            sendToClient("CMD_ONLINE_USERS|"+getAllUsers());
                            notifyToUsersInRoom("CMD_ONLINE_THIS_ROOM"+getUsersThisRoom());
                            break;
                        
                        case "CMD_SENDFILETOSERVER":    //the sender sends a file to server:
                            sender = tokenizer.nextToken();
                            receiver = tokenizer.nextToken();
                            fileName = tokenizer.nextToken();
                            int len = Integer.valueOf(tokenizer.nextToken());
                            
                            String path = System.getProperty("user.dir") + "\\sendfile\\" +fileName;
                            
                            //InputStream bis = socketOfServer.getInputStream();
                            //OutputStream os = receiverSocket.getOutputStream();   
                            //BufferedInputStream bis = new BufferedInputStream(socketOfServer.getInputStream());
                            //BufferedOutputStream bos = new BufferedOutputStream(fos);
                            //OutputStream fos = new FileOutputStream(path);
                            //BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
                            //BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));

                            BufferedInputStream bis = new BufferedInputStream(socketOfServer.getInputStream());   //get the input stream from the sender
                            FileOutputStream fos = new FileOutputStream(path);   //The output stream is the file that will be saved on the server's hard drive
                            
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int count = -1;
                            while((count = bis.read(buffer)) > 0) {  //How much is read from the sender will be temporarily stored in the buffer array
                                fos.write(buffer, 0, count);         //and then the OS takes the buffer and sends it to the receiver
                            }

                            Thread.sleep(300);
                            bis.close();
                            fos.close();
                            socketOfServer.close();
                            
                            ///notify the sender and receiver that the file has just been sent, then it's their job to download it:
                            ServerThread stSender = listUser.get(sender);        
                            ServerThread stReceiver = listUser.get(receiver);
                            
                            sendToSpecificClient(stSender, "CMD_FILEAVAILABLE|"+fileName+"|"+receiver+"|"+sender);
                            sendToSpecificClient(stReceiver, "CMD_FILEAVAILABLE|"+fileName+"|"+sender+"|"+sender);
                            
                            isBusy = false;
                            break;
                            
                        case "CMD_DOWNLOADFILE":    //server sends file to someone who just pressed download file
                            fileName = tokenizer.nextToken();
                            path = System.getProperty("user.dir") + "\\sendfile\\" + fileName;
                            FileInputStream fis = new FileInputStream(path);
                            BufferedOutputStream bos = new BufferedOutputStream(socketOfServer.getOutputStream());
                            
                            byte []buffer2 = new byte[BUFFER_SIZE];
                            int count2=0;
                            
                            while((count2 = fis.read(buffer2)) > 0) {
                                bos.write(buffer2, 0, count2);    //Continuously send each part of the file to the server
                            }

                            bos.close();
                            fis.close();
                            socketOfServer.close();
                            
                            break;
                            
                        case "CMD_ICON":
                            icon = tokenizer.nextToken();
                            notifyToUsersInRoom("CMD_ICON|"+icon+"|"+this.clientName);
                            break;
                            
                        default:
                            notifyToAllUsers(message);
                            break;
                    }
                    
                } catch (Exception e) {
                    clientQuit();
                    break;
                }
            }
        } catch (IOException ex) {
            clientQuit();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        //this.closeServerThread();
    }
}