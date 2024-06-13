package com.renown.whiskeyvictor

interface SessionAdapterClickCallback {

    fun onSessionClicked(session: Session, adapterPosition: Int?)

    fun onSessionDeleteClicked(session: Session, adapterPosition: Int?)

}