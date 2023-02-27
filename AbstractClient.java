
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public abstract class AbstractClient implements Runnable// it implemments runnable so that the class can be converted to
                                                        // a thread
{



    private Socket clientSocket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread clientReader; // the thread created to read data from the server

    private boolean readyToStop = false; // indicates if the thread is ready to stop,loop in the runnable interface
                                        

    private String host;
    private int port;

    // constructor

    public  AbstractClient(String host, int port) {
        // initializing the variables
        this.host = host;
        this.port = port;

    }

   


    final public void openConnection() throws IOException {
        // if already connection is open
        if (isConnected())
            return;

        // if connection is not open,then create socket and data streams
        try {
            clientSocket = new Socket(host, port);
            output = new ObjectOutputStream(clientSocket.getOutputStream());// getoutputstream is a method of socket
                                                                            // class in java
            input = new ObjectInputStream(clientSocket.getInputStream());// getInputStream is method of socket class in
                                                                         // java
        } catch (IOException ex) 
        {
            // when there is a failure , then all three of them must be closed.i.e in case
            // the catch block is implemented
            try {
                closeAll();

            } catch (Exception exc) {

            }
            throw ex;
        }

        clientReader = new Thread(this);// it will create a data reader thread
        readyToStop = false;
        clientReader.start();//it starts the thread
                             
    }

   
    final public void sendToServer(Object msg) throws IOException
    {
        if(clientSocket==null||output==null)
        {
            throw new SocketException((String) ("socket not found"));
        }

        output.writeObject(msg);  //this is the message that wil be sent to the client

    }

    // closes the connection with the server
    final public void closeConnection() throws IOException {

        readyToStop = true;
        try{
        closeAll(); 
        }
        finally{
connectionClosed();//hook
        }
    }


    // ACCESSING METHODS

    // returs true if the client is connected
    final public boolean isConnected() {
        return clientReader != null && clientReader.isAlive();//thread is alive
    }

    final public int getPort() {
        return port;
    }


    //sets the port number for the NEXT connection 
    //change in port only takes effect at the time of next call to openConnection
    final public void setPort(int port) {
        this.port = port;

    }

    final public String getHost()
    {
        return host;
    }

    
    //sets the host for the NEXT connection 
    //change in host only takes effect at the time of next call to openConnection
    final public void setHost(String host)
    {
        this.host=host;
    }

    final public InetAddress getInternetAddress()
    {
        return clientSocket.getInetAddress();

    }


    //RUN METHODS

   
    final public void run()
    {
        connectionEstablished();//hook

        Object msg;//the message from the server

        //loop waiting for data
        try
        {
            while(readyToStop!=true)
            {
              
               
                    msg=input.readObject();
                    //the concerete class will do whatever they want to do with the msg by implementing the handler method
                   
                        handleMessageFromServer(msg);
                    

            }
               
               
            

        }
        catch(Exception exception)
        {
            if(readyToStop!=true)
            {
                try{
                    closeAll();
                }
                catch(Exception ex)
                {

                }
                clientReader=null;
                connectionException(exception);//hook

            }
        }
        finally 
        {
            clientReader=null;
            
        }




    }

    //METHOD TO BE USED WITHIN FRAMEWORK

    //it closes all the connection of client with the server
    final public void closeAll() throws IOException
    {
        try
        {
            if(clientSocket!=null)
            {
                clientSocket.close();
            }
            if(output!=null)
            {
                output.close();
            }
            if(input!=null)
            {
                input.close();
            }

        }
        finally
        {
            //set the streams and socket to null
            output=null;
            input=null;
            clientSocket=null;


        }


    }
   

    //METHODS THAT CAN BE OVERRIDDEN IN SUB CLASS / HOOKS
     protected void connectionClosed(){} 
     protected void connectionException(Exception exception){}
     protected void connectionEstablished(){}


     //METHOD THAT MUST BE OVERRIDDEN IN SUB CLASS
     protected abstract void handleMessageFromServer(Object msg);
     


}