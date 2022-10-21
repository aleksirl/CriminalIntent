package com.bignerbranch.android.criminal_intent.database

import android.provider.ContactsContract.Data
import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

class CrimeTypeConverters {

    @TypeConverter
    fun fromData(data: Date?): Long?{
        return data?.time
    }
    @TypeConverter
    fun toData(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }


    @TypeConverter
    fun toUUID (uuid: String?): UUID?{
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromUUID (uuid: UUID?): String?{
        return uuid?.toString()
    }
}