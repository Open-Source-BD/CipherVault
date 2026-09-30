package com.example.data.model

enum class VaultCategory(val displayName: String) {
  ALL("All Items"),
  LOGINS("Logins"),
  CARDS("Credit Cards"),
  SECURE_NOTES("Secure Notes"),
  IDENTITIES("Identities"),
  OTHER("Other")
}

data class VaultItem(
  val id: Long = 0,
  val title: String,
  val username: String = "",
  val password: String = "",
  val website: String = "",
  val notes: String = "",
  val category: VaultCategory = VaultCategory.LOGINS,
  val isFavorite: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isRevealed: Boolean = false
)
