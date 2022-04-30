package net.chetch.messaging;

import android.net.ParseException;
import android.util.Log;

import net.chetch.utilities.SLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPClient extends ClientConnection {

    public String ip;
    public int port;

    Socket socket;

    @Override
    public String getConnectionString(){
        return ip + ":" + port;
    }

    @Override
    public void parseConnectionString(String connectionString) throws Exception{
        String[] parts = connectionString.split(":");
        if(parts.length != 2){
            throw new Exception("Cannot parse " + connectionString);
        }

        ip = parts[0];
        port = Integer.parseInt(parts[1]);
    }

    @Override
    public void parseMessage(Message message) throws Exception{
        if(!message.hasValue("IP")){
            throw new Exception("Parse error: message does not have IP value");
        }
        if(!message.hasValue("Port")){
            throw new Exception("Parse error: message does not have Port value");
        }
        ip = message.getString("IP");
        port = message.getInt("Port");
    }

    @Override
    public void close(){
        super.close();
        if(socket != null){
            try {
                socket.close();
            } catch(Exception e){
                if(SLog.LOG) SLog.e("TCPClient", e.getMessage());
            }
        }
    }

    @Override
    public void connect() throws Exception {
        if(SLog.LOG)SLog.i("TCPClient", id + " connecting...");

        SocketAddress sockaddr = new InetSocketAddress(ip, port);
        socket = new Socket();

        //set state to 'opened' .. this is to indicate that the lisening thread has started
        //and the connection is imminently going to try and 'connect'
        setState(ConnectionState.OPENED);

        socket.connect(sockaddr, connectionTimeout);
        if (!socket.isConnected()) {
            throw new Exception("Socket not friken connected");
        }
        if(SLog.LOG)SLog.i("TCPClient", id + " connected");

        //get the needed input and output streams
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        //now set to connected after having assigned the streams
        setState(ConnectionState.CONNECTED);

        //now hang in until message received or timeout occurred
        do {
            if(SLog.LOG)SLog.i("TCPClient", id + " waiting to receive...");
            receiveMessage();
            if(!socket.isConnected()){
                throw new Exception("Socket disconnected");
            }
        } while(remainConnected);

        if(SLog.LOG)SLog.i("TCPClient", id + " finished.");
    }
}
