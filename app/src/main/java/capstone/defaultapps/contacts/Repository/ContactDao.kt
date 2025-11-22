package capstone.defaultapps.contacts.Repository

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface ContactDao {

    @Query("SELECT * FROM contactitems_table WHERE uuid=:uuid order by firstName ASC, lastName ASC")
    fun getContactItems(uuid: String): Flow<List<ContactItem>>
    
    @Query("SELECT * FROM contactitems_table order by firstName ASC, lastName ASC")
    fun getAllContactItems(): Flow<List<ContactItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contactItem: ContactItem): Long

    @Query("DELETE FROM contactitems_table")
    fun deleteAll(): Int

    @Query("SELECT * FROM contactitems_table WHERE id = :id")
    suspend fun getItem(id: Long): ContactItem

    @Query("DELETE FROM contactitems_table WHERE id=:id")
    suspend fun deleteItem(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItem(item: ContactItem)

    @Query("DELETE FROM contactitems_table WHERE uuid = :uuid")
    suspend fun deleteContactsByUUID(uuid: String): Int

    @Query("SELECT * FROM contactitems_table WHERE (firstName LIKE :query OR lastName LIKE :query OR company LIKE :query OR email LIKE :query) order by firstName ASC, lastName ASC")
    suspend fun searchContactsByQuery(query: String): List<ContactItem>

}