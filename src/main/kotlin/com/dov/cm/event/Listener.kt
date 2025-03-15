package com.dov.cm.event

import java.lang.reflect.Method

class Listener(
    private val method: Method,
    private val obj: Any,
    private val event: Class<*>,
    // Change from var to private val for the backing field
    private val _priority: Int
) {
    // Define a property that uses the backing field
    // This way Kotlin won't generate a conflicting getter
    var priority: Int
        get() = _priority
        private set(value) {} // Read-only property now

    fun getMethod(): Method = method

    fun getObject(): Any = obj

    fun getEvent(): Class<*> = event

    // Either change the name of this method
    fun retrievePriority(): Int = _priority

    // Or you can remove it entirely if not needed for Java interop
    // and just use the Kotlin property
}