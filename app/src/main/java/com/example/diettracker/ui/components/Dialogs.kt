package com.example.diettracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.diettracker.util.NumberParsing

/** Standard confirmation dialog, used for every destructive action. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "确定",
    dismissLabel: String = "取消",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}

/** A one-line "label + error" block shared by the food form. */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Numeric text field with sanitising input filter and an optional suffix.
 * Keeps the raw string in state so partial input like "1." stays typeable.
 */
@Composable
fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(NumberParsing.sanitizeDecimalInput(raw)) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        supportingText = supportingText?.let {
            { Text(it, style = MaterialTheme.typography.labelSmall) }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

/** Adds consistent vertical rhythm inside forms. */
@Composable
fun FormSpacer(height: Int = 12) {
    Spacer(Modifier.height(height.dp))
}
