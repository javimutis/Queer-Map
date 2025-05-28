package com.cursoandroid.queermap.domain.model

data class User(
    val id: String? = null,
    val name: String,
    val username: String,
    val email: String,
    val birthday: String
)