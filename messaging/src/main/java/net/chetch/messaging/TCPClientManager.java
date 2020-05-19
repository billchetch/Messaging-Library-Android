package net.chetch.messaging;

public class TCPClientManager extends ClientManager<TCPClient> {

    public static TCPClient connect(String connectionString, String clientName) throws Exception{
        if(instance == null) {
            instance = new TCPClientManager();
        }

        return (TCPClient)instance.connect(connectionString, clientName, 10000);
    }

    public static TCPClient getClient(String idOrName){
        if(instance != null){
            return (TCPClient)instance.getConnection(idOrName);
        }
        return null;
    }

    private static TCPClientManager instance = null;

    public TCPClientManager(){
        //empty
        super(TCPClient.class);
    }
}
