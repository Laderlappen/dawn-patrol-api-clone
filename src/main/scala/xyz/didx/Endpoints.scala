package xyz.didx

import sttp.tapir.*

import Library.*
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import xyz.didx.ai.AiHandler
import xyz.didx.ai.AiHandler.getAiResponse
import xyz.didx.ai.model.ChatState
import cats.effect.ExitCode
import cats.data.EitherT

import cats.effect.unsafe.implicits.global

object Endpoints:
  case class User(name: String) extends AnyVal
  case class Message(msg: String) extends AnyVal

  val aiEndpoint: PublicEndpoint[Message, Unit, String, Any] = endpoint.get
    .in("ai")
    .in(query[Message]("msg"))
    .out {

      stringBody
    }
  val echoServerEndpoint: ServerEndpoint[Any, IO] =
    aiEndpoint.serverLogicSuccess(message => {

      IO.pure {

        val aiResponse = for {
          responseState <- EitherT(
            AiHandler.getAiResponse(
              input = message.msg,
              conversationId = "0725320983",
              state = ChatState.Onboarding,
              telNo = Some("0725320983")
            )
          )
        } yield s"Got response from AI: ${responseState._1}"

        aiResponse.value.unsafeRunSync() match
          case Right(response) => {
            println(s"Ai output ${response}")
            s"${response}"
          }
          case Left(error) => {
            println(s"Error from AI: $error")
            s"Error from AI"
          }
          case null => {
            s"Something went wrong"
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
    List(helloServerEndpoint, booksListingServerEndpoint, echoServerEndpoint)

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
