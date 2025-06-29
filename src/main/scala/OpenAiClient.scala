import sttp.client3._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

object OpenAIClient {
  val apiKey: String = "chiave"

  //modelli per strutturare la richiesta e la risposta
  case class ChatMessage(role: String, content: String)
  case class ChatRequest(model: String, messages: List[ChatMessage], temperature: Double = 0.7)
  case class ChatResponse(choices: List[ChatChoice])
  case class ChatChoice(message: ChatMessage)

  def sendMessageToChatGPT(prompt: String): String = {
    val backend = HttpURLConnectionBackend()
    val url = uri"https://api.openai.com/v1/chat/completions"

    //payload della richiesta
    val payload = ChatRequest(
      model = "gpt-4o-mini",
      messages = List(ChatMessage("user", prompt))
    )

    //richiesta HTTP POST
    val response = basicRequest
      .post(url)
      .header("Authorization", s"Bearer $apiKey")
      .header("Content-Type", "application/json")
      .body(payload.asJson.noSpaces)
      .send(backend)

    //gestione risposta
    response.body match {
      case Right(body) =>
        decode[ChatResponse](body) match {
          case Right(chatResponse) => chatResponse.choices.head.message.content
          case Left(error)         => s"Errore nel parsing della risposta: $error"
        }
      case Left(error) =>
        s"Errore nella richiesta: $error"
    }
  }
}
