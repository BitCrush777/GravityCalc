package com.example.gravitycalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gravitycalc.ui.theme.GravityCalcTheme
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GravityCalcTheme {
                GlassCalculatorScreen()
            }
        }
    }
}

@Composable
fun GlassCalculatorScreen() {
    // --- STATE ---
    var currentInput by remember { mutableStateOf("0") }
    var previousInput by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf<String?>(null) }
    var formula by remember { mutableStateOf("") }
    var isNewInput by remember { mutableStateOf(true) }
    val historyList = remember { mutableStateListOf<String>() }
    
    var showHistory by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    var bgStartColor by remember { mutableStateOf(Color(0xFF2E1A47)) }
    var bgEndColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }

    val hapticFeedback = LocalHapticFeedback.current

    // --- ANIMATION ---
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "colorAnim"
    )

    val currentBgColor1 = Color(
        red = bgStartColor.red + (bgEndColor.red - bgStartColor.red) * animValue * 0.2f,
        green = bgStartColor.green + (bgEndColor.green - bgStartColor.green) * animValue * 0.2f,
        blue = bgStartColor.blue + (bgEndColor.blue - bgStartColor.blue) * animValue * 0.2f,
        alpha = 1f
    )
    val currentBgColor2 = Color(
        red = bgEndColor.red + (bgStartColor.red - bgEndColor.red) * animValue * 0.2f,
        green = bgEndColor.green + (bgStartColor.green - bgEndColor.green) * animValue * 0.2f,
        blue = bgEndColor.blue + (bgStartColor.blue - bgEndColor.blue) * animValue * 0.2f,
        alpha = 1f
    )

    // --- LOGIC FUNCTIONS ---
    fun formatDisplay(value: Double): String {
        return when {
            value.isNaN() -> "Error"
            value.isInfinite() -> "Infinity"
            value % 1.0 == 0.0 -> value.toLong().toString()
            else -> value.toString().let { if (it.length > 12) it.take(12) else it }
        }
    }

    fun calculateResult() {
        val op = operator ?: return
        val v1 = previousInput.toDoubleOrNull() ?: 0.0
        val v2 = currentInput.toDoubleOrNull() ?: 0.0
        val result = when (op) {
            "+" -> v1 + v2
            "−" -> v1 - v2
            "×" -> v1 * v2
            "÷" -> if (v2 != 0.0) v1 / v2 else Double.NaN
            "xʸ" -> v1.pow(v2)
            "mod" -> v1 % v2
            else -> v2
        }
        currentInput = formatDisplay(result)
        previousInput = ""
        operator = null
    }

    fun factorial(n: Double): Double {
        if (n < 0) return Double.NaN
        if (n == 0.0) return 1.0
        var res = 1.0
        for (i in 1..n.toInt()) res *= i
        return res
    }

    val onButtonClick: (String) -> Unit = { label ->
        if (hapticsEnabled) {
            if (label in listOf("=", "+", "−", "×", "÷", "AC")) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        when (label) {
            in "0".."9", "." -> {
                if (isNewInput) { currentInput = if (label == ".") "0." else label; isNewInput = false }
                else if (label != "." || !currentInput.contains(".")) {
                    currentInput = if (currentInput == "0" && label != ".") label else currentInput + label
                }
            }
            "AC" -> { currentInput = "0"; previousInput = ""; operator = null; formula = ""; isNewInput = true }
            "DEL" -> { currentInput = if (currentInput.length > 1) currentInput.dropLast(1) else "0"; isNewInput = (currentInput == "0") }
            "+", "−", "×", "÷", "xʸ", "mod" -> {
                if (operator != null && !isNewInput) calculateResult()
                previousInput = currentInput; operator = label; formula = "$previousInput $label"; isNewInput = true
            }
            "=" -> {
                if (operator != null) {
                    val expression = "$previousInput $operator $currentInput"
                    calculateResult()
                    historyList.add(0, "$expression = $currentInput")
                }
                isNewInput = true
            }
            else -> {
                val value = currentInput.toDoubleOrNull() ?: 0.0
                val result = when (label) {
                    "sin" -> sin(value * PI / 180.0); "cos" -> cos(value * PI / 180.0); "tan" -> tan(value * PI / 180.0)
                    "sin⁻¹" -> asin(value) * 180.0 / PI; "cos⁻¹" -> acos(value) * 180.0 / PI; "tan⁻¹" -> atan(value) * 180.0 / PI
                    "log" -> if (value > 0) log10(value) else Double.NaN; "ln" -> if (value > 0) ln(value) else Double.NaN
                    "√" -> if (value >= 0) sqrt(value) else Double.NaN; "x²" -> value.pow(2.0); "π" -> PI; "e" -> E
                    "!" -> factorial(value); "|x|" -> abs(value); "exp" -> exp(value)
                    else -> value
                }
                formula = if (label !in listOf("(", ")")) "$label($currentInput)" else formula
                currentInput = formatDisplay(result)
                isNewInput = true
            }
        }
    }

    if (showHistory) HistoryDialog(historyList, onDismiss = { showHistory = false })
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showColorPicker) BackgroundColorDialog(bgStartColor, { start, end -> bgStartColor = start; bgEndColor = end }, { showColorPicker = false })

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(currentBgColor1, currentBgColor2)))) {
        Box(modifier = Modifier.size(600.dp).offset(x = (-150).dp, y = (-100).dp).graphicsLayer { rotationZ = animValue * 360f }.blur(150.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MainGlassPanel(currentInput, formula, { showHistory = true }, { showAbout = true }, { showColorPicker = true }, showAdvanced, { showAdvanced = it }, menuExpanded, { menuExpanded = it }, hapticsEnabled, { hapticsEnabled = it }, onButtonClick)
        }
    }
}

