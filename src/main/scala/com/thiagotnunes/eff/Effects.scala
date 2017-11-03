package com.thiagotnunes.eff

import java.sql.Connection

import cats.data.Reader
import com.thiagotnunes.models.Thingy
import com.twitter.util.{Future, FuturePool}
import org.atnos.eff.MemberIn.|=
import org.atnos.eff.addon.twitter.TwitterTimedFuture
import org.atnos.eff.addon.twitter.future._
import org.atnos.eff.all._
import org.atnos.eff.concurrent.Scheduler
import org.atnos.eff.syntax.addon.twitter.future._
import org.atnos.eff.syntax.all._
import org.atnos.eff.{Eff, ExecutorServices, Fx}

class Imperative() {
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

class WithReader {
  type ReaderConnection[A] = Reader[Connection, A]
  type Stack = Fx.fx1[ReaderConnection]

  type _readerConnection[R] = ReaderConnection |= R

  def run(connection: Connection): Thingy = {
    def program[R: _readerConnection]: Eff[R, Thingy] = {
      for {
        connection <- ask[R, Connection]
      } yield {
        val thingy = findThingy(1)(connection)
        insertThingy(thingy)(connection)
        thingy
      }
    }


    program[Stack].runReader(connection).run
  }

  def findThingy(id: Int)(connection: Connection): Thingy = {
    Thingy() // this could throw an exception in real database calls
  }

  def insertThingy(thingy: Thingy)(connection: Connection): Unit = {
    () // this could throw an exception in real database calls
  }
}

class WithReaderEither {
  type ReaderConnection[A] = Reader[Connection, A]
  type EitherString[A] = Either[String, A]
  type Stack = Fx.fx2[ReaderConnection, EitherString]

  type _readerConnection[R] = ReaderConnection |= R
  type _eitherString[R] = EitherString |= R

  def run(connection: Connection): Either[String, Thingy] = {
    def program[R: _readerConnection: _eitherString]: Eff[R, Thingy] = {
      for {
        connection <- ask[R, Connection]

        thingy <- findThingy(1)(connection)
        _ <- insertThingy(thingy)(connection)
      } yield {
        thingy
      }
    }

    program[Stack].runReader(connection).runEither.run
  }

  def findThingy[R: _readerConnection : _eitherString](id: Int)(connection: Connection): Eff[R, Thingy] = {
    right[R, String, Thingy](Thingy()) // handles exception with either
  }

  def insertThingy[R: _readerConnection : _eitherString](thingy: Thingy)(connection: Connection): Eff[R, Unit] = {
    right[R, String, Unit](())
  }
}

class WithReaderEitherOption {
  type ReaderConnection[A] = Reader[Connection, A]
  type EitherString[A] = Either[String, A]
  type Stack = Fx.fx3[ReaderConnection, EitherString, Option]

  type _readerConnection[R] = ReaderConnection |= R
  type _eitherString[R] = EitherString |= R

  def run(connection: Connection): Either[String, Option[Thingy]] = {
    def program[R: _readerConnection: _eitherString: _option]: Eff[R, Thingy] = {
      for {
        connection <- ask[R, Connection]

        thingy <- findThingy(1)(connection)

        _ <- insertThingy(thingy)(connection)
      } yield {
        thingy
      }
    }

    program[Stack].runReader(connection).runOption.runEither.run
  }

  def findThingy[R: _readerConnection : _eitherString: _option](id: Int)(connection: Connection): Eff[R, Thingy] = {
    for {
      optionResult <- fromEither(Right(Some(Thingy())))
      result <- fromOption(optionResult)
    } yield {
      result
    }
  }

  def insertThingy[R: _readerConnection : _eitherString: _option](thingy: Thingy)(connection: Connection): Eff[R, Unit] = {
    right[R, String, Unit](())
  }
}

class WithReaderFutureEitherOption {
  type ReaderConnection[A] = Reader[Connection, A]
  type EitherString[A] = Either[String, A]
  type Stack = Fx.fx4[ReaderConnection, TwitterTimedFuture, EitherString, Option]

  type _readerConnection[R] = ReaderConnection |= R
  type _eitherString[R] = EitherString |= R

  implicit val scheduler: Scheduler = ExecutorServices.schedulerFromGlobalExecutionContext
  implicit val futurePool: FuturePool = FuturePool.unboundedPool

  def run(connection: Connection): Future[Either[String, Option[Thingy]]] = {
    def program[R: _readerConnection: _future: _eitherString: _option]: Eff[R, Thingy] = {
      for {
        connection <- ask[R, Connection]

        thingy <- findThingy(1)(connection)

        _ <- insertThingy(thingy)(connection)
      } yield {
        thingy
      }
    }

    program[Stack].runReader(connection).runOption.runEither.runAsync
  }

  def findThingy[R: _readerConnection: _future: _eitherString: _option](id: Int)(connection: Connection): Eff[R, Thingy] = {
    for {
      eitherOptionResult <- futureDelay(Right(Some(Thingy())))
      optionResult <- fromEither(eitherOptionResult)
      result <- fromOption(optionResult)
    } yield {
      result
    }
  }

  def insertThingy[R: _readerConnection: _future: _eitherString: _option](thingy: Thingy)(connection: Connection): Eff[R, Unit] = {
    for {
      eitherResult <- futureDelay(Right(()))
      result <- fromEither(eitherResult)
    } yield {
      result
    }
  }
}
