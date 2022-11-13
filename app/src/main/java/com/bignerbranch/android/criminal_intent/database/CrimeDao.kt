package com.bignerbranch.android.criminal_intent.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bignerbranch.android.criminal_intent.Crime
import java.util.UUID


@Dao
interface CrimeDao {
    @Query("Select * FROM crime")
    fun getCrimes(): LiveData<List<Crime>>
    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>
    @Update
    fun updateCrime (crime: Crime)
    @Insert
    fun addCrime (crime: Crime)

}