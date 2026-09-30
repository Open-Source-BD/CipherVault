package com.example.data.repository

import com.example.crypto.CryptoManager
import com.example.data.db.EncryptedItemEntity
import com.example.data.db.VaultDao
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.crypto.SecretKey

class VaultRepository(private val vaultDao: VaultDao) {

  fun getDecryptedItems(masterKey: SecretKey): Flow<List<VaultItem>> {
    return vaultDao.getAll().map { entities ->
      entities.mapNotNull { entity ->
        try {
          decryptEntity(entity, masterKey)
        } catch (_: Exception) {
          // If decryption fails (e.g. corrupted entry), return safe placeholder or null
          null
        }
      }
    }
  }

  suspend fun insertItem(item: VaultItem, masterKey: SecretKey): Long {
    val entity = encryptItem(item, masterKey)
    return vaultDao.insert(entity)
  }

  suspend fun updateItem(item: VaultItem, masterKey: SecretKey) {
    val entity = encryptItem(item, masterKey)
    vaultDao.update(entity)
  }

  suspend fun deleteItem(id: Long) {
    vaultDao.deleteById(id)
  }

  suspend fun clearAll() {
    vaultDao.clearAll()
  }

  suspend fun getAllEntities(): List<EncryptedItemEntity> {
    // Note: Used for backup/export
    return emptyList()
  }

  private fun encryptItem(item: VaultItem, masterKey: SecretKey): EncryptedItemEntity {
    val iv = CryptoManager.generateIv()
    val (titleCipher, ivBase64) = CryptoManager.encrypt(item.title, masterKey, iv)
    val (userCipher, _) = CryptoManager.encrypt(item.username, masterKey, iv)
    val (passCipher, _) = CryptoManager.encrypt(item.password, masterKey, iv)
    val (webCipher, _) = CryptoManager.encrypt(item.website, masterKey, iv)
    val (notesCipher, _) = CryptoManager.encrypt(item.notes, masterKey, iv)
    val (catCipher, _) = CryptoManager.encrypt(item.category.name, masterKey, iv)

    return EncryptedItemEntity(
      id = item.id,
      titleCipher = titleCipher,
      usernameCipher = userCipher,
      passwordCipher = passCipher,
      websiteCipher = webCipher,
      notesCipher = notesCipher,
      categoryCipher = catCipher,
      iv = ivBase64,
      isFavorite = item.isFavorite,
      createdAt = item.createdAt,
      updatedAt = System.currentTimeMillis()
    )
  }

  private fun decryptEntity(entity: EncryptedItemEntity, masterKey: SecretKey): VaultItem {
    val title = CryptoManager.decrypt(entity.titleCipher, entity.iv, masterKey)
    val username = CryptoManager.decrypt(entity.usernameCipher, entity.iv, masterKey)
    val password = CryptoManager.decrypt(entity.passwordCipher, entity.iv, masterKey)
    val website = CryptoManager.decrypt(entity.websiteCipher, entity.iv, masterKey)
    val notes = CryptoManager.decrypt(entity.notesCipher, entity.iv, masterKey)
    val categoryName = try {
      CryptoManager.decrypt(entity.categoryCipher, entity.iv, masterKey)
    } catch (_: Exception) {
      VaultCategory.LOGINS.name
    }
    val category = try {
      VaultCategory.valueOf(categoryName)
    } catch (_: Exception) {
      VaultCategory.LOGINS
    }

    return VaultItem(
      id = entity.id,
      title = title,
      username = username,
      password = password,
      website = website,
      notes = notes,
      category = category,
      isFavorite = entity.isFavorite,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
      isRevealed = false
    )
  }
}
