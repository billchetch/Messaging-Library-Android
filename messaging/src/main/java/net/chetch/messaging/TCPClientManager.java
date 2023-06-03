package net.chetch.messaging;

public class TCPClientManager extends ClientManager<TCPClient> {

    public static TCPClient connect(String connectionString, String clientName, String authToken) throws Exception{
        if(instance == null) {
            instance = new TCPClientManager();
        }

        return (TCPClient)instance.connect(connectionString, clientName, 10000, authToken);
    }

    public static TCPClient connect(String connectionString, String clientName) throws Exception {
        return connect(connectionString, clientName, null);
    }

    public static TCPClient getClient(String idOrName){
        if(instance != null){
            return (TCPClient)instance.getConnection(idOrName);
        }
        return null;
    }

    public static void pause(){
        if(instance != null)instance.pauseKeepAlive();
    }

    public static void resume(){
        if(instance != null)instance.resumeKeepAlive();
    }
    
    private static TCPClientManager instance = null;

    public TCPClientManager(){
        //empty
        super(TCPClient.class);
    }
}
