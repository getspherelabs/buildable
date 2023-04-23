package io.behzod.buildable

import io.spherelabs.buildableannotations.BuildableComponent

@BuildableComponent
class Student: Person {
    override fun sleep() {
       println("Student sleeps 5 hours")
    }
}