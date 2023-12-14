package xyz.didx

import sttp.tapir.*

import Library.*
import cats.effect.IO
import io.circe.generic.auto.*
import scala.collection.mutable
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import xyz.didx.ai.AiHandler
import xyz.didx.ai.AiHandler.getAiResponse
import xyz.didx.ai.model.ChatState
import cats.effect.ExitCode
import cats.data.EitherT

case class AiInteractionCounter(counter: Int)

object Endpoints:
  case class User(name: String) extends AnyVal
  case class Message(msg: String, id: String)

  private var interactionCounter = AiInteractionCounter(0)
  private val userStates: mutable.Map[String, ChatState] = mutable.Map()

  def incrementAiInteractionCounter = {
    interactionCounter =
      interactionCounter.copy(counter = interactionCounter.counter + 1)
    interactionCounter.counter
  }

  val aiEndpoint: PublicEndpoint[Message, Unit, String, Any] =
    endpoint.post
      .in("ai")
      .in(jsonBody[Message])
      .out(stringBody)

  val aiServerEndpoint: ServerEndpoint[Any, IO] =
    aiEndpoint.serverLogicSuccess(message => {
      val userId = message.id
      val currentState = userStates.getOrElse(userId, ChatState.Onboarding)

      println(s"Number of AI interactions: ${incrementAiInteractionCounter}")

      IO.defer {
        val aiResponse =
          for {
            responseState <- EitherT(
              AiHandler.getAiResponse(
                input = message.msg,
                conversationId = userId,
                state = currentState,
                telNo = Some(userId)
              )
            )
          } yield (
            s"${responseState._1}",
            userStates.update(userId, responseState._2)
          )

        aiResponse.value
          .flatMap {
            case Right(response) => {
              println(s"Ai output: ${response}")
              println(s"State: ${userStates.get(userId)}")
              IO.pure(s"${response._1}")
            }
            case Left(error) => {
              println(s"Error from AI: $error")
              IO.pure(s"Error generating AI response")
            }
          }
      }
    })

  val helloEndpoint: PublicEndpoint[User, Unit, String, Any] = endpoint.get
    .in("hello")
    .in(query[User]("name"))
    .out(stringBody)
  val helloServerEndpoint: ServerEndpoint[Any, IO] =
    helloEndpoint.serverLogicSuccess(user => IO.pure(s"Hello ${user.name}"))

  val booksListing: PublicEndpoint[Unit, Unit, List[Book], Any] = endpoint.get
    .in("books" / "list" / "all")
    .out(jsonBody[List[Book]])
  val booksListingServerEndpoint: ServerEndpoint[Any, IO] =
    booksListing.serverLogicSuccess(_ => IO.pure(Library.books))

  val apiEndpoints: List[ServerEndpoint[Any, IO]] =
    List(helloServerEndpoint, booksListingServerEndpoint, aiServerEndpoint)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "dawn-patrol-api", "1.0.0")

  val all: List[ServerEndpoint[Any, IO]] = apiEndpoints ++ docEndpoints

object Library:
  case class Author(name: String)
  case class Book(title: String, year: Int, author: Author)

  val books = List(
    Book(
      "The Sorrows of Young Werther",
      1774,
      Author("Johann Wolfgang von Goethe")
    ),
    Book("On the Niemen", 1888, Author("Eliza Orzeszkowa")),
    Book("The Art of Computer Programming", 1968, Author("Donald Knuth")),
    Book("Pharaoh", 1897, Author("Boleslaw Prus"))
  )
