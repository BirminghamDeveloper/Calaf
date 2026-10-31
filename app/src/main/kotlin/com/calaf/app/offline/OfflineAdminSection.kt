package com.calaf.app.offline

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OfflineAdminSection(demo: OfflineDemo) {
    val revision by demo.revision.collectAsState()
    if (!demo.isAdmin) return
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var error by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Administrator controls", style = MaterialTheme.typography.titleLarge)
        Text("Offline demo administrator. Actions affect only accounts and reports stored on this device.")
        OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide administrator panel" else "Open administrator panel") }
        if (error) Text("The action could not be completed. Reopen the panel and try again.", color = MaterialTheme.colorScheme.error)
        if (expanded) {
            val members = remember(revision) { demo.adminMembers() }
            val reports = remember(revision) { demo.adminReports() }
            Text("Members (${members.size})", style = MaterialTheme.typography.titleMedium)
            members.forEach { member ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(member.email)
                    Text(if (member.id == "demo-admin") "Administrator" else if (member.suspended) "Suspended" else "Member")
                    if (member.id != "demo-admin") TextButton(onClick = {
                        pending = (if (member.suspended) "Restore ${member.email}?" else "Suspend ${member.email}? They will be unable to log in or appear in discovery.") to
                            { demo.suspendMember(member.id, !member.suspended) }
                    }) { Text(if (member.suspended) "Restore member" else "Suspend member") }
                } }
            }
            Text("Reports (${reports.size})", style = MaterialTheme.typography.titleMedium)
            reports.forEach { report ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("Reported member: ${report.target}")
                    Text(report.details)
                    Text(report.status)
                    if (report.status == "OPEN") Row {
                        TextButton(onClick = { pending = "Mark this report resolved?" to { demo.resolveReport(report.id, false) } }) { Text("Resolve report") }
                        TextButton(onClick = { pending = "Dismiss this report?" to { demo.resolveReport(report.id, true) } }) { Text("Dismiss report") }
                    }
                } }
            }
            Text("Recorded administrative actions: ${demo.auditCount()}")
        }
    }
    pending?.let { action -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("Confirm administrator action") },
        text = { Text(action.first) }, confirmButton = { TextButton(onClick = {
            error = runCatching { action.second() }.isFailure; pending = null
        }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }) }
}
