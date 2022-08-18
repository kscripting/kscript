#!/usr/bin/env kscript

@file:DependsOn("org.jetbrains.kotlin:kotlin-reflect:1.1.60")
@file:DependsOn("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

@file:CompilerOptions("-jvm-target 1.8")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject


val mapper: ObjectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

data class User(val id: String, val name: String)

fun getUsers(args: Array<String>) {
    val (_, _, result) = Fuel.get("https://jsonplaceholder.typicode.com/users").responseObject<User>()
}

println("hoo_ray")
