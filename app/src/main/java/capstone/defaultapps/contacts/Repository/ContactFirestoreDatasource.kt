package capstone.defaultapps.contacts.Repository

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactFirestoreDatasource(val repository: ContactRepository) {

    val TAG = "ContactFirestoreDatasource"
    val db = Firebase.firestore
    lateinit var uuid: String
    var contactList: MutableMap<Long, ContactItem> = HashMap<Long, ContactItem>()
    lateinit var collectionReference: CollectionReference

    fun setUUID(uuid: String) {
        this.uuid = uuid
        collectionReference = db.collection("contacts_$uuid")
        
        // Listen for real-time updates from Firestore
        collectionReference.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }
            
            if (snapshot != null && !snapshot.isEmpty()) {
                Log.d(TAG, "Current number of documents: ${snapshot.documents.size}")
                contactList = HashMap<Long, ContactItem>()
                
                for (document in snapshot.documents) {
                    document.toObject(ContactItem::class.java)?.let { item ->
                        item.id?.let { id ->
                            contactList[id] = item
                        }
                    }
                }
                
                Log.d(TAG, "Current List Size is ${contactList.size}")
                CoroutineScope(Dispatchers.IO).launch {
                    repository.notifyFirestoreChange(contactList.values.toList())
                }
            } else {
                Log.d(TAG, "Current data: Empty")
            }
        }
    }

    fun insertContactItem(item: ContactItem) {
        if (!::uuid.isInitialized) {
            Log.e(TAG, "UUID not set for Firestore")
            return
        }
        
        db.collection("contacts_$uuid")
            .document(item.id.toString())
            .set(item)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot added with ID: ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    fun updateContactItem(item: ContactItem) {
        if (!::uuid.isInitialized) {
            Log.e(TAG, "UUID not set for Firestore")
            return
        }
        
        db.collection("contacts_$uuid")
            .document(item.id.toString())
            .set(item)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot updated with ID: ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
            }
    }

    fun deleteContactItem(itemId: Long) {
        if (!::uuid.isInitialized) {
            Log.e(TAG, "UUID not set for Firestore")
            return
        }
        
        db.collection("contacts_$uuid")
            .document(itemId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot deleted with ID: $itemId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting document", e)
            }
    }
}