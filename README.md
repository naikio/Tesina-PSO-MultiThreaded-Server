# Java Thread Pool
## Un piccolo web server di esempio

Lo scopo di questi progetti è quello di presentare un semplice web server scritto interamente in Java, che faccia uso di un thread pool.

La classe server, tramite un attributo ThreadPool, gestirà le richieste in ingresso assegnandole ai vari thread a disposizione.

### 1 - My Custom Thread Pooled Server
######My Thread Pool
La classe *MyThreadPool* contiene 3 attributi
  - una stringa, che dà nome al thread
  - una *BlockingQueue* (coda generica, in questo caso una coda di oggetti Runnable)
  - un *TaskExecutor*, classe Runnable che si occupa di estrarre il prossimo task dalla coda ed eseguirlo

Il **costruttore** prende in input 2 parametri: *queueSize*, *nThread*, e crea un oggetto MyThreadPool con un numero di thread pari a *nThread*, ciascuno dei quali gestisce una coda di dimensione *queueSize*

La funzione **submitTask** inserisce nella coda un task
```java
public class MyThreadPool {
    BlockingQueue <Runnable> queue;
    public MyThreadPool(int queueSize, int nThread) {
        queue = new BlockingQueue<>(queueSize);
        String threadName = null;
        TaskExecutor task = null;
        for (int count = 0; count < nThread; count++) {
        	threadName = "Thread-"+count;
        	task = new TaskExecutor(queue);
            Thread thread = new Thread(task, threadName);
            thread.start();
        }
    }

    public void submitTask(Runnable task) throws InterruptedException {
        queue.enqueue(task);
    }
}
```
######Task Executor
La classe *TaskExecutor* è un Runnable, che ha un solo attributo: una *BlockingQueue*.
Questa classe contiene inoltre la funzione **run()**, che non fa altro che estrarre il primo task dalla coda ed eseguirlo
```java
public class TaskExecutor implements Runnable {
    BlockingQueue<Runnable> queue;
    
	...

    @Override
    public void run() {
        ...
            while (true) {
                String name = Thread.currentThread().getName();
                Runnable task = queue.dequeue(); //estrae primo TASK
                System.out.println("Task Started by Thread :" + name);
                task.run(); //esegue TASK
                System.out.println("Task Finished by Thread :" + name);
            }
        ...
    }
}
```
######Blocking Queue
La classe *BlockingQueue* gestisce una generica coda di oggetti.
Il suo **costruttore** prende in input la dimensione massima della coda.

Le funzioni **enqueue** e **dequeue**, rispettivamente, inseriscono o estraggono dalla coda un task.

Notare l'utilizzo delle chiamate di **wait()** e **notifyAll()**, rispettivamente per mettere in attesa il thread nel caso in cui la coda sia piena, e per "svegliare" tutti i thread in attesa, nel caso in cui la coda sia vuota.
```java
import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue <Type>  {
    private Queue<Type> queue = new LinkedList<Type>();
    private int EMPTY = 0;
    private int MAX_TASK_IN_QUEUE = -1;

    public BlockingQueue(int size){
        this.MAX_TASK_IN_QUEUE = size;
    }

    public synchronized void enqueue(Type task)
            throws InterruptedException  {
        while(this.queue.size() == this.MAX_TASK_IN_QUEUE) {
            wait();
        }
        if(this.queue.size() == EMPTY) {
            notifyAll();
        }
        this.queue.offer(task);
    }

    public synchronized Type dequeue()
            throws InterruptedException{
        while(this.queue.size() == EMPTY){
            wait();
        }
        if(this.queue.size() == this.MAX_TASK_IN_QUEUE){
            notifyAll();
        }
        return this.queue.poll();
    }
}
```
######Worker Runnable
La classe *WorkerRunnable* legge dallo stream della socket, fa il parsing delle richieste e risponde di conseguenza.

Il suo **costruttore** prende in input la socket sulla quale comunicare, e una stringa serverText.

Dentro la funzione **run()**, il server è in grado di mostrare una home page (http://localhost:9000/), di scaricare un file presente nella cartella del progetto (http://localhost:9000/nome-file), e di mostrare un'ulteriore pagina (http://localhost:9000/upload) contenente un form che permette di effettuare l'upload di file di testo con una richiesta POST.

#####Dettagli sul funzionamento
La classe *Server*, contenente l'attributo di tipo MyThreadPool, viene lanciata dal main. Nella funzione  **run()** il server si mette in ascolto sulla serverSocket: all'arrivo di una nuova richiesta, viene ritornata la clientSocket. A questo punto è sufficiente lanciare la funzione **submitTask** della thread pool, passando come parametro un oggetto *WorkerRunnable* che comunica sulla clientSocket appena ottenuta.

```java
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import servers.MyThreadPool;

public class Server implements Runnable{

    ...//altri attributi
    protected MyThreadPool threadPool = new MyThreadPool(1,1);
    	// A pool of thread is initialized. Each request is handled by one thread
    	// You can see that if you set this value to 1, you won't be able to download multiple files at the same time
    
    // constructor
    public Server(int port){
        this.serverPort = port;
    }

    
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
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
            try {
				this.threadPool.submitTask(
				    new WorkerRunnable(clientSocket,
				        "Thread Pooled Server"));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }
        System.out.println("Server Stopped.") ;
    }
    
	...
    
    public static void main(String[] args) {
    	
    	Server server = new Server(9000);
        //lancia l'oggetto server appena creato, eseguendo la funzione run() in un nuovo thread
    	new Thread(server).start();
    	...
    }
}
```

### 2a - Thread Pooled Server
######Server
L'attributo ThreadPool della classe server, questa volta, è un oggetto di tipo ExecutorService:
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

   ... //altri attributi
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
```
Come si può notare, la funzione *newFixedThreadPool* prende in input la dimensione del pool.


### 2b - Cached Pooled Server
######Server
```java
protected ExecutorService threadPool = Executors.newCachedThreadPool();
// i thread in questo caso sono creati dinamicamente
```
