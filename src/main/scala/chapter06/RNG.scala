package chapter06

trait RNG {

  def nextInt: (Int, RNG)

}

object RNG {

//  case class State[S,+A](run: S => (A,S))

  type State[S,+A] = S => (A,S)

  type Rand[+A] = State[RNG, A]
//  type Rand[+A] = RNG => (A, RNG)

  def int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] = rng => (a, rng)


  //  def map[S,A,B](a: S => (A,S))(f: A => B): S => (B,S) =
  def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      ( f(a), rng2 )
    }

  def nonNegativeEven: Rand[Int] = map(nonNegativeInt)(i => i - i % 2)

  /**
    * Use map to reimplement double in a more elegant way.
    */
  def doubleViaMap: Rand[Double] = map(nonNegativeInt)(i => i / ( Int.MaxValue.toDouble + 1 ) )

  /**
    * Write the implementation of map2 based on the following signature. This function
    * takes two actions, ra and rb, and a function f for combining their results, and returns
    * a new action that combines them.
    */
  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a, rng1) = ra(rng)
      val (b, rng2) = rb(rng1)
      ( f(a, b) , rng2)
    }

  def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2(ra, rb)((_,_))

  def randIntDouble: Rand[(Int, Double)] =
    both(int, double)

  def randDoubleInt: Rand[(Double, Int)] =
    both(double, int)

  /**
    * Hard: If you can combine two RNG transitions, you should be able to combine a whole
    * list of them. Implement sequence for combining a List of transitions into a single
    * transition. Use it to reimplement the ints function you wrote before. For the latter,
    * you can use the standard library function List.fill(n)(x) to make a list with x repeated n times.
    */
  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs match {
      case Nil      => unit(Nil)
      case h :: t   => cons(h, sequence(t))
    }

  /**
    * Tail recursive implementation of sequence
    */
  def sequence2[A](fs: List[Rand[A]]): Rand[List[A]] = {

    @annotation.tailrec
    def go(fs: List[Rand[A]], rs: Rand[List[A]]): Rand[List[A]] =
      fs match {
        case Nil    => rs
        case h :: t => go(t, cons(h, rs))
      }

    go(fs, unit(Nil))
  }

  /**
    * Implement flatMap, and then use it to implement nonNegativeLessThan.
    * flatMap allows us to generate a random A with Rand[A], and then take that A and
    * choose a Rand[B] based on its value. In nonNegativeLessThan, we use it to choose
    * whether to retry or not, based on the value generated by nonNegativeInt.
    */
  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r) = f(rng)
      g(a)(r)
    }

  def cons[A](r: Rand[A], rs: Rand[List[A]]): Rand[List[A]] =
    map2(r, rs)(_::_)

  def toList[A](r: Rand[A]): Rand[List[A]] =
    map(r)(List(_))

  def toList[A](ra: Rand[A], rb: Rand[A]): Rand[List[A]] =
    map2(ra, rb)(_::List(_))

  def nonNegativeLessThan(n: Int): Rand[Int] = {
    flatMap(nonNegativeInt) { i =>
      val mod = i % n
      if (i + (n-1) - mod >= 0) unit(mod) else nonNegativeLessThan(n)
    }
  }

  /**
    * Reimplement map and map2 in terms of flatMap. The fact that this is possible is what
    * we're referring to when we say that flatMap is more powerful than map and map2.
    */
  def mapViaFlatMap[A, B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(a => unit(f(a)))

  def map2ViaFlatMap[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => flatMap(rb)(b => unit(f(a, b))))

  def rollDie: Rand[Int] = map(nonNegativeLessThan(6))(_+1)

  /**
    * Write a function that uses RNG.nextInt to generate a random integer between 0 and
    * Int.maxValue (inclusive). Make sure to handle the corner case when nextInt returns
    * Int.MinValue, which doesn't have a non-negative counterpart.
    */
  def nonNegativeInt: Rand[Int] = rng => {
    val (n, rng1) = rng.nextInt

    if (n >= 0)
      (n, rng1)
    else if (n == Int.MinValue)
      (0, rng1)
    else
      (n + Int.MaxValue, rng1)
  }

  /**
    * Write a function to generate a Double between 0 and 1, not including 1. Note: You can
    * use Int.MaxValue to obtain the maximum positive integer value, and you can use
    * x.toDouble to convert an x: Int to a Double.
    */
  def double(rng: RNG): (Double, RNG) = {
    val (nn, rng1) = nonNegativeInt(rng)

    ( nn / ( Int.MaxValue.toDouble + 1 ), rng1 )
  }

  def applyFn[A](rng: RNG, fn: Int => A): (A, RNG) = {
    val (n, r) = rng.nextInt
    ( fn(n), r )
  }

  /**
    * Write functions to generate an (Int, Double) pair, a (Double, Int) pair, and a
    * (Double, Double, Double) 3-tuple. You should be able to reuse the functions you’ve
    * already written.
    */

  def intDouble(rng: RNG): ((Int,Double), RNG) = {
    val (i, r) = rng.nextInt
    val (d, r2) = double(r)
    ( (i, d), r2)
  }

  def doubleInt(rng: RNG): ((Double,Int), RNG) = {
    val ( (i, d), r ) = intDouble(rng)
    ( (d, i), r )
  }

  def double3(rng: RNG): ((Double,Double,Double), RNG) = {
    val ( d1, r1 ) = double(rng)
    val ( d2, r2 ) = double(r1)
    val ( d3, r3 ) = double(r2)
    ( (d1, d2, d3), r3 )
  }

  /**
   * Write a function to generate a list of random integers.
   */
  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
    var rng1 = rng
    val list = scala.collection.mutable.ListBuffer[Int]()
    (1 to count).foreach(_ => {
      val (i, r) = rng1.nextInt
      rng1 = r
      list += i
    })
    (list.toList, rng1)
  }
}

import RNG._

object RGNApp {

  def main(args: Array[String]): Unit = {
    println(nonNegativeLessThan(6)(new SimpleRNG(5))._1)
    println(rollDie(new SimpleRNG(5))._1)
  }

}
