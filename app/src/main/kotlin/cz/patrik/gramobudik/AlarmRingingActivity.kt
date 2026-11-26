package cz.patrik.gramobudik

import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.patrik.gramobudik.ui.theme.GramoBudKTheme
import kotlinx.coroutines.delay

private val RingingBackground = Color(0xFF110E19)
private val RingingVinylCoral = Color(0xFFE85D4A)
private val RingingVinylGold = Color(0xFFFFD166)
private val RingingVinylCream = Color(0xFFFFF1E6)
private val RingingRedStop = Color(0xFFD32F2F)

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        forceMaxAlarmVolume()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        val fallbackSongName = PlaybackState.currentSongName.value.takeIf { it.isNotBlank() }
            ?: AlarmPreferences(this).getAlarms().firstOrNull { !it.songName.isNullOrBlank() }?.songName
            ?: "Vybraná písnička"

        setContent {
            GramoBudKTheme(darkTheme = true) {
                val isPlaying by PlaybackState.isPlaying.collectAsStateWithLifecycle()
                val currentSongName by PlaybackState.currentSongName.collectAsStateWithLifecycle()
                var hasObservedPlaying by remember { mutableStateOf(PlaybackState.isPlaying.value) }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        hasObservedPlaying = true
                    } else if (hasObservedPlaying) {
                        finish()
                    } else {
                        delay(1500L)
                        if (!PlaybackState.isPlaying.value) {
                            finish()
                        }
                    }
                }

                AlarmRingingScreen(
                    songName = currentSongName.ifBlank { fallbackSongName },
                    onStopClick = {
                        PlaybackService.stopPlayback(this@AlarmRingingActivity)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forceMaxAlarmVolume()
    }

    private fun forceMaxAlarmVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (_: SecurityException) {
        }
    }
}

@Composable
private fun AlarmRingingScreen(
    songName: String,
    onStopClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF211834),
                        RingingBackground,
                        Color(0xFF0A0810),
                    ),
                ),
            )
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF251F36),
                border = BorderStroke(1.dp, RingingVinylCoral.copy(alpha = 0.55f)),
            ) {
                Text(
                    text = "GRAMOBUDÍK • BUDÍK ZVONÍ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RingingVinylCream,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                SpinningVinylCanvas(modifier = Modifier.size(248.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = songName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6F3FF),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B2218),
                        border = BorderStroke(1.dp, RingingVinylGold.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = "Hlasitost: 100 %",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RingingVinylGold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onStopClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RingingRedStop,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "ZASTAVIT PÍSNIČKU",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SpinningVinylCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinyl_rotation",
    )
    val noteFloatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "note_float",
    )

    Canvas(modifier = modifier) {
        val discCenter = Offset(size.width * 0.48f, size.height * 0.54f)
        val discRadius = size.minDimension * 0.41f

        // Warm ambient halo behind the vinyl
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RingingVinylCoral.copy(alpha = 0.28f),
                    Color(0xFF422854).copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = discCenter,
                radius = discRadius * 1.32f,
            ),
            radius = discRadius * 1.32f,
            center = discCenter,
        )

        rotate(degrees = rotationAngle, pivot = discCenter) {
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
                style = Stroke(width = 2.dp.toPx()),
            )

            // Glossy light reflection sheen arcs so rotation is clearly visible
            val sheenDiameter = discRadius * 1.44f
            val sheenTopLeft = Offset(
                discCenter.x - sheenDiameter / 2f,
                discCenter.y - sheenDiameter / 2f,
            )
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = -35f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = sheenTopLeft,
                size = Size(sheenDiameter, sheenDiameter),
                style = Stroke(width = discRadius * 0.42f),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 145f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = sheenTopLeft,
                size = Size(sheenDiameter, sheenDiameter),
                style = Stroke(width = discRadius * 0.42f),
            )

            // Concentric groove rings
            listOf(0.90f, 0.81f, 0.72f, 0.63f, 0.54f, 0.45f).forEach { factor ->
                drawCircle(
                    color = Color(0xFF2B2B35),
                    radius = discRadius * factor,
                    center = discCenter,
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }

            // Warm coral-red center label
            drawCircle(
                color = RingingVinylCoral,
                radius = discRadius * 0.34f,
                center = discCenter,
            )

            // Inner cream ring on label
            drawCircle(
                color = RingingVinylCream,
                radius = discRadius * 0.25f,
                center = discCenter,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Label marker dots to emphasize rotation
            val markerOffset = discRadius * 0.165f
            drawCircle(
                color = RingingVinylCream.copy(alpha = 0.85f),
                radius = discRadius * 0.025f,
                center = Offset(discCenter.x + markerOffset, discCenter.y),
            )
            drawCircle(
                color = RingingVinylCream.copy(alpha = 0.85f),
                radius = discRadius * 0.025f,
                center = Offset(discCenter.x - markerOffset, discCenter.y),
            )

            // Center spindle hole
            drawCircle(
                color = Color(0xFF1A1625),
                radius = discRadius * 0.075f,
                center = discCenter,
            )
            drawCircle(
                color = Color(0xFFB7B0C9),
                radius = discRadius * 0.075f,
                center = discCenter,
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }

        // Golden musical notes overlapping upper-right edge
        val floatPx = noteFloatOffset.dp.toPx()
        drawRingingBeamedNotes(
            topLeft = Offset(
                size.width * 0.65f,
                size.height * 0.08f + floatPx * 0.6f,
            ),
            boxSize = Size(size.width * 0.24f, size.height * 0.24f),
            color = RingingVinylGold,
        )
        drawRingingSingleNote(
            topLeft = Offset(
                size.width * 0.82f,
                size.height * 0.29f - floatPx * 0.45f,
            ),
            boxSize = Size(size.width * 0.13f, size.height * 0.16f),
            color = RingingVinylGold.copy(alpha = 0.92f),
        )
    }
}

