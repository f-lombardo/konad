package io.konad

import io.konad.Maybe.Companion.maybe
import io.konad.Maybe.Companion.nullable
import io.konad.applicative.builders.ap
import io.konad.applicative.builders.flatten
import io.konad.applicative.builders.map
import io.konad.exceptions.ResultException
import io.konad.hkt.ApplicativeFunctorKind
import io.konad.hkt.FunctorKind
import io.konad.hkt.Kind
import io.konad.hkt.MonadKind

sealed class Result<out T>: ApplicativeFunctorKind<ResultOf, T>, MonadKind<ResultOf, T>{

    data class Ok<out T>(val value: T): Result<T>()
    data class Errors(val error: Error, val prev: Errors? = null): Result<Nothing>(){

        constructor(description: String) : this(Error(description))

        val description = toList().joinToString(" - " ) { it.description }

        fun toList(): Collection<Error> = (prev
            ?.run { toList() }
            ?: emptyList()) +
            listOf(error)
    }

    companion object{
        fun <T> pure(value: T): Result<T> = Ok(value)
    }

    inline fun <R> map(fn: (T) -> R): Result<R> = flatMap { Ok(fn(it)) }

    inline fun <R> flatMap(fn: (T) -> Result<R>): Result<R> = when(this){
        is Ok -> fn(this.value)
        is Errors -> this
    }

    fun <R> ap(liftedFn: Result<(T) -> R>): Result<R> = when(liftedFn){
        is Ok -> map(liftedFn.value)
        is Errors -> when(this){
            is Ok -> liftedFn
            is Errors -> Errors(error, liftedFn)
        }
    }

    inline fun <R> fold(onOk: (T) -> R, onErrors: (Errors) -> R): R =
        when (this) {
            is Ok -> onOk(this.value)
            is Errors -> onErrors(this)
        }

    fun get(): T = when(this){
        is Ok -> value
        is Errors -> throw ResultException(this)
    }

    fun toMaybe(): ApplicativeFunctorKind<MaybeOf, T> = when(this){
        is Ok -> value
        is Errors -> null
    }.maybe

    override fun <R> mapK(fn: (T) -> R): FunctorKind<ResultOf, R> = map(fn)
    override fun <R> flatMapK(fn: (T) -> MonadKind<ResultOf, R>): MonadKind<ResultOf, R> = flatMap { fn(it).result }
    override fun <R> apK(liftedFn: FunctorKind<ResultOf, (T) -> R>): ApplicativeFunctorKind<ResultOf, R> = ap(liftedFn.result)
}

fun <T> Result<T>.ifError(defaultValue: T) = ifError { defaultValue }

fun <T> Result<T>.ifError(errorHandler: (Result.Errors) -> T) = when(this){
    is Result.Ok -> value
    is Result.Errors -> errorHandler(this)
}

fun <T> T?.ifNull(errorMessage: String) = this
    ?.run { Result.Ok(this) }
    ?: Result.Errors(errorMessage)

open class ResultOf
val <T> Kind<ResultOf, T>.result
    get() = this as Result<T>

fun <T> Collection<Result<T>>.flatten(): Result<Collection<T>> =
    flatten(Result.Companion::pure)
    .result

fun <T> T.ok(): Result.Ok<T> = Result.Ok(this)
fun String.error(): Result.Errors = Result.Errors(this)