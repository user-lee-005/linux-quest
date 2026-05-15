package com.linuxquest

import android.app.Application

class LinuxQuestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LinuxQuestApp
            private set
    }
}
