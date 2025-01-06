import org.apache.spark
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.fpm.{FPGrowth, FPGrowthModel}
import spire.math.QuickSort.limit
import java.io.IOException
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets


object App extends cask.MainRoutes {

  def aggiungiCORS(risposta: String): cask.Response[String] = {
    cask.Response(risposta, headers = Seq(
      "Access-Control-Allow-Origin" -> "*", // Permetti l'accesso da qualsiasi origine
      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS", // Metodi consentiti
      "Access-Control-Allow-Headers" -> "Content-Type" // Header consentiti
    ))
  }

  @cask.options("/")
  def handleOptions(): cask.Response[String] = {
    aggiungiCORS("")
  }

  override def port: Int = 6060

  // Initialize SparkSession
  val ss = SparkSession
    .builder()
    .master("local[*]")
    .appName("ProgettoBigData")
    .getOrCreate()

  import ss.implicits._

  val sc = ss.sparkContext
  val prodotti = ss.read.options(Map("header" -> "true", "inferSchema" -> "true")).csv("src/main/data/products.csv")
  val dipartimenti = ss.read.options(Map("header" -> "true", "inferSchema" -> "true")).csv("src/main/data/departments.csv")
  val corsie = ss.read.options(Map("header" -> "true", "inferSchema" -> "true")).csv("src/main/data/aisles.csv")
  val ordiniProdotti = ss.read.options(Map("header" -> "true", "inferSchema" -> "true")).csv("src/main/data/order_products__prior.csv")
    .union(ss.read.options(Map("inferSchema" -> "true", "header" -> "true")).csv("src/main/data/order_products__train.csv"))
  val ordini = ss.read.options(Map("header" -> "true", "inferSchema" -> "true")).csv("src/main/data/orders.csv")
  sc.setLogLevel("ERROR")

  var modello: FPGrowthModel = null

  @cask.get("/")
  def home(): cask.Response[String] = {
    aggiungiCORS("Il server è in esecuzione sulla porta &060")
  }


  def convertiRispostaInJson(dataframe: DataFrame): String = {
    val array = dataframe.toJSON.collect()
    array.mkString("[", ",", "]")
  }


