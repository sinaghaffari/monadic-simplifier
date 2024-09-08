package gg.sina.monadic_simplifier

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Simplifiers {
  implicit class FutureEitherThrowableOps[A](futureEither: Future[Either[Throwable, A]]) {
    /**
     * Convert to [[Step]].
     *
     * @return [[Step]] analogous to this object
     */
    def ?|(implicit executionContext: ExecutionContext) : Step[A] = for {
      either <- new FutureOps(futureEither).?|
      result <- new EitherThrowableOps(either).?|
    } yield result
  }

  implicit class FutureEitherOps[A, B](futureEither: Future[Either[B, A]]) {
    /**
     * Convert to [[Step]].
     *
     * @param fn Function mapping the left side of the [[Either]] within the [[Future]] to a [[Throwable]].
     * @return [[Step]] analogous to this object.
     */
    def ?|(fn: B => Throwable)(implicit ec: ExecutionContext): Step[A] = for {
      either <- new FutureOps(futureEither).?|
      result <- new EitherOps(either).?|(fn)
    } yield result
  }

  implicit class FutureOps[A](future: Future[A]) {
    /**
     * Convert to [[Step]].
     *
     * @return [[Step]] analogous to this object
     */
    def ?|(implicit executionContext: ExecutionContext): Step[A] = {
      val promise = Promise[Either[Throwable, A]]()
      future.onComplete {
        case Success(v) => promise.success(Right(v))
        case Failure(e) => promise.success(Left(e))
      }
      Step(promise.future)
    }
  }

  implicit class OptionOps[A](option: Option[A]) {
    /**
     * Convert to [[Step]].
     *
     * @param ex The [[Throwable]] that this [[Step]] should fail with if the [[Option]] is [[None]].
     * @return [[Step]] analogous to this object
     */
    def ?|(ex: Throwable): Step[A] = Step(Future.successful(option.toRight(ex)))
  }

  implicit class FutureOptionOps[A](futureOption: Future[Option[A]]) {
    /**
     * Convert to [[Step]].
     *
     * @param ex The [[Throwable]] that this [[Step]] should fail with if the [[Option]] within the [[Future]] is [[None]].
     * @return [[Step]] analogous to this object.
     */
    def ?|(ex: Throwable)(implicit executionContext: ExecutionContext): Step[A] = for {
      option <- new FutureOps(futureOption).?|
      result <- new OptionOps(option).?|(ex)
    } yield result
  }

  implicit class EitherThrowableOps[A](either: Either[Throwable, A]) {
    /**
     * Convert to [[Step]].
     *
     * @return [[Step]] analogous to this object.
     */
    def ?| : Step[A] = Step(Future.successful(either))
  }

  implicit class EitherOps[A, B](either: Either[B, A]) {
    /**
     * Convert to [[Step]].
     *
     * @param fn Function mapping the left side of the [[Either]] to a [[Throwable]].
     * @return [[Step]] analogous to this object.
     */
    def ?|(fn: B => Throwable): Step[A] = Step(Future.successful(either.left.map(fn)))
  }

  implicit class TryOps[A](`try`: Try[A]) {
    /**
     * Convert to [[Step]].
     *
     * @return [[Step]] analogous to this object.
     */
    def ?| : Step[A] = Step(Future.successful(`try`.toEither))
  }

  implicit class BooleanOps(boolean: Boolean) {
    /**
     * Convert to [[Step]]. This conversion can place a condition on the for-yield passing or failing.
     *
     * @param ex The [[Throwable]] that this [[Step]] should fail with if the [[Boolean]] is false.
     * @return A [[Step]][Unit] that will succeed only if the [[Boolean]] is true.
     */
    def ?|(ex: Throwable): Step[Unit] = Step(Future.successful(if (boolean) Right(()) else Left(ex)))
  }

  implicit class FutureBooleanOps(futureBoolean: Future[Boolean]) {
    /**
     * Convert to [[Step]]. This conversion can place a condition on the for-yield passing or failing.
     *
     * @param ex The [[Throwable]] that this [[Step]] should fail with if the [[Boolean]] within the [[Future]] is false.
     * @return A [[Step]][Unit] that will succeed only if the [[Boolean]] within the [[Future]] is true.
     */
    def ?|(ex: Throwable)(implicit executionContext: ExecutionContext): Step[Unit] = for {
      boolean <- new FutureOps(futureBoolean).?|
      result <- new BooleanOps(boolean).?|(ex)
    } yield result
  }

  final case class Step[+A](run: Future[Either[Throwable, A]]) {
    def map[B](f: A => B)(implicit ec: ExecutionContext): Step[B] =
      copy(run = run.map(_.map(f)))

    def flatMap[B](f: A => Step[B])(implicit ec: ExecutionContext): Step[B] =
      copy(run = run.flatMap(_.fold(
        err => Future.successful(Left[Throwable, B](err)),
        succ => f(succ).run
      )))

    def withFilter(p: A => Boolean)(implicit ec: ExecutionContext): Step[A] =
      copy(run = run.filter {
        case Right(a) if p(a) => true
        case Left(_) => true
        case _ => false
      })
  }

  implicit def stepToFuture[A](step: Step[A])(implicit ec: ExecutionContext): Future[A] = step.run.map(_.toTry.get)
}
