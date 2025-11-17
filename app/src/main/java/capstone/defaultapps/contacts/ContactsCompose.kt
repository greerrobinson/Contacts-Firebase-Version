package capstone.defaultapps.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@Serializable
data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String
)

private enum class ContactsMode {
    LIST_ONLY,
    LIST_AND_DETAILS,
    EDIT
}

// Mirrors loadEventsFromJson() in CalendarCompose.kt
@Composable
fun loadContactsFromJson(): List<Contact> {
    val context = LocalContext.current
    return remember {
        val inputStream = context.resources.openRawResource(R.raw.contacts)
        val jsonText = InputStreamReader(inputStream).readText()
        Json.decodeFromString(jsonText)
    }
}

@Composable
fun ContactsScreen() {
    val initialContacts = loadContactsFromJson()
    var contacts by remember { mutableStateOf(initialContacts) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedContactId by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(ContactsMode.LIST_ONLY) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else {
            val q = searchQuery.trim()
            contacts.filter { c ->
                c.name.contains(q, ignoreCase = true) ||
                        c.phone.contains(q, ignoreCase = true) ||
                        c.email.contains(q, ignoreCase = true)
            }
        }
    }

    val selectedContact = contacts.firstOrNull { it.id == selectedContactId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        ContactsTitle(
            onAddClick = {
                editingContact = null
                mode = ContactsMode.EDIT
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ContactsSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (mode) {
            ContactsMode.LIST_ONLY -> {
                ContactsListSection(
                    contacts = filteredContacts,
                    onContactSelected = { contact ->
                        selectedContactId = contact.id
                        mode = ContactsMode.LIST_AND_DETAILS
                    }
                )
            }

            ContactsMode.LIST_AND_DETAILS -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ContactsListSection(
                            contacts = filteredContacts,
                            onContactSelected = { contact ->
                                selectedContactId = contact.id
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (selectedContact != null) {
                            ContactDetailsSection(
                                contact = selectedContact,
                                onEdit = {
                                    editingContact = selectedContact
                                    mode = ContactsMode.EDIT
                                },
                                onDelete = {
                                    contacts = contacts.filterNot { it.id == selectedContact.id }
                                    selectedContactId = null
                                    mode = ContactsMode.LIST_ONLY
                                },
                                onBackToList = {
                                    mode = ContactsMode.LIST_ONLY
                                    selectedContactId = null
                                }
                            )
                        } else {
                            EmptyDetailsPlaceholder(
                                onBackToList = {
                                    mode = ContactsMode.LIST_ONLY
                                    selectedContactId = null
                                }
                            )
                        }
                    }
                }
            }

            ContactsMode.EDIT -> {
                ContactEditSection(
                    initial = editingContact,
                    onCancel = {
                        mode = if (selectedContactId != null) {
                            ContactsMode.LIST_AND_DETAILS
                        } else {
                            ContactsMode.LIST_ONLY
                        }
                    },
                    onSave = { updated ->
                        contacts = if (editingContact == null) {
                            val nextId = (contacts.maxOfOrNull { it.id } ?: 0) + 1
                            contacts + updated.copy(id = nextId)
                        } else {
                            contacts.map { c ->
                                if (c.id == updated.id) updated else c
                            }
                        }
                        selectedContactId = updated.id
                        mode = ContactsMode.LIST_AND_DETAILS
                    }
                )
            }
        }
    }
}

@Composable
private fun ContactsTitle(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Contacts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) {
            Text(text = "Add")
        }
    }
}

@Composable
private fun ContactsSearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Search") },
        singleLine = true
    )
}

@Composable
private fun ContactsListSection(
    contacts: List<Contact>,
    onContactSelected: (Contact) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "No contacts found",
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                ContactListItem(
                    contact = contact,
                    onClick = { onContactSelected(contact) }
                )
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0E0E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = contact.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.phone,
                fontSize = 14.sp
            )
            if (contact.email.isNotBlank()) {
                Text(
                    text = contact.email,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ContactDetailsSection(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBackToList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToList) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE0E0E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = contact.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Phone: ${contact.phone}")
                if (contact.email.isNotBlank()) {
                    Text(text = "Email: ${contact.email}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDelete) {
                Text(text = "Delete", color = Color.Red)
            }
            TextButton(onClick = onEdit) {
                Text(text = "Edit")
            }
        }
    }
}

@Composable
private fun EmptyDetailsPlaceholder(
    onBackToList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select a contact to view details",
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToList) {
            Text(text = "Back to list")
        }
    }
}

@Composable
private fun ContactEditSection(
    initial: Contact?,
    onCancel: () -> Unit,
    onSave: (Contact) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }

    val editingExisting = initial != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (editingExisting) "Edit Contact" else "Add Contact",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Name") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Phone") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    val baseId = initial?.id ?: 0
                    val updated = Contact(
                        id = baseId,
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim()
                    )
                    onSave(updated)
                }
            }
        ) {
            Text(
                text = if (editingExisting) "Save Changes" else "Save Contact"
            )
        }
    }
}