  @cask.get("/topNProdotti/:num")
  def topNprodottiPiuVenduti(num: Int): cask.Response[String] = {
    val topNprodotti = ordiniProdotti
      .join(prodotti, Seq("product_id")) // Join sulla colonna "product_id"
      .groupBy("product_name")           // Raggruppa per product_name
      .count()                           // Conta il numero di vendite per prodotto
      .orderBy(desc("count"))            // Ordina in ordine decrescente per conteggio
      .limit(num)
    val rispostaJson = convertiRispostaInJson(topNprodotti)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/ordiniOre")
  def ordiniOre():cask.Response[String]={
    val df_ore=ordini.groupBy("order_hour_of_day").count().orderBy("order_hour_of_day")
    val rispostaJson=convertiRispostaInJson(df_ore)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  def prodottiPerOgniCorridoio(): DataFrame = {
    val prodOgniCorridoio = ordiniProdotti
      .join(prodotti, "product_id")  // Uniamo i dati con la tabella dei prodotti
      .groupBy("aisle_id", "product_name")  // Raggruppiamo per aisle_id e nome del prodotto
      .agg(count("product_name").alias("vendite"))  // Calcoliamo il conteggio delle vendite
      .orderBy(col("aisle_id"), desc("vendite"))  // Ordiniamo per aisle_id e vendite in ordine decrescente
    prodOgniCorridoio
  }

  @cask.get("/topProdottiPerAisle")
  def prodottoPiuVendutoPerCorridoio(): cask.Response[String] = {
    val ppoc = prodottiPerOgniCorridoio()
      .groupBy("aisle_id")
      .agg(max("vendite").alias("vendite_piu_alte"))

    val prodottoPiuVenduto = prodottiPerOgniCorridoio()
      .join(ppoc, Seq("aisle_id"))
      .filter(col("vendite") === col("vendite_piu_alte"))
      .select("aisle_id", "product_name", "vendite").orderBy(col("aisle_id").asc)

    val rispostaJson=convertiRispostaInJson(prodottoPiuVenduto)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/prodottoPiuVendutoPerAisleSpecifico/:aisleId")
  def prodottoPiuVendutoPerCorridoioSpecifico(aisleId: Int): cask.Response[String] = {
    val prodFiltrati = ordiniProdotti
      .join(prodotti, "product_id")
      .groupBy("aisle_id", "product_name")
      .agg(count("product_name").alias("vendite"))
      .filter(col("aisle_id") === aisleId)
      .orderBy(desc("vendite"))
      .limit(1)

    val rispostaJson=convertiRispostaInJson(prodFiltrati)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/topOrderDay")
  def giornoConPiuOrdini(): cask.Response[String] = {
    val giornoPiuOrdini = ordini.groupBy("order_dow")
      .agg(count("*").alias("ordiniTotali"))
      .orderBy(desc("ordiniTotali"))

    val rispostaJson=convertiRispostaInJson(giornoPiuOrdini)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/numeroArticoliOrdine")
  def numeroArticoliOrdine(): cask.Response[String] = {
    val ordiniConProdotti = ordiniProdotti
      .join(prodotti, "product_id")
      .groupBy("order_id")
      .agg(count("product_id").alias("numero_articoli"))
      .orderBy(desc("numero_articoli"))
    val distribuzioneArticoli = ordiniConProdotti
      .groupBy("numero_articoli")
      .count()
      .orderBy(asc("numero_articoli"))
    val rispostaJson = convertiRispostaInJson(distribuzioneArticoli)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/analizzaUtente/:utente")
  def analizzaUtente(utente: String): cask.Response[String] = {
    val ordiniUtente = ordini.filter($"user_id" === utente)
    // Unisci gli ordini con i prodotti per ottenere informazioni sui prodotti acquistati
    val prodottiAcquistati = ordiniUtente
      .join(ordiniProdotti, "order_id") // Unisci con la tabella ordini_prodotti per ottenere i dettagli sui prodotti
      .join(prodotti, "product_id")    // Unisci con la tabella prodotti per ottenere il nome del prodotto
      .select("user_id","order_id", "product_name", "order_dow", "order_hour_of_day")  // Seleziona le colonne rilevanti
    // Raggruppa i prodotti acquistati per l'utente e conta le occorrenze di ciascun prodotto
    val prodottiRaggruppati = prodottiAcquistati
      .groupBy("user_id","product_name")
      .agg(count("product_name").alias("numero_acquisti"))
      .orderBy(desc("numero_acquisti")) // Ordina i prodotti per numero di acquisti
    val rispostaJson = convertiRispostaInJson(prodottiRaggruppati)
    scriviFile(rispostaJson)
    aggiungiCORS(rispostaJson)
  }

  @cask.get("/regoleAssociative")
def regoleAssociative(): cask.Response[String] = {
  val train = ss.read.options(Map("inferSchema" -> "true", "header" -> "true"))
    .csv("src/main/data/order_products__train.csv")

  // Prepara i dati raggruppando i prodotti per ordine
  val dataTrain = train.join(prodotti, "product_id")
    .groupBy("order_id")
    .agg(collect_set("product_name").alias("items"))

  // Esegui il modello FP-Growth
  val fpgrowth = new FPGrowth()
    .setItemsCol("items")
    .setMinSupport(0.001)
    .setMinConfidence(0.4)
  modello = fpgrowth.fit(dataTrain)

  // Estrai le regole associative
  val rules = modello.associationRules

  val rispostaJson=convertiRispostaInJson(rules)
  scriviFile(rispostaJson)
  aggiungiCORS(rispostaJson)
}

  @cask.get("/predizione")
  def predizione(dati: String): cask.Response[String] = {
    val listaDati: Seq[String] = dati.split(",").map(_.trim).toSeq
    if (modello == null) {
      regoleAssociative()
    }
    val regole = modello.associationRules
    // Filtra le regole di associazione dove tutti gli antecedenti sono presenti nei dati
    val proposte = regole.filter { x =>
      x.getAs[Seq[String]]("antecedent").forall(listaDati.contains)
    }
    // Ordina in base alla confidenza e limita a 5 proposte
    val finale = proposte.sort(col("confidence").desc).limit(5)

    // Estrai direttamente i conseguenti dalle regole e uniscili in un set
    val proposals = finale.collect().flatMap { rule =>
      val consequent = rule.getAs[Seq[String]]("consequent")
      consequent
    }.toSet
    // Filtra le proposte per rimuovere quelle già presenti nei dati originali
    val proposteFinali = proposals.filterNot(listaDati.contains)

    val rdd = sc.parallelize(proposteFinali.toSeq)
    val dataframe = rdd.toDF("suggestion")
    val rispostaJson= convertiRispostaInJson(dataframe)
    aggiungiCORS(rispostaJson)
  }

  def scriviFile(content: String): Unit = {
    try {
      Files.write(Paths.get("src/main/scala/ultimaQuery.txt"), content.getBytes(StandardCharsets.UTF_8))
      println(s"Il contenuto è stato scritto nel file ultimaQuery.txt")
    } catch {
      case e: IOException =>
        println(s"Si è verificato un errore durante la scrittura del file: ${e.getMessage}")
    }
  }

@cask.get("/chiediAchat")
def chiediAChat():cask.Response[String]= {
 //leggere il contenuto del file
  val filename = "src/main/scala/ultimaQuery.txt"
  val content = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)
  val messaggio="Analizzami in modo approfondito questi dati che provengono dal sito Instacart." + content +" non usare grassetto"
  val risposta=OpenAIClient.sendMessageToChatGPT(messaggio)
  aggiungiCORS(risposta)
}
  initialize()
}
