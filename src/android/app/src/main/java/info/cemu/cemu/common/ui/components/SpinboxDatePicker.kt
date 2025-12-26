package info.cemu.cemu.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.cemu.cemu.common.ui.localization.tr
import java.util.Calendar

@Composable
fun SpinboxDatePickerDialog(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // If date is 0, default to current time
    val startTime = if (initialDateMillis > 0) initialDateMillis else System.currentTimeMillis()
    
    var tempDate by remember { mutableStateOf(startTime) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(tr("Select Birthday")) },
        text = {
            val currentCal = Calendar.getInstance().apply { timeInMillis = tempDate }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day
                SpinItem(
                    value = currentCal.get(Calendar.DAY_OF_MONTH),
                    label = tr("Day"),
                    range = 1..31,
                    onValueChange = { newValue ->
                        currentCal.set(Calendar.DAY_OF_MONTH, newValue)
                        tempDate = currentCal.timeInMillis
                    }
                )
                // Month
                SpinItem(
                    value = currentCal.get(Calendar.MONTH) + 1,
                    label = tr("Month"),
                    range = 1..12,
                    onValueChange = { newValue ->
                        currentCal.set(Calendar.MONTH, newValue - 1)
                        tempDate = currentCal.timeInMillis
                    }
                )
                // Year
                SpinItem(
                    value = currentCal.get(Calendar.YEAR),
                    label = tr("Year"),
                    range = 1900..2100,
                    onValueChange = { newValue ->
                        currentCal.set(Calendar.YEAR, newValue)
                        tempDate = currentCal.timeInMillis
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempDate) }) { Text(tr("OK")) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(tr("Cancel")) }
        }
    )
}

@Composable
private fun SpinItem(
    value: Int,
    label: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("▲")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value.toString(), style = MaterialTheme.typography.bodyLarge)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("▼")
        }
    }
}
