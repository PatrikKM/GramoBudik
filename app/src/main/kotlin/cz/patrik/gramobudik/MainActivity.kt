package cz.patrik.gramobudik

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.patrik.gramobudik.ui.theme.GramoBudKTheme
import java.util.Calendar
import java.util.Locale

private val VinylBackground = Color(0xFF13101C)
private val VinylSurface = Color(0xFF1D182B)
private val VinylSurfaceElevated = Color(0xFF262038)
private val VinylBorder = Color(0xFF332B4A)
private val VinylCoral = Color(0xFFE85D4A)
private val VinylGold = Color(0xFFFFD166)
private val VinylCream = Color(0xFFFFF1E6)
private val VinylRedStop = Color(0xFFD32F2F)
private val VinylRedBannerBg = Color(0xFF2D151C)
private val VinylWarningBg = Color(0xFF282019)
private val VinylSuccessGreen = Color(0xFF34D399)
private val VinylSuccessBg = Color(0xFF152922)
private val VinylTextPrimary = Color(0xFFF6F3FF)
private val VinylTextSecondary = Color(0xFFB7B0C9)
private val VinylTextMuted = Color(0xFF8C84A3)

private val VinylDarkColorScheme = darkColorScheme(
    primary = VinylCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A241E),
    onPrimaryContainer = VinylCream,
    secondary = VinylGold,
    onSecondary = Color(0xFF241B00),
    secondaryContainer = Color(0xFF3A301A),
    onSecondaryContainer = VinylGold,
    tertiary = Color(0xFFA78BFA),
    background = VinylBackground,
    onBackground = VinylTextPrimary,
    surface = VinylSurface,
    onSurface = VinylTextPrimary,
    surfaceVariant = VinylSurfaceElevated,
    onSurfaceVariant = VinylTextSecondary,
    surfaceContainer = VinylSurface,
    surfaceContainerHigh = VinylSurfaceElevated,
    surfaceContainerHighest = Color(0xFF302845),
    outline = VinylBorder,
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        setContent {
            GramoBudKTheme(darkTheme = true) {
                MaterialTheme(
                    colorScheme = VinylDarkColorScheme,
                    typography = MaterialTheme.typography,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = VinylBackground,
                    ) {
                        GramoBudikMainScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GramoBudikMainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) { AlarmPreferences(context) }

    var songUri by remember { mutableStateOf(prefs.songUri) }
    var songName by remember { mutableStateOf(prefs.songName) }
    var hour by remember { mutableIntStateOf(prefs.hour) }
    var minute by remember { mutableIntStateOf(prefs.minute) }
    var isScheduled by remember { mutableStateOf(prefs.isScheduled) }
    var scheduledTriggerMillis by remember { mutableLongStateOf(prefs.scheduledTriggerMillis) }

    var hasNotificationPermission by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context))
    }
    var ignoringBatteryOptimizations by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    val isPlaying by PlaybackState.isPlaying.collectAsStateWithLifecycle()
    val currentSongName by PlaybackState.currentSongName.collectAsStateWithLifecycle()

    fun syncFromPreferencesAndPermissions() {
        songUri = prefs.songUri
        songName = prefs.songName
        hour = prefs.hour
        minute = prefs.minute
        isScheduled = prefs.isScheduled
        scheduledTriggerMillis = prefs.scheduledTriggerMillis
        hasNotificationPermission = hasNotificationPermission(context)
        canScheduleExactAlarms = AlarmScheduler.canScheduleExactAlarms(context)
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncFromPreferencesAndPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isPlaying) {
        isScheduled = prefs.isScheduled
        scheduledTriggerMillis = prefs.scheduledTriggerMillis
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || hasNotificationPermission(context)
    }

    val pickSongLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            val displayName = resolveDisplayName(context, uri)
            val uriString = uri.toString()
            prefs.songUri = uriString
            prefs.songName = displayName
            songUri = uriString
            songName = displayName
            feedbackMessage = null
            isFeedbackError = false
        }
    }

    val needsNotificationPermission =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission
    val needsExactAlarmPermission =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1730),
                        VinylBackground,
                        Color(0xFF0E0C15),
                    ),
                ),
            )
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderSection()

            if (isPlaying) {
                ActivePlaybackBanner(
                    playingSongName = currentSongName.ifBlank {
                        songName?.takeIf { it.isNotBlank() } ?: "Vybraná písnička"
                    },
                    onStopClick = { PlaybackService.stopPlayback(context) },
                )
            }

            if (needsNotificationPermission) {
                PermissionCard(
                    title = "Povolení oznámení",
                    description = "Aby bylo možné budík kdykoli zastavit z oznamovací lišty, povolte zobrazování oznámení.",
                    buttonText = "Povolit oznámení",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }

            if (needsExactAlarmPermission) {
                PermissionCard(
                    title = "Přesný budík",
                    description = "Pro spolehlivé spuštění písničky v přesný čas i při vypnutém displeji povolte oprávnění pro přesné budíky.",
                    buttonText = "Povolit přesný budík",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            )
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            if (!ignoringBatteryOptimizations) {
                PermissionCard(
                    title = "Optimalizace baterie",
                    description = "Aby systém neuspal budík při vypnuté obrazovce, povolte běh aplikace bez omezení baterie (na Xiaomi/HyperOS také Automatické spuštění).",
                    buttonText = "Povolit bez omezení",
                    onClick = {
                        val requestIntent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        )
                        try {
                            context.startActivity(requestIntent)
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            SongSelectionCard(
                songName = when {
                    !songName.isNullOrBlank() -> songName!!
                    !songUri.isNullOrBlank() -> "Vybraná písnička"
                    else -> "Žádná písnička nevybrána"
                },
                hasSongSelected = !songUri.isNullOrBlank(),
                onSelectSongClick = { pickSongLauncher.launch(arrayOf("audio/*")) },
            )

            LaunchTimeCard(
                hour = hour,
                minute = minute,
                isScheduled = isScheduled,
                scheduledTriggerMillis = scheduledTriggerMillis,
                onChangeTimeClick = { showTimePickerDialog = true },
            )

            ControlsSection(
                isScheduled = isScheduled,
                feedbackMessage = feedbackMessage,
                isFeedbackError = isFeedbackError,
                onScheduleOrCancelClick = {
                    if (isScheduled) {
                        AlarmScheduler.cancel(context)
                        isScheduled = false
                        scheduledTriggerMillis = 0L
                        feedbackMessage = "Naplánování bylo zrušeno."
                        isFeedbackError = false
                    } else {
                        val currentExactAllowed = AlarmScheduler.canScheduleExactAlarms(context)
                        canScheduleExactAlarms = currentExactAllowed
                        when {
                            songUri.isNullOrBlank() -> {
                                feedbackMessage = "Nejprve vyberte písničku, která se má přehrát."
                                isFeedbackError = true
                            }
                            !currentExactAllowed -> {
                                feedbackMessage = "Chybí oprávnění pro přesný budík. Povolte ho prosím výše."
                                isFeedbackError = true
                            }
                            else -> {
                                val triggerMillis = AlarmScheduler.schedule(context, hour, minute)
                                isScheduled = true
                                scheduledTriggerMillis = triggerMillis
                                feedbackMessage = formatScheduledStatus(triggerMillis, hour, minute)
                                isFeedbackError = false
                            }
                        }
                    }
                },
                onTestPlaybackClick = {
                    val uri = songUri
                    if (uri.isNullOrBlank()) {
                        feedbackMessage = "Nejprve vyberte písničku, kterou chcete vyzkoušet."
                        isFeedbackError = true
                    } else {
                        val resolvedName = songName?.takeIf { it.isNotBlank() } ?: "Vybraná písnička"
                        PlaybackService.startPlayback(
                            context = context,
                            songUri = uri,
                            songName = resolvedName,
                            openRingingUi = false,
                        )
                        feedbackMessage = null
                        isFeedbackError = false
                    }
                },
            )

            HyperOsGuideCard(
                initiallyExpanded = isXiaomiFamilyDevice(),
                onOpenAppSettingsClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Písnička se spustí i při vypnutém displeji vždy na 100 % hlasitosti.",
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
            )
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(min = 280.dp, max = 380.dp),
            containerColor = VinylSurfaceElevated,
            titleContentColor = VinylTextPrimary,
            textContentColor = VinylTextPrimary,
            title = {
                Text(
                    text = "Vybrat čas spuštění",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newHour = timePickerState.hour
                        val newMinute = timePickerState.minute
                        hour = newHour
                        minute = newMinute
                        prefs.hour = newHour
                        prefs.minute = newMinute

                        if (isScheduled) {
                            if (AlarmScheduler.canScheduleExactAlarms(context) && !songUri.isNullOrBlank()) {
                                val newTrigger = AlarmScheduler.schedule(context, newHour, newMinute)
                                scheduledTriggerMillis = newTrigger
                                feedbackMessage = formatScheduledStatus(newTrigger, newHour, newMinute)
                                isFeedbackError = false
                            } else {
                                AlarmScheduler.cancel(context)
                                isScheduled = false
                                scheduledTriggerMillis = 0L
                                feedbackMessage = null
                                isFeedbackError = false
                            }
                        }
                        showTimePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VinylCoral,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Uložit", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Zrušit", color = VinylTextSecondary)
                }
            },
        )
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylLogoBadge(modifier = Modifier.size(62.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "GramoBudík",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = VinylTextPrimary,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = "Hudební budík na 100 % hlasitosti",
                style = MaterialTheme.typography.bodyMedium,
                color = VinylTextSecondary,
            )
        }
    }
}

@Composable
private fun VinylLogoBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val discCenter = Offset(size.width * 0.44f, size.height * 0.56f)
        val discRadius = size.minDimension * 0.42f

        // Subtle glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VinylCoral.copy(alpha = 0.25f), Color.Transparent),
                center = discCenter,
                radius = discRadius * 1.25f,
            ),
            radius = discRadius * 1.25f,
            center = discCenter,
        )

        // Charcoal vinyl disc
        drawCircle(
            color = Color(0xFF121216),
            radius = discRadius,
            center = discCenter,
        )
        drawCircle(
            color = Color(0xFF39324D),
            radius = discRadius,
            center = discCenter,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Groove rings
        listOf(0.82f, 0.67f, 0.52f).forEach { factor ->
            drawCircle(
                color = Color(0xFF2B2B35),
                radius = discRadius * factor,
                center = discCenter,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Coral center label + cream ring + spindle hole
        drawCircle(
            color = VinylCoral,
            radius = discRadius * 0.35f,
            center = discCenter,
        )
        drawCircle(
            color = VinylCream,
            radius = discRadius * 0.25f,
            center = discCenter,
            style = Stroke(width = 1.2.dp.toPx()),
        )
        drawCircle(
            color = Color(0xFF1A1625),
            radius = discRadius * 0.09f,
            center = discCenter,
        )

        // Musical note icon badge in upper-right overlapping the disc
        val badgeCenter = Offset(size.width * 0.77f, size.height * 0.27f)
        val badgeRadius = size.minDimension * 0.22f
        drawCircle(
            color = Color(0xFF251F36),
            radius = badgeRadius,
            center = badgeCenter,
        )
        drawCircle(
            color = VinylGold.copy(alpha = 0.75f),
            radius = badgeRadius,
            center = badgeCenter,
            style = Stroke(width = 1.4.dp.toPx()),
        )

        val noteSize = Size(badgeRadius * 1.15f, badgeRadius * 1.15f)
        drawBeamedEighthNotes(
            topLeft = Offset(
                badgeCenter.x - noteSize.width * 0.52f,
                badgeCenter.y - noteSize.height * 0.48f,
            ),
            boxSize = noteSize,
            color = VinylGold,
        )
    }
}

@Composable
private fun ActivePlaybackBanner(
    playingSongName: String,
    onStopClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VinylRedBannerBg),
        border = BorderStroke(1.5.dp, Color(0xFFEF5350)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = Color(0xFFFF5252))
                }
                Text(
                    text = "PRÁVĚ HRAJE • 100 % HLASITOST",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB4AB),
                    letterSpacing = 0.8.sp,
                )
            }
            Text(
                text = playingSongName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VinylRedStop,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "ZASTAVIT PÍSNIČKU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VinylWarningBg),
        border = BorderStroke(1.dp, VinylGold.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VinylGold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VinylTextSecondary,
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VinylGold,
                    contentColor = Color(0xFF1D1504),
                ),
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SongSelectionCard(
    songName: String,
    hasSongSelected: Boolean,
    onSelectSongClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VinylSurface),
        border = BorderStroke(1.dp, VinylBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    drawBeamedEighthNotes(
                        topLeft = Offset.Zero,
                        boxSize = size,
                        color = if (hasSongSelected) VinylGold else VinylTextMuted,
                    )
                }
                Text(
                    text = "Písnička",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VinylTextPrimary,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = VinylSurfaceElevated,
            ) {
                Text(
                    text = songName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasSongSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (hasSongSelected) VinylTextPrimary else VinylTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }

            OutlinedButton(
                onClick = onSelectSongClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VinylCoral.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylCream),
            ) {
                Text(
                    text = "Vybrat písničku",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LaunchTimeCard(
    hour: Int,
    minute: Int,
    isScheduled: Boolean,
    scheduledTriggerMillis: Long,
    onChangeTimeClick: () -> Unit,
) {
    val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VinylSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isScheduled) VinylSuccessGreen.copy(alpha = 0.55f) else VinylBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClockIcon(
                    modifier = Modifier.size(22.dp),
                    color = if (isScheduled) VinylSuccessGreen else VinylCoral,
                )
                Text(
                    text = "Čas spuštění",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VinylTextPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VinylTextPrimary,
                    letterSpacing = 1.sp,
                )

                OutlinedButton(
                    onClick = onChangeTimeClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VinylBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = VinylSurfaceElevated,
                        contentColor = VinylTextPrimary,
                    ),
                ) {
                    Text(
                        text = "Změnit čas",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (isScheduled) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = VinylSuccessBg,
                    border = BorderStroke(1.dp, VinylSuccessGreen.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = formatScheduledStatus(scheduledTriggerMillis, hour, minute),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VinylSuccessGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlsSection(
    isScheduled: Boolean,
    feedbackMessage: String?,
    isFeedbackError: Boolean,
    onScheduleOrCancelClick: () -> Unit,
    onTestPlaybackClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Ovládání",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = VinylTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        if (isScheduled) {
            OutlinedButton(
                onClick = onScheduleOrCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, VinylCoral),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF28191D),
                    contentColor = Color(0xFFFFB4AB),
                ),
            ) {
                Text(
                    text = "Zrušit naplánování",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Button(
                onClick = onScheduleOrCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VinylCoral,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Naplánovat spuštění",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Button(
            onClick = onTestPlaybackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VinylSurfaceElevated,
                contentColor = VinylGold,
            ),
            border = BorderStroke(1.dp, VinylGold.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "Vyzkoušet písničku (100 %)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!feedbackMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isFeedbackError) Color(0xFF31181F) else VinylSuccessBg,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isFeedbackError) Color(0xFFFF6B6B).copy(alpha = 0.5f) else VinylSuccessGreen.copy(alpha = 0.4f),
                ),
            ) {
                Text(
                    text = feedbackMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isFeedbackError) Color(0xFFFFB4AB) else VinylSuccessGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ClockIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.42f, center.y + radius * 0.15f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBeamedEighthNotes(
    topLeft: Offset,
    boxSize: Size,
    color: Color,
) {
    val left = topLeft.x
    val top = topLeft.y
    val w = boxSize.width
    val h = boxSize.height

    val leftHeadCenter = Offset(left + 0.25f * w, top + 0.78f * h)
    val rightHeadCenter = Offset(left + 0.75f * w, top + 0.68f * h)
    val headSize = Size(0.28f * w, 0.20f * h)

    rotate(degrees = -20f, pivot = leftHeadCenter) {
        drawOval(
            color = color,
            topLeft = Offset(leftHeadCenter.x - headSize.width / 2f, leftHeadCenter.y - headSize.height / 2f),
            size = headSize,
        )
    }
    rotate(degrees = -20f, pivot = rightHeadCenter) {
        drawOval(
            color = color,
            topLeft = Offset(rightHeadCenter.x - headSize.width / 2f, rightHeadCenter.y - headSize.height / 2f),
            size = headSize,
        )
    }

    val stemWidth = (0.09f * w).coerceAtLeast(1.5f)
    val leftStemX = left + 0.36f * w
    val rightStemX = left + 0.86f * w
    val leftTopY = top + 0.22f * h
    val rightTopY = top + 0.12f * h

    drawLine(
        color = color,
        start = Offset(leftStemX, leftHeadCenter.y - 0.02f * h),
        end = Offset(leftStemX, leftTopY),
        strokeWidth = stemWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(rightStemX, rightHeadCenter.y - 0.02f * h),
        end = Offset(rightStemX, rightTopY),
        strokeWidth = stemWidth,
        cap = StrokeCap.Round,
    )

    val beamPath = Path().apply {
        val beamThickness = (0.14f * h).coerceAtLeast(2f)
        moveTo(leftStemX - stemWidth * 0.4f, leftTopY)
        lineTo(rightStemX + stemWidth * 0.4f, rightTopY)
        lineTo(rightStemX + stemWidth * 0.4f, rightTopY + beamThickness)
        lineTo(leftStemX - stemWidth * 0.4f, leftTopY + beamThickness)
        close()
    }
    drawPath(path = beamPath, color = color)
}

private fun isXiaomiFamilyDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
    val brand = Build.BRAND.lowercase(Locale.ROOT)
    return manufacturer in setOf("xiaomi", "redmi", "poco") ||
        brand in setOf("xiaomi", "redmi", "poco")
}

@Composable
private fun HyperOsGuideCard(
    initiallyExpanded: Boolean,
    onOpenAppSettingsClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VinylSurface),
        border = BorderStroke(1.dp, VinylBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Návod pro HyperOS / Xiaomi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VinylGold,
                )
                Text(
                    text = if (expanded) "Skrýt ▲" else "Zobrazit ▼",
                    style = MaterialTheme.typography.labelLarge,
                    color = VinylTextSecondary,
                )
            }

            Text(
                text = "Aby se písnička spolehlivě spustila i při dlouho vypnuté obrazovce, nastavte v detailu aplikace tyto 4 položky:",
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextSecondary,
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    HyperOsStepItem(
                        title = "1. Automatické spuštění na pozadí",
                        detail = "Nastavení → Aplikace → GramoBudík → Povolit automatické spuštění (zabrání zablokování budíku po vypnutí displeje).",
                    )
                    HyperOsStepItem(
                        title = "2. Spořič baterie: Bez omezení",
                        detail = "Nastavení → Aplikace → GramoBudík → Baterie → vyberte „Bez omezení“ (SmartPower neuspí přehrávání).",
                    )
                    HyperOsStepItem(
                        title = "3. Další oprávnění (HyperOS)",
                        detail = "Nastavení → Aplikace → GramoBudík → Další oprávnění → povolte „Zobrazovat na zamykací obrazovce“ a „Spouštět vyskakovací okna na pozadí“ (zobrazí velké tlačítko Zastavit).",
                    )
                    HyperOsStepItem(
                        title = "4. Oznámení a přesné budíky",
                        detail = "V aplikaci nebo v Nastavení povolte „Oznámení“ a „Alarmy a připomenutí“ (přesný budík).",
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedButton(
                        onClick = onOpenAppSettingsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, VinylGold.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylGold),
                    ) {
                        Text(
                            text = "Otevřít nastavení aplikace",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HyperOsStepItem(
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VinylSurfaceElevated,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = VinylTextPrimary,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextSecondary,
            )
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    val queriedName = try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }

    return queriedName?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.takeIf { it.isNotBlank() }
        ?: "Vybraná písnička"
}

private fun formatScheduledStatus(triggerMillis: Long, hour: Int, minute: Int): String {
    val timeStr = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    if (triggerMillis <= 0L) {
        return "Naplánováno na $timeStr"
    }
    val nowCal = Calendar.getInstance()
    val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
    val isToday = nowCal.get(Calendar.YEAR) == triggerCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == triggerCal.get(Calendar.DAY_OF_YEAR)
    val dayLabel = if (isToday) "dnes" else "zítra"
    return "Naplánováno na $timeStr ($dayLabel)"
}
