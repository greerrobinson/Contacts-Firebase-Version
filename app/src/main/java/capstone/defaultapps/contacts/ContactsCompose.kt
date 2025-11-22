package capstone.defaultapps.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import capstone.defaultapps.contacts.Repository.ContactItem

// ---------- EXTENSIONS ----------

val ContactItem.sectionLetter: Char
    get() = if (firstName.isNotBlank()) {
        firstName.first().uppercaseChar()
    } else if (lastName.isNotBlank()) {
        lastName.first().uppercaseChar()
    } else {
        '#'
    }

val ContactItem.fullName: String
    get() = "$firstName $lastName".trim()

// ---------- COLOR PALETTE ----------

// From Figma:
// SOLID APP Blue #4: #0077B6
// Card fill: #F6F6F6
// Stroke: #8A8989

private val AppBlue = Color(0xFF0077B6)
private val CardFill = Color(0xFFF6F6F6)
private val Stroke = Color(0xFF8A8989)
private val TextGray = Color(0xFF8A8989)

private enum class ContactsMode {
    HOME_LIST,
    DETAILS,
    EDIT
}

// ---------- ROOT SCREEN ----------

@Composable
fun ContactsScreen(viewModel: ContactsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var mode by remember { mutableStateOf(ContactsMode.HOME_LIST) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var editingContact by remember { mutableStateOf<ContactItem?>(null) }

    val selectedContact = uiState.contacts.firstOrNull { it.id == selectedContactId }

    // Show error messages
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Here you could show a Snackbar or Toast
            viewModel.clearError()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (mode) {
            ContactsMode.HOME_LIST -> {
                ContactsHomeScreen(
                    contacts = uiState.contacts,
                    searchQuery = uiState.searchQuery,
                    isLoading = uiState.isLoading,
                    onSearchChange = viewModel::updateSearchQuery,
                    onAddClick = {
                        editingContact = null
                        mode = ContactsMode.EDIT
                    },
                    onContactClick = { contact ->
                        selectedContactId = contact.id
                        mode = ContactsMode.DETAILS
                    }
                )
            }

            ContactsMode.DETAILS -> {
                ContactDetailsScreen(
                    contact = selectedContact,
                    onBack = {
                        mode = ContactsMode.HOME_LIST
                        selectedContactId = null
                    },
                    onEdit = {
                        if (selectedContact != null) {
                            editingContact = selectedContact
                            mode = ContactsMode.EDIT
                        }
                    },
                    onDelete = {
                        selectedContact?.let { contact ->
                            contact.id?.let { id ->
                                viewModel.deleteContact(id)
                            }
                        }
                        mode = ContactsMode.HOME_LIST
                        selectedContactId = null
                    }
                )
            }

            ContactsMode.EDIT -> {
                ContactEditScreen(
                    initial = editingContact,
                    onCancel = {
                        mode = if (selectedContactId != null) {
                            ContactsMode.DETAILS
                        } else {
                            ContactsMode.HOME_LIST
                        }
                    },
                    onSave = { updatedContact ->
                        if (editingContact == null) {
                            // Adding new contact
                            viewModel.addContact(
                                firstName = updatedContact.firstName,
                                lastName = updatedContact.lastName,
                                company = updatedContact.company,
                                cellPhone = updatedContact.cellPhone,
                                workPhone = updatedContact.workPhone,
                                email = updatedContact.email,
                                address = updatedContact.address,
                                notes = updatedContact.notes
                            )
                        } else {
                            // Updating existing contact
                            viewModel.updateContact(updatedContact)
                        }

                        selectedContactId = updatedContact.id
                        mode = ContactsMode.DETAILS
                    }
                )
            }
        }
    }
}

// ---------- HOME LIST (A/B/C… SECTIONS) ----------

@Composable
fun ContactsHomeScreen(
    contacts: List<ContactItem>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onContactClick: (ContactItem) -> Unit
) {
    val grouped = remember(contacts) {
        contacts.groupBy { it.sectionLetter }.toSortedMap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top bar: title + add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contacts",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onAddClick) {
                Text(text = "+", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (letter, list) ->
                    item(key = "header_$letter") {
                        Text(
                            text = letter.toString(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(list, key = { it.id ?: 0 }) { contact ->
                        ContactListCard(contact = contact, onClick = { onContactClick(contact) })
                    }
                }

                if (grouped.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No contacts found", color = TextGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListCard(
    contact: ContactItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardFill),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = contact.fullName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (contact.company.isNotBlank()) {
                Text(
                    text = contact.company,
                    fontSize = 14.sp,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------- DETAILS SCREEN ----------

@Composable
fun ContactDetailsScreen(
    contact: ContactItem?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    if (contact == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No contact selected")
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Contacts", color = AppBlue)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) {
                Text("Edit", color = AppBlue)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contact name
        Text(
            text = contact.fullName,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (contact.company.isNotBlank()) {
            Text(
                text = contact.company,
                fontSize = 16.sp,
                color = TextGray
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Phone numbers
        if (contact.cellPhone.isNotBlank()) {
            PhoneRow("Mobile", contact.cellPhone)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (contact.workPhone.isNotBlank()) {
            PhoneRow("Work", contact.workPhone)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Email
        if (contact.email.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardFill),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Email", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(contact.email, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Address
        if (contact.address.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardFill),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Address", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(contact.address, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Notes
        if (contact.notes.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardFill),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Notes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(contact.notes, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PhoneRow(label: String, number: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label – same style as Email / Address / Notes
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        // Value – sits *under* the label
        Text(
            text = number,
            fontSize = 14.sp
        )
    }
}

// ---------- ADD / EDIT SCREEN ----------

@Composable
fun ContactEditScreen(
    initial: ContactItem?,
    onCancel: () -> Unit,
    onSave: (ContactItem) -> Unit
) {
    var firstName by remember { mutableStateOf(initial?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initial?.lastName ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var cellPhone by remember { mutableStateOf(initial?.cellPhone ?: "") }
    var workPhone by remember { mutableStateOf(initial?.workPhone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    val editingExisting = initial != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = AppBlue)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (firstName.isNotBlank() || lastName.isNotBlank()) {
                        val updated = ContactItem(
                            id = initial?.id,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            company = company.trim(),
                            cellPhone = cellPhone.trim(),
                            workPhone = workPhone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            notes = notes.trim()
                        )
                        onSave(updated)
                    }
                }
            ) {
                Text(
                    text = if (editingExisting) "Save" else "Add",
                    color = AppBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (editingExisting) "Edit Contact" else "New Contact",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = cellPhone,
                    onValueChange = { cellPhone = it },
                    label = { Text("Mobile Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = workPhone,
                    onValueChange = { workPhone = it },
                    label = { Text("Work Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                )
            }
        }
    }
}