package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_vault_items")
data class EncryptedItemEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val titleCipher: String,
  val usernameCipher: String,
  val passwordCipher: String,
  val websiteCipher: String,
  val notesCipher: String,
  val categoryCipher: String,
  val iv: String,
  val isFavorite: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
