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

Il **costruttore** prende in input 2 parametri: *queueSize*, *nThread*, e crea un oggetto MyThreadPool con *nThread* thread, ciascuno dei quali gestisce una coda di dimensione *queueSize*

La funzione **submitTask** inserisce nella coda un task
######Task Executor
La classe *TaskExecutor* è un Runnable, che un solo attributo: una *BlockingQueue*.
Questa classe contiene inoltre la funzione **run()**, che non fa altro che estrarre il primo task dalla coda ed eseguirlo
######Blocking Queue
La classe *BlockingQueue* gestisce una generica coda di oggetti.
Il suo **costruttore** prende in input la dimensione massima della coda.

Le funzioni **enqueue** e **dequeue**, rispettivamente, inseriscono o estraggono dalla coda un task.
######Worker Runnable
La classe *WorkerRunnable* legge dallo stream della socket, fa il parsing delle richieste e risponde di conseguenza.

Il suo **costruttore** prende in input la socket sulla quale comunicare, e una stringa serverText.

Dentro la funzione **run()**, il server è in grado di mostrare una home page (http://localhost:9000/), di scaricare un file presente nella cartella del progetto (http://localhost:9000/nome-file), e di mostrare un'ulteriore pagina (http://localhost:9000/upload) contenente un form che permette di effettuare l'upload di file di testo con una richiesta POST.

#####Dettagli sul funzionamento
La classe *Server*, contenente l'attributo di tipo MyThreadPool, viene lanciata dal main. Nella funzione  **run()** il server si mette in ascolto sulla serverSocket: all'arrivo di una nuova richiesta, viene ritornata la clientSocket. A questo punto è sufficiente lanciare la funzione **submitTask** della thread pool, passando come parametro un oggetto *WorkerRunnable* che comunica sulla clientSocket appena ottenuta.

### 2a - Thread Pooled Server
### 2b - Cached Pooled Server
