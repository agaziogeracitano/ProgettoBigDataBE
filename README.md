Il backend è stato sviluppato utilizzando Apache Spark con il linguaggio Scala per l'elaborazione e l'analisi di dataset di grandi dimensioni. Il sistema implementa un'architettura client-server, 
con API REST esposte tramite il microframework Cask, consentendo la comunicazione con il frontend e la gestione delle query sui dati. Il backend è progettato per elaborare dati in modo distribuito 
ed efficiente, garantendo elevate prestazioni anche su grandi volumi di informazioni.

**Tecnologie Utilizzate**
-Apache Spark: Framework di elaborazione distribuita
-Scala: Linguaggio di programmazione funzionale e orientato agli oggetti
-Cask: Microframework per l'esposizione di API REST
-MLlib: Libreria di Machine Learning integrata in Spark
-OpenAI API: Per il supporto all'analisi tramite intelligenza artificiale
-JSON: Formato di scambio dati

**Funzionalità Principali**
-Generazione di regole associative con l'algoritmo FPGrowth
-Predizioni di acquisti futuri basate sui prodotti già acquistati
-Analisi del comportamento degli utenti attraverso il tracciamento degli ordini
-Query base per ottenere informazioni sui prodotti più venduti, giorni con maggiore affluenza e orari di acquisto
-Supporto AI per l'analisi descrittiva dei risultati tramite ChatGPT
-Configurazione CORS per consentire la comunicazione con il frontend