@Composable
fun IOSGlassContainer(onDismiss: () -> Unit, title: String, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onDismiss, interactionSource = remember { MutableInteractionSource() }, indication = null), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.85f).shadow(40.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = 0.15f)).border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp)).clickable(enabled = false) { }.padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    content()
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "1.0" } }
    IOSGlassContainer(onDismiss = onDismiss, title = "About") {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GravityCalc", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Text("Version $versionName", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("This app was developed by Saidarshan.K", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
        }
    }
}

@Composable
fun HistoryDialog(history: List<String>, onDismiss: () -> Unit) {
    IOSGlassContainer(onDismiss = onDismiss, title = "History") {
        if (history.isEmpty()) Text("No calculations yet", color = Color.White.copy(alpha = 0.5f))
        else LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history) { item ->
                Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = item, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun BackgroundColorDialog(currentStart: Color, onColorSelect: (Color, Color) -> Unit, onDismiss: () -> Unit) {
    val themes = listOf("Default" to (Color(0xFF2E1A47) to Color(0xFF1A1A2E)), "Red" to (Color(0xFF471A1A) to Color(0xFF2E0F0F)), "Green" to (Color(0xFF1A472E) to Color(0xFF0F2E1A)), "Blue" to (Color(0xFF1A2E47) to Color(0xFF0F1A2E)), "Purple" to (Color(0xFF4A148C) to Color(0xFF311B92)), "Dark" to (Color(0xFF121212) to Color(0xFF000000)))
    IOSGlassContainer(onDismiss = onDismiss, title = "Theme") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            themes.forEach { (name, colors) ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (currentStart == colors.first) Color.White.copy(alpha = 0.2f) else Color.Transparent).clickable { onColorSelect(colors.first, colors.second); onDismiss() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.first).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(name, color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun MainGlassPanel(currentInput: String, formula: String, onHistoryClick: () -> Unit, onAboutClick: () -> Unit, onColorPickerClick: () -> Unit, showAdvanced: Boolean, onAdvancedToggle: (Boolean) -> Unit, menuExpanded: Boolean, onMenuToggle: (Boolean) -> Unit, hapticsEnabled: Boolean, onHapticsToggle: (Boolean) -> Unit, onButtonClick: (String) -> Unit) {
    val glassBrush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
    Box(modifier = Modifier.fillMaxSize().shadow(60.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(glassBrush).border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), RoundedCornerShape(32.dp)).padding(20.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdvancedToggle(!showAdvanced) }) { 
                    Icon(imageVector = if (showAdvanced) Icons.Default.Close else Icons.Default.Settings, contentDescription = "Advanced", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(30.dp)) 
                }
                Box {
                    IconButton(onClick = { onMenuToggle(true) }) { Icon(imageVector = Icons.Default.Menu, "Menu", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(30.dp)) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuToggle(false) }, modifier = Modifier.background(Color(0xFF2E1A47))) {
                        DropdownMenuItem(text = { Text("History", color = Color.White) }, leadingIcon = { Icon(Icons.Default.History, null, tint = Color.White) }, onClick = { onMenuToggle(false); onHistoryClick() })
                        DropdownMenuItem(text = { Text("Background", color = Color.White) }, leadingIcon = { Icon(Icons.Default.Palette, null, tint = Color.White) }, onClick = { onMenuToggle(false); onColorPickerClick() })
                        DropdownMenuItem(text = { Text("Vibration: ${if(hapticsEnabled) "ON" else "OFF"}", color = Color.White) }, onClick = { onHapticsToggle(!hapticsEnabled); onMenuToggle(false) })
                        DropdownMenuItem(text = { Text("About", color = Color.White) }, leadingIcon = { Icon(Icons.Default.Info, null, tint = Color.White) }, onClick = { onMenuToggle(false); onAboutClick() })
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 32.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                if (formula.isNotEmpty()) Text(text = formula, color = Color.White.copy(alpha = 0.45f), fontSize = 24.sp, fontWeight = FontWeight.Light, maxLines = 1)
                val fontSize = when { currentInput.length > 12 -> 48.sp; currentInput.length > 8 -> 64.sp; else -> 90.sp }
                Text(text = currentInput, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.End, maxLines = 1, letterSpacing = (-2).sp, overflow = TextOverflow.Clip)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))
            
            if (showAdvanced) {
                AdvancedPanel(onButtonClick)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sci = listOf(listOf("sin", "cos"), listOf("tan", "log"), listOf("ln", "√"), listOf("xʸ", "π"), listOf("e", "x²"))
                    sci.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { label -> CalcButton(label, Modifier.weight(1f), isScientific = true, onClick = { onButtonClick(label) }) } } }
                }
                Column(Modifier.weight(4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val num = listOf(listOf("AC", "DEL", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("0", ".", "="))
                    num.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { label -> val weight = if (label == "0") 2.1f else 1f; CalcButton(label, Modifier.weight(weight), isOperator = label in "÷×−+=", isSpecial = label in "AC DEL %", onClick = { onButtonClick(label) }) } } }
                }
            }
        }
    }
}

