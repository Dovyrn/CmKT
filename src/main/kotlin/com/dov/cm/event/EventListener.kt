package com.dov.cm.event

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.RUNTIME)
annotation class EventListener(
    val priority: Priority = Priority.DEFAULT
)