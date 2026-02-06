package com.example.composetutorial

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 1. ENTITY: Tämä on tietokantataulu "UserProfile"
@Entity
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Käytämme aina id=1, koska meillä on vain yksi käyttäjä
    val username: String,
    val imagePath: String? // Tänne tallennetaan kuvan tiedostopolku
)

// 2. DAO: Tässä ovat komennot tietokannan käyttöön
@Dao
interface UserDao {
    // Hakee käyttäjän. "Flow" tarkoittaa, että UI päivittyy automaattisesti, jos tiedot muuttuvat.
    @Query("SELECT * FROM UserProfile WHERE id = 1")
    fun getUser(): Flow<UserProfile?>

    // Tallentaa tai päivittää käyttäjän tiedot.
    // "suspend" tarkoittaa, että tämä tehdään taustalla, ettei sovellus jumitu.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)
}

// 3. DATABASE: Itse tietokanta, joka sitoo nämä yhteen
@Database(entities = [UserProfile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}