package io.behzod.buildable

fun main() {
    val student = PersonFactory(PersonType.STUDENT)

    println(student.sleep())
}