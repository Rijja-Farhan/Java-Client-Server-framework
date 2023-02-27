import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


import java.io.*;


public abstract class AbstractServer implements Runnable  {



//INSTANCE VARIABLES /SERVER INTERNALS
    private ServerSocket serverSocket=null; //server socket is java class that is used to provide system independent implementation of server side
    private Thread connectionListener; //the connection listener thread
    private int port;
    private int backlog=10;//max number of clients that can be waiting to connect

   
    private int timeout=500; 


    private ThreadGroup clientThreadGroup;


    private boolean readyToStop=true;

    

    //CONSTRUCTOR
    public AbstractServer(int port)
    {
        this.port=port;

        this.clientThreadGroup= new ThreadGroup("ConnectionToClient threads"){
            public void uncaughtException(Thread thread,Throwable exception)
            {
                //all uncaught exceptions will be sent to client exception hook
                clientException((ConnectionToClient)thread,exception);
            }
        };
    }
    

    //INSTANCE METHODS

  
    final public void listen() throws IOException
    {

        if(isListening()!=true)
        {
            if(serverSocket==null)
          {
            serverSocket=  new ServerSocket(getPort(),backlog);
          }


         serverSocket.setSoTimeout(timeout);
         readyToStop = false;
         connectionListener= new Thread(this);
         connectionListener.start();


        }
    }


    //this methods makes the server to stop accepting new connections
    final public void stopListening()
    {
        readyToStop=true;
    }



    final synchronized public void close() throws IOException
    {
        if(serverSocket==null)
        return;
        stopListening();

        try
        {
            serverSocket.close();
        }
        finally
        {
            
                //close the clients that are already connected to the server
                Thread[] clientsThreadList =getClientConnections();

                for(int i=0;i<clientsThreadList.length;i++)
                {
                    try{
                        ((ConnectionToClient)clientsThreadList[i]).close();
                    }
                    catch(Exception ex)//ignore all the exception
                    {

                    }
                    
                }
            serverSocket=null;
             serverClosed();
        }
    }





     
    public void sendToAllClients(Object msg){
            Thread [] clientThreadsList =getClientConnections();
            for(int i=0;i<clientThreadsList.length;i++)
            {
                try{
                    ((ConnectionToClient)clientThreadsList[i]).sendToClient(msg);
                }
                catch(Exception e){}
            }

        }

    

        //ACCESSING METHODS

        //it returns true if server is ready to accept new clients
        final public boolean isListening()
        {
            return connectionListener!=null ;
        }


        //returns true if server is closed
        final public boolean isClosed()
        {
            return serverSocket ==null;
        }


        //this method returns the array containg the existing clients
        synchronized final public Thread[] getClientConnections()
        {
            Thread [] clientThreadList= new Thread[clientThreadGroup.activeCount()];
            clientThreadGroup.enumerate(clientThreadList);
            return clientThreadList;
        }


        final public int getNumberOfClients()
        {
            return clientThreadGroup.activeCount();
        }

        final public int getPort()
        {
            return port;
        }

        final public void setPort(int port)
        {
            this.port=port;
        }


        //this methods sets the time for time out.the server may take timeout amount of time to stop listening
        final public void setTimeout(int timeout)
        {
            this.timeout=timeout;
        }


        //back log is the maximum number of waiting connections that can be accepted by the operating system
        final public void setBackLog(int backlog)
        {
            this.backlog=backlog;
        }

       
      

       



//RUN METHOD
//runs the listening thread that allows the client to connect
final public void run()
{
    
    serverStarted();//hook
    
    try
    {
        while(readyToStop!=true)
        {
            try{
            //waiting her for the new connection from client
            Socket clientSocket=  serverSocket.accept();

            //when the client is accepted. create a thread to handle data exchange and then add it to the threds group
            synchronized(this)
            {
              ConnectionToClient c =new ConnectionToClient(this.clientThreadGroup, clientSocket, this);
            }
        }
        catch(InterruptedIOException ex)
        {
            //this exception will occur when connection timeout occurs
        }

    }
    serverStopped();
}
    catch(IOException ex)
    {
       if(readyToStop!=true)
       {
        listeningException(ex);//hook
       }
       else{
        serverStopped();
       }

    }
    finally{
        readyToStop=true;
        connectionListener=null;
        
        }




    }


    //METHODS THAT CAN BE OVERRIDDEN BY THE SUBCLASS / HOOKS

    protected void clientConnected(ConnectionToClient client) {}

    //this method is synchronized because a client maybe disconnected but the thread remains active until it is asynchroniously  removed from the threads group
     synchronized protected void clientDisconnected(ConnectionToClient client) {}

     //it is called when an exception is thrown in the connection to client
     synchronized protected void clientException(ConnectionToClient client, Throwable exception) {}

     protected void listeningException(Throwable exception) {}
     protected void serverStarted() {}
     protected void serverStopped() {}
     protected void serverClosed() {}

     //METHOD THAT MUST BE OVERRIDDEN IN THE SUBCLASS


     //this method handles a command that is sent from one client to the server
     protected abstract void handleMessageFromClient(Object msg, ConnectionToClient client);


     //METHODS TO BE USED WITHIN FRAMEWORK ONLY


     //it recieves the command sent from client to server. it is called by the run method of connection to clients instances that are waiting foe the messages to come
     
     final synchronized void receiveMessageFromClient(
        Object msg, ConnectionToClient client)
      {
        this.handleMessageFromClient(msg, client);
      }


}












