package de.healthforge.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lokale Einkaufslisten-Items (REQ-SHOP-001/002/003).
 *
 * Wird aus geplanten Mahlzeiten in einem Datumsbereich aggregiert. Jeder Eintrag
 * repräsentiert eine Zutat in einer normalisierten Einheit (best-effort). Mehrere
 * Aggregations-Runs werden in `runId` getrennt, sodass der User den letzten Lauf
 * sehen + abhaken kann, ohne dass alte Einträge verloren gehen.
 *
 * `category` ist best-effort aus `IngredientDto.category` bzw. fallback `"Sonstiges"`.
 * `ingredientId` ist optional (kann null sein für rezeptlose Snapshot-Einträge).
 */
@Entity(
    tableName = "shopping_list_item",
    indices = [
        Index(value = ["runId"]),
        Index(value = ["runId", "category"]),
    ],
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "runId")
    val runId: Long,
    @ColumnInfo(name = "ingredientId")
    val ingredientId: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "unit")
    val unit: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "checked")
    val checked: Boolean = false,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
)
