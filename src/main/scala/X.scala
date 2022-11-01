import cats.{Monad, MonadError}
import scala.language.higherKinds

case class UserId(id: String)
case class OrderId(id: String)
case class UserProfile(userId: UserId, userName: String)
case class Order(userId: UserId, orderId: OrderId)

trait Users[F[_]] {
  def profileFor(userId: UserId): F[UserProfile]
}

object Users {
  def apply[F[_]](implicit users: Users[F]): Users[F] = users
}

trait Orders[F[_]] {
  def ordersFor(userId: UserId): F[List[Order]]
}

object Orders {
  def apply[F[_]](implicit F: Orders[F]): Orders[F] = F
}

trait Logging[F[_]] {
  def error(e: Throwable): F[Unit]
}

object X {
  type MonadThrowable[F[_]] = MonadError[F, Throwable]
  import cats.implicits._

  def apply[F[_]: Monad: Users: Orders: Logging]: F[UserProfile] = {
    val userId = UserId("")

    import cats.implicits._

    for {
      profile <- Users[F].profileFor(userId)
      orders <- Orders[F].ordersFor(userId)
    } yield profile

  }
}

object Y {

  implicit val userRepo: Users[Option] = new Users[Option] {
    override def profileFor(userId: UserId): Option[UserProfile] = None
  }

  implicit val orderRepo = new Orders[Option] {
    override def ordersFor(userId: UserId): Option[List[Order]] = None
  }

  implicit val logger = new Logging[Option] {
    override def error(e: Throwable): Option[Unit] = None
  }

  val a: Option[UserProfile] = X[Option]

  def main(args: Array[String]): Unit = {
    a
  }
}
