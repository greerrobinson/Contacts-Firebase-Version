package capstone.defaultapps.contacts

import android.app.Application
import capstone.defaultapps.contacts.Repository.ContactRoomDatabase
import capstone.defaultapps.contacts.Repository.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ContactsApplication: Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { ContactRoomDatabase.getDatabase(this, applicationScope)}
    val repository by lazy{ 
        val repo = ContactRepository(database.eventDao())
        repo
    }
}