@Composable
fun AdvancedPanel(onButtonClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val advancedRows = listOf(listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "!"), listOf("|x|", "exp", "mod", "("), listOf(")", "asin", "acos", "atan"))
        advancedRows.forEach { row -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { label -> CalcButton(label, Modifier.weight(1f), isScientific = true, onClick = { onButtonClick(label) }) } } }
    }
}

@Composable
fun CalcButton(label: String, modifier: Modifier = Modifier, isScientific: Boolean = false, isOperator: Boolean = false, isSpecial: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "btnScale")
    val opacity by animateFloatAsState(if (isPressed) 0.7f else 1f, label = "btnOpacity")
    val bgColor = when { isOperator -> Color.White.copy(alpha = 0.28f); isSpecial -> Color.White.copy(alpha = 0.18f); isScientific -> Color.White.copy(alpha = 0.08f); else -> Color.White.copy(alpha = 0.18f) }
    val shape = if (isScientific) RoundedCornerShape(16.dp) else if (label == "0") RoundedCornerShape(32.dp) else CircleShape
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }.aspectRatio(if (label == "0") 2.1f else 1f).clip(shape).background(bgColor).border(0.8.dp, Color.White.copy(0.3f), shape).clickable(interactionSource, null) { onClick() }, contentAlignment = Alignment.Center) {
        Text(text = label, color = Color.White, fontSize = if (isScientific) 18.sp else 26.sp, fontWeight = if (isOperator) FontWeight.Bold else FontWeight.Medium)
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GlassCalculatorPreview() {
    GravityCalcTheme {
        GlassCalculatorScreen()
    }
}
