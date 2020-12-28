package io.konad.laws

import io.konad.hkt.ApplicativeFunctorKind
import io.konad.hkt.FunctorKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.checkAll

open class ApplicativeFunctorLaws<F>(private val pure: (Any) -> ApplicativeFunctorKind<F, Any>): StringSpec({

    "Maybe respects the first applicative law (identity)"{
        val id = { x: Any -> x }
        checkAll<Any> { v -> pure(v).apK(pure(id)) shouldBe pure(v) }
    }

    "Maybe respects the second applicative law (Homomorphism)"{
        val f = { x: Double -> x + 6 }
        checkAll<Double> { v -> pure(v).apK(pure(f)) shouldBe pure(f(v)) }
    }

    "Maybe respects the third applicative law (Interchange)"{
        val f = { x: Double -> x + 6 }
        val u = pure(f)

        checkAll<Double> { v -> pure(v).apK(u) shouldBe u.apK(pure { f2: (Double) -> Double -> f2(v) }) }
    }

    "Maybe respects the fourth applicative law (Composition)"{
        val compose = { f: (Double) -> String -> { g: (Int) -> Double -> { a: Int -> f(g(a)) } } }
        val g = { x: Double -> x.toString() }
        val f = { x: Int -> x + 6.0 }
        val u = pure(g)
        val v = pure(f)

        checkAll<Int> { w ->
            pure(w).apK(v.apK(u.apK(pure(compose)))) shouldBe pure(w).apK(v).apK(u)
        }
    }


})
