package com.hopecoding.kotlinmaps.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.hopecoding.kotlinmaps.model.Place
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Completable

@Dao
interface PlaceDao {

    @Query("SELECT * FROM Place")
    fun getAll(): Flowable<List<Place>>

    @Insert
    fun insert(place: Place): Completable

    @Delete
    fun delete(place: Place): Completable

}