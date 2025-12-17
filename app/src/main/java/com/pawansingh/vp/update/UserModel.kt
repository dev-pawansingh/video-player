package com.pawansingh.vp.update

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePic: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
