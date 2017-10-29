package com.thiagotnunes.monad_transformers

import java.sql.Connection

import cats.{Id, Monad}
import cats.data._
import com.thiagotnunes.models.Thingy
import com.twitter.util.Future
import cats.implicits._

class Imperative {
  def run(connection: Connection): Thingy = {
    val thingy = findThingy(1)(connection)
    insertThingy(thingy)(connection)

    thingy
  }

  def findThingy(id: Int)(connection: Connection): Thingy = {
    Thingy() // this could throw an exception in real database calls
  }

  def insertThingy(thingy: Thingy)(connection: Connection): Unit = {
    () // this could throw an exception in real database calls
  }
}


// Reader makes program construction effect free and
// we can inject connection later
class WithReader {
  def run(connection: Connection): Thingy = {
    val program = for {
      thingy <- findThingy(1)
      _ <- insertThingy(thingy)
    } yield {
      thingy
    }

    program.run(connection)
  }

  def findThingy(id: Int): Kleisli[Id, Connection, Thingy] = {
    Kleisli[Id, Connection, Thingy](connection =>
      Thingy() // this could throw an exception in real database calls
    )
  }

  def insertThingy(thingy: Thingy): Kleisli[Id, Connection, Unit] = {
    Kleisli[Id, Connection, Unit](connection =>
      () // this could throw an exception in real database calls
    )
  }
}

// Handles errors with either
class WithReaderEither {
  def run(connection: Connection): Either[String, Thingy] = {
    val program = for {
      thingy <- findThingy(1)
      _ <- insertThingy(thingy)
    } yield {
      thingy
    }

    program.run(connection)
  }

  def findThingy(id: Int): Kleisli[Either[String, ?], Connection, Thingy] = {
    Kleisli[Either[String, ?], Connection, Thingy](connection =>
      Right(Thingy()) // handle exceptions with either
    )
  }

  def insertThingy(thingy: Thingy): Kleisli[Either[String, ?], Connection, Unit] = {
    Kleisli[Either[String, ?], Connection, Unit](connection =>
      Right(()) // handle exceptions with either
    )
  }
}

// Handle missing values with option
class WithReaderEitherOption {
  def run(connection: Connection): Either[String, Option[Thingy]] = {
    val program = for {
      thingy <- findThingy(1)
      _ <- insertThingy(thingy)
    } yield {
      thingy
    }

    program
      .run(connection)
      .value
  }

  def findThingy(id: Int): Kleisli[OptionT[Either[String, ?], ?], Connection, Thingy] = {
    Kleisli((connection: Connection) =>
      OptionT[Either[String, ?], Thingy](Right(Some(Thingy()))) // handle no value with option
    )
  }

  def insertThingy(thingy: Thingy): Kleisli[OptionT[Either[String, ?], ?], Connection, Unit] = {
    Kleisli((connection: Connection) =>
      OptionT[Either[String, ?], Unit](Right(None)) // handle no value with option
    )
  }
}

// Makes computation asynchronous with future
class WithReaderFutureEitherOption {
  implicit val twitterFutureMonad: Monad[Future] = new Monad[Future] {
    override def pure[A](x: A): Future[A] = {
      Future.value(x)
    }

    override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = {
      fa.flatMap(f)
    }

    override def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] = {
      f(a).flatMap {
        case Left(a1) => tailRecM(a1)(f)
        case Right(b) => Future.value(b)
      }
    }
  }

  def run(connection: Connection): Future[Either[String, Option[Thingy]]] = {
    val program = for {
      thingy <- findThingy(1)
      _ <- insertThingy(thingy)
    } yield {
      thingy
    }

    program
      .run(connection)
      .value
      .value
  }

  def findThingy(id: Int): Kleisli[OptionT[EitherT[Future, String, ?], ?], Connection, Thingy] = {
    Kleisli((connection: Connection) =>
      OptionT(
        EitherT[Future, String, Option[Thingy]](
          Future.value(Right(Some(Thingy()))) // makes database call asynchronous
        )
      )
    )
  }

  def insertThingy(thingy: Thingy): Kleisli[OptionT[EitherT[Future, String, ?], ?], Connection, Unit] = {
    Kleisli((connection: Connection) =>
      OptionT(
        EitherT[Future, String, Option[Unit]](
          Future.value(Right(None)) // makes database call asynchronous
        )
      )
    )
  }
}
