package com.example.contactexplorer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "contact_groups")
data class ContactGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(tableName = "contact_group_memberships", primaryKeys = ["contactId", "groupId"])
data class ContactGroupMembership(val contactId: String, val groupId: Long)

@Entity(tableName = "favorite_contacts")
data class FavoriteContact(@PrimaryKey val contactId: String)

@Entity(tableName = "recent_contacts")
data class RecentContact(@PrimaryKey val contactId: String, val viewedAt: Long)

@Entity(tableName = "contact_tags")
data class ContactTag(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "contact_tag_assignments", primaryKeys = ["contactId", "tagId"])
data class ContactTagAssignment(val contactId: String, val tagId: Long)

@Entity(tableName = "deleted_contacts")
data class DeletedContact(
    @PrimaryKey val contactId: String,
    val lookupKey: String,
    val name: String,
    val phone: String,
    val email: String,
    val company: String,
    val address: String,
    val note: String,
    val lastModified: Long,
    val timesContacted: Int,
    val deletedAt: Long,
    val groupIds: String,
    val tagIds: String,
    val wasFavorite: Boolean,
    val vcard: String = "",
)

@Entity(tableName = "contact_events")
data class ContactEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String?,
    val contactName: String,
    val action: String,
    val details: String,
    val occurredAt: Long,
)

@Entity(tableName = "smart_group_rules")
data class SmartGroupRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val field: String,
    val value: String,
)

@Dao
interface ExplorerDao {
    @Query("SELECT * FROM contact_groups ORDER BY name COLLATE NOCASE")
    fun observeGroups(): Flow<List<ContactGroup>>

    @Query("SELECT * FROM contact_group_memberships")
    fun observeMemberships(): Flow<List<ContactGroupMembership>>

    @Query("SELECT contactId FROM favorite_contacts")
    fun observeFavorites(): Flow<List<String>>

    @Query("SELECT contactId FROM recent_contacts ORDER BY viewedAt DESC")
    fun observeRecentIds(): Flow<List<String>>

    @Query("SELECT * FROM contact_tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<ContactTag>>

    @Query("SELECT * FROM contact_tag_assignments")
    fun observeTagAssignments(): Flow<List<ContactTagAssignment>>

    @Query("SELECT * FROM deleted_contacts ORDER BY deletedAt DESC")
    fun observeDeletedContacts(): Flow<List<DeletedContact>>

    @Query("SELECT * FROM contact_events ORDER BY occurredAt DESC LIMIT 500")
    fun observeEvents(): Flow<List<ContactEvent>>

    @Query("SELECT * FROM smart_group_rules ORDER BY name COLLATE NOCASE")
    fun observeSmartGroups(): Flow<List<SmartGroupRule>>

    @Insert
    suspend fun createGroup(group: ContactGroup): Long

    @Query("UPDATE contact_groups SET name = :name WHERE id = :groupId")
    suspend fun renameGroup(groupId: Long, name: String)

    @Query("DELETE FROM contact_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFavorite(favorite: FavoriteContact)

    @Query("DELETE FROM favorite_contacts WHERE contactId = :contactId")
    suspend fun removeFavorite(contactId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecent(recent: RecentContact)

    @Query("DELETE FROM recent_contacts WHERE contactId = :contactId")
    suspend fun removeRecent(contactId: String)

    @Query("DELETE FROM contact_group_memberships WHERE contactId = :contactId")
    suspend fun clearGroupsForContact(contactId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMembership(membership: ContactGroupMembership)

    @Query("DELETE FROM contact_group_memberships WHERE groupId = :groupId")
    suspend fun clearGroupMemberships(groupId: Long)

    @Query("DELETE FROM favorite_contacts WHERE contactId = :contactId")
    suspend fun clearFavorite(contactId: String)

    @Query("DELETE FROM recent_contacts WHERE contactId = :contactId")
    suspend fun clearRecent(contactId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTag(tag: ContactTag): Long

    @Query("DELETE FROM contact_tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("DELETE FROM contact_tag_assignments WHERE tagId = :tagId")
    suspend fun clearTagAssignments(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignTag(assignment: ContactTagAssignment)

    @Query("DELETE FROM contact_tag_assignments WHERE contactId = :contactId")
    suspend fun clearTagsForContact(contactId: String)

    @Query("DELETE FROM contact_tag_assignments WHERE contactId IN (:contactIds)")
    suspend fun clearTagsForContacts(contactIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDeletedContact(contact: DeletedContact)

    @Query("DELETE FROM deleted_contacts WHERE contactId = :contactId")
    suspend fun removeDeletedContact(contactId: String)

    @Query("DELETE FROM deleted_contacts WHERE deletedAt < :cutoff")
    suspend fun purgeOldDeletedContacts(cutoff: Long)

    @Insert
    suspend fun addEvent(event: ContactEvent)

    @Query("DELETE FROM contact_events WHERE contactId = :contactId")
    suspend fun clearEventsForContact(contactId: String)

    @Insert
    suspend fun createSmartGroup(group: SmartGroupRule): Long

    @Query("DELETE FROM smart_group_rules WHERE id = :id")
    suspend fun deleteSmartGroup(id: Long)
}

@Database(
    entities = [ContactGroup::class, ContactGroupMembership::class, FavoriteContact::class, RecentContact::class,
        ContactTag::class, ContactTagAssignment::class, DeletedContact::class, ContactEvent::class, SmartGroupRule::class],
    version = 3,
    exportSchema = true,
)
abstract class ExplorerDatabase : RoomDatabase() {
    abstract fun explorerDao(): ExplorerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS contact_tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS contact_tag_assignments (contactId TEXT NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY(contactId, tagId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS deleted_contacts (contactId TEXT NOT NULL PRIMARY KEY, lookupKey TEXT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, company TEXT NOT NULL, address TEXT NOT NULL, note TEXT NOT NULL, lastModified INTEGER NOT NULL, timesContacted INTEGER NOT NULL, deletedAt INTEGER NOT NULL, groupIds TEXT NOT NULL, tagIds TEXT NOT NULL, wasFavorite INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS contact_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contactId TEXT, contactName TEXT NOT NULL, action TEXT NOT NULL, details TEXT NOT NULL, occurredAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS smart_group_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, field TEXT NOT NULL, value TEXT NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deleted_contacts ADD COLUMN vcard TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var instance: ExplorerDatabase? = null

        fun get(context: Context): ExplorerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ExplorerDatabase::class.java,
                "contact-explorer.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
