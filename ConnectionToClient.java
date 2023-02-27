import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

//an instance of this class is created by the adstract server class when a client attemps to connect.
//it accepts message from the client. it is also responsibl for sending data/message back to the client sicne the sockets are private
//abstract server class contains a set of intanceses of this class and is responsible for adding and deleting connection i.e instances


public class ConnectionToClient extends Thread {
    private AbstractServer server;//a reference to the server that created this instance
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean readyToStop;
    

    //hash map to save clients information for later use
    
    
    private  HashMap savedinfo = new HashMap<>(10);
    private Socket clientSocket;
   
    //CONSTRUCTOR

    public ConnectionToClient(ThreadGroup group, Socket clientSocket,AbstractServer server) throws IOException
    {
        super(group,(Runnable)null);

        //initiallize variables
        this.clientSocket=socket;
        this.server=server;

        clientSocket.setSoTimeout(0);//inifinite timeout
        

        //initiallize object streams
        try{
            output= new ObjectOutputStream(clientSocket.getOutputStream());
            input= new ObjectInputStream(clientSocket.getInputStream());
        }
        catch(IOException ex)
        {
            try{
                closeAll();

            }
            catch(Exception exc)
            {

            }
            throw ex;
        }
        readyToStop=false;
        start(); //start the thread and wait for the message to come
        


    }
    


    


//INSTANCE METHODS
   public void sendToClient(Object msg) throws IOException
   {
    if(clientSocket==null||output==null)
    {
        throw new SocketException("socket does not exist");

    }
      output.writeObject(msg);

   }


   //closes the client
   final public void close() throws IOException{

    readyToStop=true;
    close();

   }

   //ACCESSING METHODS

   //retruns the client's address
   final public InetAddress getInternetAddress()
   {
    return clientSocket==null? null:clientSocket.getInetAddress();
   }


   //gives string representation of client
   public String toString()
   {
     return clientSocket == null ? null :
       clientSocket.getInetAddress().getHostName()
         +" (" + clientSocket.getInetAddress().getHostAddress() + ")";
   }

   //set information of clients to save 
   public void setInfo(String infoType,Object info)
   {
    savedinfo.put(infoType,info);
   }

   public Object getInfo(String infoType)
   {
     return savedinfo.get(infoType);
   }

   //RUNNABLE METHOD

   //constanly reads clients input messages.
   //and sends the red messages to the server
   final public void run()
   {
    server.clientConnected(this);
    try{
        //this is the message from the client
        Object msg;
        
        while(readyToStop!=true)
        {
            try{

                msg= input.readObject();//this waits to recieve the message
                if(readyToStop!=true&& handleMessageFromClient(msg))
                {
                    server.receiveMessageFromClient(msg, this);
                }

            }
            catch(ClassNotFoundException ex)//when unkonown clss is recieved in object
            {
                server.clientException(this,ex);
            }
            catch(RuntimeException e)//thrown by message handler
            {
                server.clientException(this,e);
            }

        }
    }
    catch(Exception e)
    {
        try{
            closeAll();
        }
        catch(Exception ex)
        {}
        server.clientException(this, e);
    }
    finally{
       
    server.clientDisconnected(this);
    }

   }


   //METHODS THAT MUST BE OVERRIDDEN


   protected boolean handleMessageFromClient(Object msg)
   {
    return true;
   }

   //METHOD THAT WILL RUN ONLY IN FRAMEWORK

   final public void closeAll() throws IOException
   {
    try{
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
    finally{
        output=null;
        input=null;
        clientSocket=null;
    }
   }






}





