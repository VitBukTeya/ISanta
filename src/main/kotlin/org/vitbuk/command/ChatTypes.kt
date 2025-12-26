package org.vitbuk.command

fun isGroupChat(type: String?): Boolean =
    type == "group" || type == "supergroup"

fun isPrivateChat(type: String?): Boolean =
    type == "private"
