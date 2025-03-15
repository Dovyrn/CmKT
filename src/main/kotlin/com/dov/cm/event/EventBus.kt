package com.dov.cm.event

import java.util.concurrent.CopyOnWriteArrayList
import java.lang.reflect.Method

class EventBus {
    private val listeners: CopyOnWriteArrayList<Listener> = CopyOnWriteArrayList()

    init {
        println("[EventBus] Initialized")
    }

    fun registerListener(obj: Any) {
        println("[EventBus] Registering listener: ${obj.javaClass.simpleName}")
        discoverListeners(obj)
    }

    fun unregisterListener(obj: Any) {
        println("[EventBus] Unregistering listener: ${obj.javaClass.simpleName}")
        listeners.removeIf { it.getObject() == obj }
    }

    private fun discoverListeners(obj: Any) {
        val clazz = obj.javaClass
        var methodsFound = 0

        // Look for methods with EventListener annotation
        clazz.declaredMethods.forEach { method ->
            method.isAccessible = true // Make private methods accessible

            if (method.isAnnotationPresent(EventListener::class.java)) {
                val parameterTypes = method.parameterTypes

                // Safety check - ensure method has exactly one parameter
                if (parameterTypes.size != 1) {
                    println("[EventBus] WARNING: Method ${method.name} in ${clazz.simpleName} has @EventListener but doesn't have exactly one parameter")
                    return@forEach
                }

                // Make sure the parameter is an Event or subclass
                if (!Event::class.java.isAssignableFrom(parameterTypes[0])) {
                    println("[EventBus] WARNING: Method ${method.name} in ${clazz.simpleName} has @EventListener but parameter is not an Event")
                    return@forEach
                }

                val priority = try {
                    method.getAnnotation(EventListener::class.java).priority.ordinal
                } catch (e: Exception) {
                    println("[EventBus] Error getting priority: ${e.message}")
                    0 // Default priority
                }

                listeners.add(Listener(method, obj, parameterTypes[0], priority))
                methodsFound++
                println("[EventBus] Registered method: ${method.name} for event ${parameterTypes[0].simpleName}")
            }
        }

        println("[EventBus] Found $methodsFound event methods in ${clazz.simpleName}")
    }

    fun invoke(event: Event): Boolean {
        println("[EventBus] Invoking event: ${event.javaClass.simpleName}")

        val eventClass = event.javaClass
        var listenerCount = 0

        // Create a copy to avoid concurrent modification
        CopyOnWriteArrayList(listeners).forEach { listener ->
            // Check if the event is assignable to the listener's event type
            if (listener.getEvent().isAssignableFrom(eventClass)) {
                try {
                    listener.getMethod().invoke(listener.getObject(), event)
                    listenerCount++
                } catch (exception: Exception) {
                    println("[EventBus] Error invoking listener: ${exception.message}")
                    exception.printStackTrace()
                }
            }
        }

        println("[EventBus] Invoked event on $listenerCount listeners")
        return event.isCancelled
    }
}