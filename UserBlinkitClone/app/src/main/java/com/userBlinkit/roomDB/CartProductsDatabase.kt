package com.userBlinkit.roomDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CartProducts::class], version = 2, exportSchema = false)
abstract class CartProductsDatabase : RoomDatabase(){

    abstract fun cartProductsDao() : CartProductsDao
    companion object{

        @Volatile
        var INSTANCE : CartProductsDatabase? = null

        fun getDatabaseInstance(context: Context): CartProductsDatabase{
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance

            //INSTANCE is null then create database instance
            synchronized(this) {
                val roomDB = Room.databaseBuilder(
                    context,
                    CartProductsDatabase::class.java,
                    "CartProducts"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // ✅ Prevents crash from missing migration
                    .build()
                INSTANCE = roomDB
                return roomDB
            }
        }
    }
}

// I am changing  version = 1 to  version = 2 & add this line .fallbackToDestructiveMigration() for avoid crash app