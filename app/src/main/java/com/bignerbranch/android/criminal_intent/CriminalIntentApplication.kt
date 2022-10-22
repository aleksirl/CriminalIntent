package com.bignerbranch.android.criminal_intent

import android.app.Application
import com.bignerbranch.android.criminal_intent.crimeRepository.CrimeRepository

class CriminalIntentApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}