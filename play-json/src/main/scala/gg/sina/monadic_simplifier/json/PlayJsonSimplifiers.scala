package gg.sina.monadic_simplifier.json

import gg.sina.monadic_simplifier.Simplifiers.Step
import play.api.libs.json.{JsError, JsResult, JsSuccess}

import scala.concurrent.Future
import scala.language.implicitConversions

object PlayJsonSimplifiers {
  implicit class JsResultOps[A](jsResult: JsResult[A]) {
    def ?| : Step[A] = Step(Future.successful(jsResult match {
    /**
     * Convert to [[Step]]. [[JsError]] will be converted to a [[JsResult.Exception]].
     *
     * @return [[Step]] analogous to this object.
     */
      case JsSuccess(result, _) => Right(result)
      case error: JsError => Left(JsResult.Exception(error))
    }))
  }
}
