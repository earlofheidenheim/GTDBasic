package com.beispiel.gtdbasic.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "status", indices = [Index(value = ["name"], unique = true)])
data class Status(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Dao
interface StatusDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(status: Status)

    @Query("SELECT name FROM status ORDER BY name ASC")
    fun getAllStatusNames(): Flow<List<String>>

    @Query("DELETE FROM status WHERE name = :name")
    suspend fun deleteByName(name: String)
}
