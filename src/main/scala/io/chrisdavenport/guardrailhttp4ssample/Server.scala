package io.chrisdavenport.guardrailhttp4ssample

import cats.{Order => _, _}
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent._

import org.http4s.implicits._

import example.server.definitions._
import example.server.store._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import scala.concurrent.duration._

object Server {

  def server[F[_]: Concurrent: Timer : ContextShift]: Resource[F, Unit] = for {
    // Shared State
    map <- Resource.liftF(
      Ref[F].of(Map[Long, Order]())
    )

    // Generate Server
    s <- EmberServerBuilder.default[F]
      .withHttpApp(new StoreResource[F]().routes(Server.handler(map)).orNotFound) // Server
      .build

    // Play with Client Against the Server
    client <- EmberClientBuilder.default[F]
      .build
      .map(example.client.store.StoreClient.httpClient(_, "http://localhost:8080"))
    _ <- Resource.liftF(
      client.placeOrder(example.client.definitions.Order(id = Some(5L)))
    )
    resp <- Resource.liftF(
      client.getOrderById(5L)
    )
    _ <- Resource.liftF(
      Sync[F].delay(println(resp))
    )
  } yield ()

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
    def placeOrder(respond: PlaceOrderResponse.type)(body: Order): F[PlaceOrderResponse] = 
      for {
        id <- body.id.fold(Sync[F].delay(scala.util.Random.nextLong))(_.pure[F])
        newOrder = body.copy(id = id.some)
        _ <- map.update(m => m + (id -> newOrder))
      } yield respond.Ok(newOrder)
  }

}