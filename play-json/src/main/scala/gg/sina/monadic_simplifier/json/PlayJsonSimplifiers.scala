package gg.sina.monadic_simplifier.json

import gg.sina.monadic_simplifier.Simplifiers.Step
import play.api.libs.json.{JsError, JsResult, JsSuccess}

import scala.concurrent.Future
import scala.language.implicitConversions

object PlayJsonSimplifiers {
  implicit class JsResultOps[A](jsResult: JsResult[A]) {
    /**
     * Convert to [[Step]]. [[JsError]] will be converted to a [[JsResult.Exception]].
     *
     * @return [[Step]] analogous to this object.
     */
    def ?| : Step[A] = ?|(JsResult.Exception)

    /**
     * Convert to [[Step]].
     *
     * @param fn Function mapping a [[JsError]] to a [[Throwable]].
     * @return [[Step]] analogous to this object.
     */
    def ?|(fn: JsError => Throwable) : Step[A] = Step(Future.successful(jsResult match {
      case JsSuccess(result, _) => Right(result)
      case error: JsError => Left(fn(error))
    }))
  }
}
