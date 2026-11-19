package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ShoppingQuest
import com.example.data.model.SpinWheelSlice
import com.example.data.model.StoreOffersConfig
import com.example.data.model.UserAccount
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DealsScreen(
    currentUser: FirebaseUser?,
    userAccount: UserAccount?,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    quests: List<ShoppingQuest>,
    lastWonSlice: SpinWheelSlice?,
    spinStatusMessage: String?,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onSignInClick: () -> Unit,
    onSpinClick: ((SpinWheelSlice) -> Unit) -> Unit,
    onClaimQuestClick: (ShoppingQuest) -> Unit,
    onGoToCartClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val wheelRotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingLockMs = userAccount?.millisUntilNextSpin(nowMs) ?: 0L
    val canSpin = currentUser != null && remainingLockMs == 0L && !isSpinning

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .testTag("deals_rewards_screen")
    ) {
        // Header Wallet & Coin Balance Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appLanguage.tr(
                                english = "Nav Coins & Daily Rewards",
                                punjabi = "ਨਵ ਕੌਇਨਜ਼ & ਸਪਿਨ ਰੀਵਾਰਡਜ਼",
                                hindi = "नव कॉइन्स और डेली रिवॉर्ड्स"
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        val coinRuleLine = if (offersConfig.coinConfig.isCoinsEnabled) {
                            "Earn ${StoreOffersConfig.formatPercent(offersConfig.coinConfig.earnRatePercent)}% Back • 1 Coin = ₹${StoreOffersConfig.formatPercent(offersConfig.coinConfig.coinValueInr)} • Max ${StoreOffersConfig.formatPercent(offersConfig.coinConfig.maxRedeemPercent)}%/order"
                        } else {
                            "Nav Coins redemption temporarily paused by store"
                        }
                        Text(
                            text = if (currentUser == null) "Sign in with Gmail to spin & earn coins • $coinRuleLine"
                            else "${currentUser.email} • $coinRuleLine",
                            style = MaterialTheme.typography.labelSmall.copy(color = SignatureGoldContainer)
                        )
                    }

                    Surface(
                        color = SignatureGold,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ForestGreenDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${userAccount?.navCoins ?: 0} Coins",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ForestGreenDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Interactive Forest Green & Gold Spin the Wheel Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, WarmCreamBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appLanguage.tr(
                                english = "Spin & Win Wheel (1 Spin / 24h)",
                                punjabi = "ਸਪਿਨ & ਵਿਨ (24 ਘੰਟਿਆਂ ਵਿੱਚ 1 ਵਾਰ)",
                                hindi = "स्पिन और जीतें (24 घंटे में 1 बार)"
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = ForestGreenDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SpinWheelCanvas(
                        rotationDegrees = wheelRotation.value,
                        modifier = Modifier
                            .size(256.dp)
                            .testTag("spin_wheel_canvas")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser == null) {
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("spin_signin_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google to Spin & Win", fontWeight = FontWeight.ExtraBold)
                        }
                    } else if (remainingLockMs > 0L) {
                        Surface(
                            color = ForestGreenContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Next Spin in ${formatCountdown(remainingLockMs)} (Server 24h Lock)",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = ForestGreenDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!canSpin) return@Button
                                isSpinning = true
                                onSpinClick { wonSlice ->
                                    val idx = SpinWheelSlice.entries.indexOf(wonSlice)
                                    val sliceDeg = 360f / SpinWheelSlice.entries.size
                                    val targetCenter = 270f - (idx * sliceDeg + sliceDeg / 2f)
                                    val targetAngle = wheelRotation.value + 1440f + ((targetCenter - (wheelRotation.value % 360f) + 360f) % 360f)
                                    coroutineScope.launch {
                                        wheelRotation.animateTo(
                                            targetValue = targetAngle,
                                            animationSpec = tween(durationMillis = 3200, easing = FastOutSlowInEasing)
                                        )
                                        isSpinning = false
                                    }
                                }
                            },
                            enabled = canSpin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignatureGold,
                                contentColor = ForestGreenDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("spin_now_btn")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) {
                                    appLanguage.tr("Spinning Wheel...", "ਘੁੰਮ ਰਿਹਾ ਹੈ...", "पहिया घूम रहा है...")
                                } else {
                                    appLanguage.tr("SPIN NOW & WIN REWARD", "ਹੁਣੇ ਸਪਿਨ ਕਰੋ ਅਤੇ ਇਨਾਮ ਜਿੱਤੋ", "अभी स्पिन करें और इनाम जीतें")
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Active Reward / Won Slice Banner
                    val activeLabel = userAccount?.takeIf { it.hasValidSpinCoupon() }?.activeSpinCouponLabel
                        ?: lastWonSlice?.displayTitle(appLanguage)
                        ?: spinStatusMessage

                    if (!activeLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SignatureGoldContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Redeem, contentDescription = null, tint = ForestGreenDark, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ForestGreenDark,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                                Button(
                                    onClick = onGoToCartClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Use in Cart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily & Weekly Shopping Quests Tracker Section
        item {
            Text(
                text = appLanguage.tr(
                    english = "Daily & Weekly Shopping Quests",
                    punjabi = "ਰੋਜ਼ਾਨਾ ਤੇ ਹਫ਼ਤਾਵਾਰੀ ਸ਼ਾਪਿੰਗ ਕੁਐਸਟਸ",
                    hindi = "दैनिक और साप्ताहिक शॉपिंग क्वेस्ट्स"
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ForestGreenDark,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        items(quests, key = { it.id }) { quest ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (quest.completed) ForestGreen else WarmCreamBorder),
                modifier = Modifier.testTag("quest_card_${quest.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quest.displayTitle(appLanguage),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenDark
                                )
                            )
                            Text(
                                text = quest.description,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A6D63))
                            )
                        }

                        Surface(
                            color = if (quest.claimed) ForestGreenContainer else SignatureGoldContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${quest.rewardCoins} Coins",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ForestGreenDark
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val fraction = (quest.progress.toFloat() / quest.target.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        color = ForestGreen,
                        trackColor = ForestGreenContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress: ${quest.progress} / ${quest.target}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                        )

                        if (quest.claimed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Claimed", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                            }
                        } else if (quest.completed) {
                            Button(
                                onClick = { onClaimQuestClick(quest) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("claim_quest_${quest.id}")
                            ) {
                                Text("Claim Coins", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF7A8B82), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("In Progress", fontSize = 11.sp, color = Color(0xFF7A8B82), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinWheelCanvas(
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val slices = SpinWheelSlice.entries
    val shortLabels = listOf("₹20 Off", "Free 15m", "50 Coins", "100 Coins", "Free 2hr", "Try Again")
    val sliceColors = listOf(
        ForestGreen,
        SignatureGold,
        ForestGreenDark,
        Color(0xFFC98A06),
        Color(0xFF196040),
        Color(0xFFE5D29A)
    )
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 360f / slices.size
            val wheelDiameter = size.minDimension - 22f
            val topLeft = Offset((size.width - wheelDiameter) / 2f, (size.height - wheelDiameter) / 2f)

            // Outer Gold Ring
            drawCircle(
                color = SignatureGold,
                radius = size.minDimension / 2f
            )

            rotate(degrees = rotationDegrees) {
                slices.forEachIndexed { idx, _ ->
                    val startAngle = idx * sweepAngle
                    val fillColor = sliceColors[idx % sliceColors.size]
                    drawArc(
                        color = fillColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = topLeft,
                        size = Size(wheelDiameter, wheelDiameter)
                    )

                    val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                    val textColor = if (idx % 2 == 0) Color.White else ForestGreenDark
                    val labelLayout = textMeasurer.measure(
                        text = shortLabels[idx],
                        style = TextStyle(
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    val radius = wheelDiameter * 0.30f
                    val tx = (size.width / 2f + Math.cos(midAngleRad) * radius - labelLayout.size.width / 2f).toFloat()
                    val ty = (size.height / 2f + Math.sin(midAngleRad) * radius - labelLayout.size.height / 2f).toFloat()
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(tx, ty)
                    )
                }
            }

            // Center Gold Hub
            drawCircle(color = ForestGreenDark, radius = 30f)
            drawCircle(color = SignatureGold, radius = 20f)

            // Top Pointer Triangle
            val pointerPath = Path().apply {
                moveTo(size.width / 2f - 14f, 0f)
                lineTo(size.width / 2f + 14f, 0f)
                lineTo(size.width / 2f, 24f)
                close()
            }
            drawPath(path = pointerPath, color = Color(0xFFB71C1C))
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
