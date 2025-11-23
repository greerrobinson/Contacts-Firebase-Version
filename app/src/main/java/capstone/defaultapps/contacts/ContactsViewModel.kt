package capstone.defaultapps.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import capstone.defaultapps.contacts.Repository.ContactItem
import capstone.defaultapps.contacts.Repository.ContactRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<ContactItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isLoggedOut: Boolean = false
)

class ContactsViewModel(private val repository: ContactRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.contactItems.collect { contactList ->
                    val filteredContacts = if (_uiState.value.searchQuery.isBlank()) {
                        contactList
                    } else {
                        filterContacts(contactList, _uiState.value.searchQuery)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        contacts = filteredContacts,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load contacts"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Filter existing contacts immediately for responsive UI
        val filteredContacts = if (query.isBlank()) {
            _uiState.value.contacts
        } else {
            filterContacts(_uiState.value.contacts, query)
        }
        _uiState.value = _uiState.value.copy(contacts = filteredContacts)
    }

    fun addContact(
        firstName: String,
        lastName: String,
        company: String = "",
        cellPhone: String = "",
        workPhone: String = "",
        email: String = "",
        address: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val newContact = ContactItem(
                    uuid = repository.uuid,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    company = company.trim(),
                    cellPhone = cellPhone.trim(),
                    workPhone = workPhone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    notes = notes.trim()
                )
                repository.insert(newContact)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add contact"
                )
            }
        }
    }

    fun updateContact(contactItem: ContactItem) {
        viewModelScope.launch {
            try {
                repository.updateItem(contactItem)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to update contact"
                )
            }
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteContactItem(contactId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to delete contact"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Clear local repository data
                repository.clearUserData()
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                // Update UI state
                _uiState.value = ContactsUiState(isLoggedOut = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout failed: ${e.message}"
                )
            }
        }
    }

    private fun filterContacts(contacts: List<ContactItem>, query: String): List<ContactItem> {
        val searchQuery = query.lowercase()
        return contacts.filter { contact ->
            contact.firstName.lowercase().contains(searchQuery) ||
            contact.lastName.lowercase().contains(searchQuery) ||
            contact.company.lowercase().contains(searchQuery) ||
            contact.email.lowercase().contains(searchQuery) ||
            contact.cellPhone.contains(searchQuery) ||
            contact.workPhone.contains(searchQuery)
        }
    }
}

class ContactsViewModelFactory(private val repository: ContactRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}