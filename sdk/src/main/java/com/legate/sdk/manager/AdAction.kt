package com.legate.sdk.manager

sealed class AdAction {
    object Idle : AdAction()
    class Error(val msg: String) : AdAction()
    class Action(val data: String?, val msg: String) : AdAction()
}