// COMPILATION_ERRORS

typealias f = (((S).() -> S).() -> S)
typealias f = ((T.() -> S).() -> S)
typealias f = ((T.T.() -> S).() -> S)
typealias f = ((T<A, B>.T<x>.() -> S).() -> S)
typealias f = (((S).() -> S).() -> S)

typealias f =  @[a] (@[a] ((S).() -> S).() -> S)
typealias f = @[a] (@[a] (T.() -> S).() -> S)
typealias f = @[a] (@[a] (T<A, B>.() -> S).() -> S)
typealias f = @[a] (@[a] ((S).() -> S).() -> S)