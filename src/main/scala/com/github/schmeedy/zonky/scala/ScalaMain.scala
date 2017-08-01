package com.github.schmeedy.zonky.scala

import java.nio.charset.{Charset, StandardCharsets}

import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.util.FastFuture
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.github.schmeedy.zonky.java.{Reporter, Loan => JLoan}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser._

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class Loan(id: Int, name: String, url: String) {
  def asJava: JLoan = new JLoan(id, name, url)
}

object ScalaMain extends App {
  private val CHECK_INTERVAL = 5 minutes

  // setting things up
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val reporter = Reporter.CONSOLE
  val httpClient = HttpClient()

  /**
    * Fetches a single page of most recent loans
    */
  def fetchMostRecentLoans(pageNo: Int): Future[Seq[Loan]] = {
    val request = HttpRequest(
      uri = "https://api.zonky.cz/loans/marketplace",
      headers = immutable.Seq(
        `User-Agent`("ZebraMan/0.1 (https://github.com/schmeedy/zebraman)"),
        RawHeader("X-Page", pageNo.toString),
        RawHeader("X-Order", "-datePublished")
      )
    )
    httpClient.executeExpectJson[Seq[Loan]](request)
  }

  // construct a stream source of recent loans across all pages
  val pages: Source[Seq[Loan], NotUsed] = Source(Stream.from(0)).mapAsync(1)(fetchMostRecentLoans)
  val loans: Source[Loan, NotUsed] = pages.mapConcat(ls => immutable.Seq(ls:_*))

  // check which loan is currently the last one and setup periodic checks
  loans.take(1).runWith(Sink.foreach { lastLoan =>
    reporter.timerStarted(lastLoan.asJava, CHECK_INTERVAL.toMillis)
    system.actorOf(Props(new NewLoanChecker(loans, CHECK_INTERVAL, lastLoan.id)))
  })
}

/**
  * A simple actor that remembers the last loan it has seen so far and checks for new loans periodically
  */
class NewLoanChecker(loans: Source[Loan, NotUsed], checkFrequency: FiniteDuration, var lastSeenId: Int)
                    (implicit mat: Materializer, reporter: Reporter) extends Actor {

  import NewLoanChecker._
  import context.dispatcher

  private val timer = context.system.scheduler.schedule(checkFrequency, checkFrequency, self, CheckingTime)

  override def receive: Receive = {
    case CheckingTime =>
      val newLoans = loans.takeWhile(_.id != lastSeenId).runWith(Sink.seq)
      newLoans.map(NewLoans) pipeTo self

    case NewLoans(nls) =>
      val newLoansJava = nls.map(_.asJava).asJava
      reporter.newLoansFetched(newLoansJava)
      nls.lastOption.foreach { lastLoan =>
        lastSeenId = lastLoan.id
      }

    case Status.Failure(err) => err.printStackTrace()
  }

  override def postStop(): Unit = timer.cancel()
}
object NewLoanChecker {
  object CheckingTime
  case class NewLoans(loans: Seq[Loan])
}

/**
  * Client APIs in Akka HTTP are a bit low-level. This is just a simple facade to make
  * HTTP requests for JSON payloads straightforward...
  */
case class HttpClient(implicit system: ActorSystem, mat: Materializer) {
  import system.dispatcher

  private val ENTITY_READ_TIMEOUT = 5 seconds

  case class HttpRequestException(message: String) extends RuntimeException

  def executeExpectJson[T: Decoder](request: HttpRequest): Future[T] =
    execute(request).flatMap(stringEntity(_)).flatMap { jsonString =>
      decode[T](jsonString) match {
        case Right(t) => FastFuture.successful(t)
        case Left(decodingErr) => FastFuture.failed(decodingErr.getCause)
      }
    }

  def execute(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request).flatMap { response =>
      if (response.status.isFailure()) FastFuture.failed(HttpRequestException(response.status.reason()))
      else FastFuture.successful(response)
    }

  private def stringEntity(response: HttpResponse, charset: Charset = StandardCharsets.UTF_8): Future[String] = {
    val strict = response.entity.toStrict(ENTITY_READ_TIMEOUT)
    strict.map(_.data.decodeString(StandardCharsets.UTF_8))
  }
}