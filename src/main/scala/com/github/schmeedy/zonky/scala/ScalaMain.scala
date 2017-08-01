package com.github.schmeedy.zonky.scala

import java.nio.charset.{Charset, StandardCharsets}
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.util.FastFuture
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
import scala.util.{Failure, Success}

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

  import system.dispatcher

  /**
    * Fetches a single page of most recent loans
    */
  def fetchMostRecentLoans(since: Option[LocalDateTime], pageNo: Int): Future[Seq[Loan]] = {
    val filter = since.map(dt => s"?datePublished__gt=$dt").getOrElse("")
    val request = HttpRequest(
      uri = s"https://api.zonky.cz/loans/marketplace$filter",
      headers = immutable.Seq(
        `User-Agent`("ZebraMan/0.1 (https://github.com/schmeedy/zebraman)"),
        RawHeader("X-Page", pageNo.toString),
        RawHeader("X-Order", "-datePublished"),
      )
    )
    httpClient.executeExpectJson[Seq[Loan]](request)
  }

  def loansSince(since: Option[LocalDateTime]): Source[Loan, NotUsed] = {
    // construct a stream source of recent loans across all pages
    val allPages = Source(Stream.from(0)).mapAsync(1)(fetchMostRecentLoans(since, _))
    allPages.takeWhile(_.nonEmpty).mapConcat(ls => immutable.Seq(ls:_*))
  }

  // check which loan is currently the last one and setup periodic checks
  loansSince(None).take(1).runWith(Sink.foreach { lastLoan =>
    system.scheduler.schedule(CHECK_INTERVAL, CHECK_INTERVAL) {
      val since = LocalDateTime.now().minusSeconds(CHECK_INTERVAL.toSeconds)
      loansSince(Some(since)).runWith(Sink.seq) onComplete {
        case Success(loans) => reporter.newLoansFetched(loans.map(_.asJava).asJava)
        case Failure(e) => e.printStackTrace()
      }
    }
    reporter.timerStarted(lastLoan.asJava, CHECK_INTERVAL.toMillis)
  })
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