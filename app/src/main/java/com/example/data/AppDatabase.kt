package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Favorite Entity
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val dramaId: Int,
    val addedTime: Long = System.currentTimeMillis()
)

// 2. Watch History Entity
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val dramaId: Int,
    val episodeNumber: Int,
    val episodeTitle: String,
    val lastWatchedTime: Long = System.currentTimeMillis(),
    val videoPositionMs: Long = 0L
)

// 3. Unlocked Episode Entity (peristent purchase tracking)
@Entity(tableName = "unlocked_episodes")
data class UnlockedEpisodeEntity(
    @PrimaryKey val compositeId: String, // format: "dramaId_episodeNumber"
    val unlockedTime: Long = System.currentTimeMillis()
)

// 4. User Balance Entity
@Entity(tableName = "user_balance")
data class UserBalanceEntity(
    @PrimaryKey val id: Int = 0, // singleton row
    val coins: Int = 50, // default starter coins
    val lastCheckInTime: Long = 0L
)

// DAO Interfaces
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedTime DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE dramaId = :dramaId)")
    fun isFavoriteFlow(dramaId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE dramaId = :dramaId)")
    suspend fun isFavorite(dramaId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE dramaId = :dramaId")
    suspend fun deleteFavoriteById(dramaId: Int)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTime DESC")
    fun getAllHistoryFlow(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE dramaId = :dramaId")
    suspend fun getHistoryForDrama(dramaId: Int): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE dramaId = :dramaId")
    suspend fun deleteHistoryById(dramaId: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}

@Dao
interface UnlockedEpisodeDao {
    @Query("SELECT * FROM unlocked_episodes")
    fun getAllUnlockedFlow(): Flow<List<UnlockedEpisodeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM unlocked_episodes WHERE compositeId = :compositeId)")
    suspend fun isEpisodeUnlocked(compositeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlock(unlock: UnlockedEpisodeEntity)
}

@Dao
interface UserBalanceDao {
    @Query("SELECT * FROM user_balance WHERE id = 0")
    fun getBalanceFlow(): Flow<UserBalanceEntity?>

    @Query("SELECT * FROM user_balance WHERE id = 0")
    suspend fun getBalance(): UserBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBalance(balance: UserBalanceEntity)
}

// Room Database Definition
@Database(
    entities = [
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        UnlockedEpisodeEntity::class,
        UserBalanceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun unlockedEpisodeDao(): UnlockedEpisodeDao
    abstract fun userBalanceDao(): UserBalanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shortflix_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
