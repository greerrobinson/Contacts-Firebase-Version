package capstone.defaultapps.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

// ---------- COLOR PALETTE ----------

// From Figma:
// SOLID APP Blue #4: #0077B6
// Card fill: #F6F6F6
// Stroke: #8A8989

private val AppBlue = Color(0xFF0077B6)
private val CardFill = Color(0xFFF6F6F6)
private val CardStroke = Color(0xFF8A8989)
private val TextGray = Color(0xFF8E8E93)


// ---------- MODEL ----------

@Serializable
data class Contact(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val company: String = "",
    val cellPhone: String = "",
    val workPhone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = ""
) {
    val fullName: String
        get() = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val sectionLetter: Char
        get() = firstName.firstOrNull()?.uppercaseChar() ?: '#'
}

private enum class ContactsMode {
    HOME_LIST,
    DETAILS,
    EDIT
}

// ---------- DATA LOADER ----------

@Composable
fun loadContactsFromJson(): List<Contact> {
    val context = LocalContext.current
    return remember {
        val inputStream = context.resources.openRawResource(R.raw.contacts)
        val jsonText = InputStreamReader(inputStream).readText()
        Json.decodeFromString(jsonText)
    }
}

// ---------- ROOT SCREEN ----------

@Composable
fun ContactsScreen() {
    val initialContacts = loadContactsFromJson()
    var contacts by remember { mutableStateOf(initialContacts) }

    var mode by remember { mutableStateOf(ContactsMode.HOME_LIST) }
    var selectedContactId by remember { mutableStateOf<Int?>(null) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        val q = searchQuery.trim()

        val baseList = if (q.isBlank()) {
            contacts
        } else {
            contacts.filter { c ->
                val haystack = listOf(
                    c.fullName,
                    c.company,
                    c.cellPhone,
                    c.workPhone,
                    c.email
                ).joinToString(" ")
                haystack.contains(q, ignoreCase = true)
            }
        }

        // Sort by FIRST name, then last name
        baseList.sortedBy { it.firstName.uppercase() + it.lastName.uppercase() }
    }


    val selectedContact = contacts.firstOrNull { it.id == selectedContactId }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (mode) {
            ContactsMode.HOME_LIST -> {
                ContactsHomeScreen(
                    contacts = filteredContacts,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
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
                        selectedContact?.let { c ->
                            contacts = contacts.filterNot { it.id == c.id }
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
                    onSave = { updated ->
                        contacts = if (editingContact == null) {
                            val nextId = (contacts.maxOfOrNull { it.id } ?: 0) + 1
                            val newContact = updated.copy(id = nextId)
                            contacts + newContact
                        } else {
                            contacts.map { c -> if (c.id == updated.id) updated else c }
                        }

                        selectedContactId = updated.id
                        mode = ContactsMode.DETAILS
                    }
                )
            }
        }
    }
}

// ---------- HOME LIST (A/B/C… SECTIONS) ----------

@Composable
private fun ContactsHomeScreen(
    contacts: List<Contact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onContactClick: (Contact) -> Unit
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
                items(list, key = { it.id }) { contact ->
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

@Composable
private fun ContactListCard(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = CardFill

        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = contact.fullName,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (contact.company.isNotBlank()) {
                Text(
                    text = contact.company,
                    fontSize = 13.sp,
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
private fun ContactDetailsScreen(
    contact: Contact?,
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
                Text("<", fontSize = 20.sp, color = AppBlue)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) {
                Text("Edit", color = AppBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = CardFill, shape = RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    color = CardStroke,
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Name + company
        Text(
            text = contact.fullName,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (contact.company.isNotBlank()) {
            Text(
                text = contact.company,
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Call / Message / Email style row (text buttons as stand-ins for icons)
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TextButton(onClick = {
                val number = contact.cellPhone.ifBlank { contact.workPhone }
                if (number.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    context.startActivity(intent)
                }
            }) {
                Text("Call", color = AppBlue)
            }

            TextButton(onClick = {
                if (contact.email.isNotBlank()) {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:${contact.email}")
                    )
                    context.startActivity(intent)
                }
            }) {
                Text("Email", color = AppBlue)
            }

            TextButton(onClick = onDelete) {
                Text("Delete", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone numbers card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardFill),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (contact.cellPhone.isNotBlank()) {
                    PhoneRow(label = "Cell Phone", number = contact.cellPhone)
                }
                if (contact.workPhone.isNotBlank()) {
                    PhoneRow(label = "Work Phone", number = contact.workPhone)
                }
            }
        }

        if (contact.email.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
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
        }

        if (contact.address.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
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
        }

        if (contact.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
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
private fun ContactEditScreen(
    initial: Contact?,
    onCancel: () -> Unit,
    onSave: (Contact) -> Unit
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
                        val baseId = initial?.id ?: 0
                        val updated = Contact(
                            id = baseId,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = CardFill, shape = RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    color = CardStroke,
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontSize = 32.sp,
                color = TextGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -------- Name / Company fields (white, full-width like Email) --------

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = company,
            onValueChange = { company = it },
            label = { Text("Company") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // -------- Phone fields (match Email/Address style) --------

        OutlinedTextField(
            value = cellPhone,
            onValueChange = { cellPhone = it },
            label = { Text("Cell Phone") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = workPhone,
            onValueChange = { workPhone = it },
            label = { Text("Work Phone") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))


        Spacer(modifier = Modifier.height(10.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Notes
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
