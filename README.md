# Java Thread Pool
## Un piccolo web server di esempio

Lo scopo di questi progetti è quello di presentare un semplice web server scritto interamente in Java, che faccia uso di un thread pool.

La classe server, tramite un attributo ThreadPool, gestirà le richieste in ingresso assegnandole ai vari thread a disposizione.

### 1 - My Custom Thread Pooled Server
```
La classe *MyThreadPool* contiene 3 attributi
  - una stringa, che dà nome al thread
  - una *BlockingQueue* (coda generica, in questo caso una coda di oggetti Runnable)
  - un *TaskExecutor*, classe Runnable che si occupa di estrarre il prossimo task dalla coda ed eseguirlo

Il **costruttore** prende in input 2 parametri: *queueSize*, *nThread*, e crea un oggetto MyThreadPool con *nThread* thread, ciascuno dei quali gestisce una coda di dimensione *queueSize*

La funzione **submitTask** inserisce nella coda un task
```

La classe *TaskExecutor* è un Runnable, che un solo attributo: una *BlockingQueue*.
Questa classe contiene inoltre la funzione **run()**, che non fa altro che estrarre il primo task dalla coda ed eseguirlo

La classe *BlockingQueue* gestisce una generica coda di oggetti.
Il suo **costruttore** prende in input la dimensione massima della coda.

Le funzioni **enqueue** e **dequeue**, rispettivamente, inseriscono o estraggono dalla coda un task.

### 2a - Thread Pooled Server
### 2b - Cached Pooled Server
