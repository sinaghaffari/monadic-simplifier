# MonadicSimplifier

This library enabled usage of multiple kinds of common monads in a single for-comprehension block. It is heavily inspired by [Kanaka-io/play-monadic-actions](https://github.com/Kanaka-io/play-monadic-actions).

## Motivation
I often find myself needing to use many different types of monads to best represent the data I'm working with. Mixing monads introduces a readability issue, however. Consider the following example:


```scala 3
object Example extends App {
  def readUserIdFromFile: Option[String] = ???
  def getUser(id: String): Future[User] = ???
  def buyBeer(user: User): Try[Beer] = ???
  def buyCigarettes(user: User): Either[UnderageCustomerException, Cigarettes] = ???

  def purchaseControlledSubstances: Future[(Beer, Cigarettes)] = {
    readUserIdFromFile match {
      case None => Future.failed(NoUserIdException("..."))
      case Some(id) => getUser(id).map { user =>
        (buyBeer(user).get, buyCigarettes(user).toTry.get)
      }
    }
  }
}
```

This roughly written example should be straightforward, but isn't very readable.

This library adds a wrapper monad `Step[A]`, which is internally represented as a `Future[Either[Throwable, A]]`.
It introduces an operator `?|` to a few commonly used monads, which converts them to a `Step`.
The idea behind `?|` is that the left-hand side contains the successful value, and the right-hand side deals with the failure. Some monads wholly contain their failure cases (Futures, Try, and Either are examples), so in those cases `?|` doesn't take any parameters and will just fail with the error contained within its monad.

The example above can be rewritten as:
```scala 3
import gg.sina.monadic_simplifier.Simplifiers.*

object SimplifiedExample extends App {
  def readUserIdFromFile: Option[String] = ???
  def getUser(id: String): Future[User] = ???
  def buyBeer(user: User): Try[Beer] = ???
  def buyCigarettes(user: User): Either[UnderageCustomerException, Cigarettes] = ???

  def purchaseControlledSubstances: Future[(Beer, Cigarettes)] = for {
    id <- readUserIdFromFile ?| NoUserIdException("...")
    user <- getUser(id).?|
    beer <- getBeer(user).?|
    cigatettes <- buyCigarettes(user).?|
  } yield (beer, cigatettes)
}
```

Note: `Step[A]` is also implicitly be converted to `Future[A]`, which is why the example above is able to return a `Step[(Beer, Cigarettes)]` even though it expects `Future[(Beer, Cigarettes)]`.

## Supported Conversions

| Defining Module                | Source Type                    | Type of right hand side | Type of the extracted value |
|--------------------------------|--------------------------------|-------------------------|-----------------------------|
| `monadic-simplifier`           | `Boolean`                      | `Throwable`             | `Unit`                      |
| `monadic-simplifier`           | `Option[A]`                    | `Throwable`             | `A`                         |
| `monadic-simplifier`           | `Try[A]`                       | N/A                     | `A`                         |
| `monadic-simplifier`           | `Future[Either[Throwable, A]]` | N/A                     | `A`                         |
| `monadic-simplifier`           | `Future[Either[B, A]]`         | `B => Throwable`        | `A`                         |
| `monadic-simplifier`           | `Future[A]`                    | N/A                     | `A`                         |
| `monadic-simplifier`           | `Future[Option[A]]`            | `Throwable`             | `A`                         |
| `monadic-simplifier`           | `Either[Throwable, A]`         | N/A                     | `A`                         |
| `monadic-simplifier`           | `Either[B, A]`                 | `B => Throwable`        | `A`                         |
| `monadic-simplifier`           | `Future[Boolean]`              | `Throwable`             | `Unit`                      |
| `monadic-simplifier-play-json` | `JsResult[A]`                  | N/A                     | `A`                         |
| `monadic-simplifier-play-json` | `JsResult[A]`                  | `JsError => Throwable`  | `A`                         |