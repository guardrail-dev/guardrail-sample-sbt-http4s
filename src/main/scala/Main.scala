import cats.syntax.all._
import cats.effect._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    Server.server[IO].use(_ => IO.never).as(ExitCode.Success)
  }

}

