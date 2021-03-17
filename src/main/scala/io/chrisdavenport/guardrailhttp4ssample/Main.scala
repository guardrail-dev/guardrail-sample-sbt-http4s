package io.chrisdavenport.guardrailhttp4ssample

import cats.{Order => _, _}
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent._

import org.http4s.implicits._

import example.server.definitions._
import example.server.store._
import org.http4s.ember.server.EmberServerBuilder

object Main extends IOApp {

  def handler[F[_]: Sync](map: Ref[F, Map[Long, Order]]) = new StoreHandler[F]{

    // We have no inventory, only orders
    def getInventory(respond: GetInventoryResponse.type)(): F[GetInventoryResponse] = 
      Applicative[F].pure(respond.Ok(Map()))

    def deleteOrder(respond: DeleteOrderResponse.type)(orderId: Long): F[DeleteOrderResponse] = 
      map.modify{m => 
        val out = m.get(orderId).fold[DeleteOrderResponse](respond.NotFound)(_ => respond.Accepted)
        (m - orderId, out)
      }

    def getOrderById(respond: GetOrderByIdResponse.type)(orderId: Long): F[GetOrderByIdResponse] = 
      map.get
        .map(_.get(orderId))
        .map(_.fold[GetOrderByIdResponse](respond.NotFound)(o => respond.Ok(o)))

    // Weird that this is optional
    def placeOrder(respond: PlaceOrderResponse.type)(body: Option[Order]): F[PlaceOrderResponse] = 
      body.traverse{order => 
        for {
          id <- order.id.fold(Sync[F].delay(scala.util.Random.nextLong))(_.pure[F])
          newOrder = order.copy(id = id.some)
          _ <- map.update(m => m + (id -> newOrder))
        } yield respond.Ok(newOrder)
      }.map(
        _.getOrElse(respond.MethodNotAllowed)
      )
  }


  def run(args: List[String]): IO[ExitCode] = {
    val r = for {
      map <- Resource.liftF(
        Ref[IO].of(Map[Long, Order]())
      )
      _ <- EmberServerBuilder.default[IO]
        .withHttpApp(new StoreResource[IO]().routes(handler(map)).orNotFound)
        .build
    } yield ()
    r.use(_ => IO.never).as(ExitCode.Success)
  }

}