private fun DrawScope.drawRingingBeamedNotes(
    topLeft: Offset,
    boxSize: Size,
    color: Color,
) {
    val left = topLeft.x
    val top = topLeft.y
    val w = boxSize.width
    val h = boxSize.height

    val leftHeadCenter = Offset(left + 0.24f * w, top + 0.78f * h)
    val rightHeadCenter = Offset(left + 0.76f * w, top + 0.68f * h)
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

    val stemWidth = (0.09f * w).coerceAtLeast(2f)
    val leftStemX = left + 0.35f * w
    val rightStemX = left + 0.87f * w
    val leftTopY = top + 0.20f * h
    val rightTopY = top + 0.10f * h

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
        val beamThickness = (0.14f * h).coerceAtLeast(3f)
        moveTo(leftStemX - stemWidth * 0.4f, leftTopY)
        lineTo(rightStemX + stemWidth * 0.4f, rightTopY)
        lineTo(rightStemX + stemWidth * 0.4f, rightTopY + beamThickness)
        lineTo(leftStemX - stemWidth * 0.4f, leftTopY + beamThickness)
        close()
    }
    drawPath(path = beamPath, color = color)
}

private fun DrawScope.drawRingingSingleNote(
    topLeft: Offset,
    boxSize: Size,
    color: Color,
) {
    val left = topLeft.x
    val top = topLeft.y
    val w = boxSize.width
    val h = boxSize.height

    val headCenter = Offset(left + 0.35f * w, top + 0.76f * h)
    val headSize = Size(0.42f * w, 0.26f * h)

    rotate(degrees = -20f, pivot = headCenter) {
        drawOval(
            color = color,
            topLeft = Offset(headCenter.x - headSize.width / 2f, headCenter.y - headSize.height / 2f),
            size = headSize,
        )
    }

    val stemWidth = (0.11f * w).coerceAtLeast(2f)
    val stemX = left + 0.51f * w
    val topY = top + 0.14f * h

    drawLine(
        color = color,
        start = Offset(stemX, headCenter.y - 0.02f * h),
        end = Offset(stemX, topY),
        strokeWidth = stemWidth,
        cap = StrokeCap.Round,
    )

    val flagPath = Path().apply {
        moveTo(stemX, topY)
        quadraticTo(left + 0.94f * w, top + 0.26f * h, left + 0.80f * w, top + 0.48f * h)
    }
    drawPath(
        path = flagPath,
        color = color,
        style = Stroke(width = stemWidth, cap = StrokeCap.Round),
    )
}
