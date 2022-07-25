package com.geekydroid.spamfilter

data class Message(
    val _id:String = "",
    val thread_id:String = "",
    val address:String = "",
    val person:Int = -1,
    val subject:String = "",
    val body:String = "",
    val phoneId:String = ""
)