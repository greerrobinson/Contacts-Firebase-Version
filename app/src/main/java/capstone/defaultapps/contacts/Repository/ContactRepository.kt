package capstone.defaultapps.contacts.Repository

import android.util.Log
import androidx.annotation.WorkerThread
import capstone.defaultapps.contacts.Repository.ContactDao
import capstone.defaultapps.contacts.Repository.ContactFirestoreDatasource
import capstone.defaultapps.contacts.Repository.ContactItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

class ContactRepository(private val contactDao: ContactDao) {

    lateinit var uuid: String
    var contactItems: Flow<List<ContactItem>> = emptyFlow()
    var firestoreDatasource: ContactFirestoreDatasource = ContactFirestoreDatasource(this)

    suspend fun setUUID(uuid: String) {
        // If switching users, clear local data first
        if (this::uuid.isInitialized && this.uuid != uuid) {
            clearAllLocalData()
        }
        
        this.uuid = uuid
        // Get all contacts for this user
        contactItems = contactDao.getAllContactItems()
        firestoreDatasource.setUUID(uuid)
    }
    
    @Suppress("RedudndantSuspendModifier")
    @WorkerThread
    suspend fun clearAllLocalData() {
        contactDao.deleteAll()
    }

    @Suppress("RedudndantSuspendModifier")
    @WorkerThread
    suspend fun insert(contactItem: ContactItem){
        val id = contactDao.insert(contactItem)
        contactItem.id = id
        firestoreDatasource.insertContactItem(contactItem)
    }

    @Suppress("RedudndantSuspendModifier")
    @WorkerThread
    suspend fun getContactItem(contactId: Long): ContactItem {
        return contactDao.getItem(contactId)
    }

    @Suppress("RedudndantSuspendModifier")
    @WorkerThread
    suspend fun deleteContactItem(id: Long): Int {
        firestoreDatasource.deleteContactItem(id)
        return contactDao.deleteItem(id)
    }

    @Suppress("RedudndantSuspendModifier")
    @WorkerThread
    suspend fun updateItem(contactItem: ContactItem) {
        contactDao.updateItem(contactItem)
        firestoreDatasource.updateContactItem(contactItem)
    }

    @WorkerThread
    suspend fun notifyFirestoreChange(contactList: List<ContactItem>) {
        for(item in contactList){
            contactDao.updateItem(item)
        }
    }
// Needs fixing
//    @Suppress("RedudndantSuspendModifier")
//    @WorkerThread
//    suspend fun searchContactsByTitle(query: String): List<ContactItem> {
//        return contactDao.searchContactsByTitle("%$query%")
//    }

}