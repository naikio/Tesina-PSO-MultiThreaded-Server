package servers;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    protected int          serverPort   = 9000;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ExecutorService threadPool =
        Executors.newCachedThreadPool();
    	// A cached pool is initialized. Each request is handled by one thread
    	// Threads are created dynamically
    
    // constructor
    public Server(int port){
        this.serverPort = port;
    }

    
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket(); //open Socket and start listening on serverPort (9000)
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept(); //accept incoming connection
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            // Each time there's a new connection, established with a clientSocket, a new thread from the Thread Pool is executed
            // This new thread is initialized using WorkerRunnable class, which contains HTML code and other stuff to handle GET and POST requests
            this.threadPool.execute(
                new WorkerRunnable(clientSocket,
                    "Thread Pooled Server"));
            
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }


    // check if server is stopped
    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    // stop server (this does not kill server's thread, it only shuts down the server socket!)
    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 9000", e);
        }
    }	
    
    
    public static void main(String[] args) {
    	
    	Server server = new Server(9400);
    	new Thread(server).start();

    	try {
    	    Thread.sleep(3000 * 1000);
    	    // As this is only for test purposes, thread that runs the server will run for 3000 seconds then it will stop 
    	} catch (InterruptedException e) {
    	    e.printStackTrace();
    	}
    	System.out.println("Stopping Server");
    	server.stop();
    }
}