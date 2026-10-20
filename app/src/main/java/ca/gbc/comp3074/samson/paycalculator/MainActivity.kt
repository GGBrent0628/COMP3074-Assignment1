package ca.gbc.comp3074.samson.paycalculator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.gbc.comp3074.samson.paycalculator.ui.theme.PayCalculatorTheme
import java.io.Serializable
import java.util.Locale

// Constants instead of "magic numbers" in the formulas
private const val REGULAR_HOURS = 40.0
private const val OVERTIME_MULTIPLIER = 1.5

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayCalculatorTheme {
                PayCalculatorScreen(
                    onAboutClick = {
                        // Explicit Intent: open AboutActivity
                        startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                    }
                )
            }
        }
    }
}

// ---------- Calculation (plain Kotlin, no UI) ----------

/** Holds the four values we show to the user. Serializable so it survives rotation. */
data class PayResult(
    val pay: Double,
    val overtimePay: Double,
    val totalPay: Double,
    val tax: Double
) : Serializable

/** Applies the formulas from the assignment. */
fun calculatePay(hours: Double, rate: Double, taxRate: Double): PayResult {
    val pay: Double
    val overtimePay: Double

    if (hours <= REGULAR_HOURS) {
        pay = hours * rate
        overtimePay = 0.0
    } else {
        pay = REGULAR_HOURS * rate
        overtimePay = (hours - REGULAR_HOURS) * rate * OVERTIME_MULTIPLIER
    }

    val totalPay = pay + overtimePay
    val tax = pay * taxRate          // as specified: tax = pay * tax_rate

    return PayResult(pay, overtimePay, totalPay, tax)
}

/** Returns an error message, or null if the text is a valid non-negative number. */
fun validate(text: String): String? {
    val number = text.trim().toDoubleOrNull()
    return when {
        text.isBlank() -> "This field is required"
        number == null -> "Please enter a valid number"
        number < 0 -> "Value cannot be negative"
        else -> null
    }
}

fun formatMoney(amount: Double): String =
    String.format(Locale.getDefault(), "\$%.2f", amount)

// ---------- UI ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCalculatorScreen(onAboutClick: () -> Unit) {
    // Input state (rememberSaveable keeps it when the screen rotates)
    var hoursText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("") }
    var taxRateText by rememberSaveable { mutableStateOf("") }

    // Error messages (null = no error)
    var hoursError by rememberSaveable { mutableStateOf<String?>(null) }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }
    var taxRateError by rememberSaveable { mutableStateOf<String?>(null) }

    // Output state (null = nothing calculated yet)
    var result by rememberSaveable { mutableStateOf<PayResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onAboutClick) { Text("About") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ----- Input -----
            NumberField("Number of hours worked", hoursText, hoursError) {
                hoursText = it
                hoursError = null
            }
            NumberField("Hourly rate", rateText, rateError) {
                rateText = it
                rateError = null
            }
            NumberField("Tax rate (e.g. 0.18 for 18%)", taxRateText, taxRateError) {
                taxRateText = it
                taxRateError = null
            }

            Button(
                onClick = {
                    hoursError = validate(hoursText)
                    rateError = validate(rateText)
                    taxRateError = validate(taxRateText)

                    result = if (hoursError == null && rateError == null && taxRateError == null) {
                        calculatePay(
                            hoursText.trim().toDouble(),
                            rateText.trim().toDouble(),
                            taxRateText.trim().toDouble()
                        )
                    } else {
                        null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate")
            }

            // ----- Output -----
            Spacer(Modifier.height(16.dp))
            ResultLine("Pay", result?.pay)
            ResultLine("Overtime pay", result?.overtimePay)
            ResultLine("Total pay", result?.totalPay, bold = true)
            ResultLine("Tax", result?.tax)
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
fun ResultLine(label: String, amount: Double?, bold: Boolean = false) {
    val text = if (amount == null) "$label:" else "$label: ${formatMoney(amount)}"
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}