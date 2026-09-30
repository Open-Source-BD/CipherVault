package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
  @Query("SELECT * FROM encrypted_vault_items ORDER BY isFavorite DESC, updatedAt DESC")
  fun getAll(): Flow<List<EncryptedItemEntity>>

  @Query("SELECT * FROM encrypted_vault_items WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): EncryptedItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: EncryptedItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<EncryptedItemEntity>)

  @Update
  suspend fun update(item: EncryptedItemEntity)

  @Delete
  suspend fun delete(item: EncryptedItemEntity)

  @Query("DELETE FROM encrypted_vault_items WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM encrypted_vault_items")
  suspend fun clearAll()

  @Query("SELECT COUNT(*) FROM encrypted_vault_items")
  fun count(): Flow<Int>
}
