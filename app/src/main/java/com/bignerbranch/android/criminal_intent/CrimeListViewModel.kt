package com.bignerbranch.android.criminal_intent

import androidx.lifecycle.ViewModel
import com.bignerbranch.android.criminal_intent.crimeRepository.CrimeRepository

class CrimeListViewModel: ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()

    fun addCrime (crime: Crime){
        crimeRepository.addCrime(crime)
    }



}