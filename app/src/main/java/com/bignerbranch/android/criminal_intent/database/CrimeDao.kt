package com.bignerbranch.android.criminal_intent.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.bignerbranch.android.criminal_intent.Crime
import java.util.UUID


@Dao
interface CrimeDao {
    @Query("Select * FROM crime")
    fun getCrimes(): LiveData<List<Crime>>
    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>

}