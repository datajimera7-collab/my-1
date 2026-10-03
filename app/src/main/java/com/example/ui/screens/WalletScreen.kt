package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.COINS_PER_INR
import com.example.data.WalletTransaction
import com.example.ui.components.AdminPostsBannerSection
import com.example.ui.components.KingoLogoBadge
import com.example.ui.components.WithdrawDialog
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.util.TimeFormatter
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import java.util.Locale

private val DarkVaultBg = Color(0xFF090D16)
private val CardSurfaceBg = Color(0xFF111827)
private val GoldAccent = Color(0xFFFFB800)
private val EmeraldAccent = Color(0xFF10B981)
private val CyanAccent = Color(0xFF06B6D4)
private val IndigoAccent = Color(0xFF6366F1)

private fun resolveEffectiveCoins(item: WalletTransaction): Int {
    return if (item.coins != 0) {
        item.coins
    } else if (item.title.contains("Payout", ignoreCase = true) || item.title.contains("Withdrawal", ignoreCase = true)) {
        val coinsMatch = Regex("""(\d+)\s*Coins""", RegexOption.IGNORE_CASE).find(item.title)
        val inrMatch = Regex("""₹\s*(\d+(?:\.\d+)?)""").find(item.title)
        when {
            coinsMatch != null -> -(coinsMatch.groupValues[1].toIntOrNull() ?: 1000)
            inrMatch != null -> -(((inrMatch.groupValues[1].toDoubleOrNull() ?: 10.0) * COINS_PER_INR).toInt())
            else -> -1000
        }
    } else {
        0
    }
}

@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val walletBalance by viewModel.walletBalance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val adminPosts by viewModel.adminPosts.collectAsState()
    val dismissedPostIds by viewModel.dismissedPostIds.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showHistoryPage by remember { mutableStateOf(false) }
    var withdrawalSuccessMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (showHistoryPage) {
            showHistoryPage = false
        } else {
            viewModel.switchTab(AppScreen.HOME)
        }
    }

    val inrEquivalent = walletBalance.toDouble() / COINS_PER_INR.toDouble()
    val formattedInr = String.format(Locale.US, "%.2f", inrEquivalent)

    val totalEarnedCoins = remember(transactions) {
        transactions.map { resolveEffectiveCoins(it) }.filter { it > 0 }.sum()
    }
    val totalWithdrawnCoins = remember(transactions) {
        transactions.map { resolveEffectiveCoins(it) }.filter { it < 0 }.sum().let { Math.abs(it) }
    }
    val totalWithdrawnInr = String.format(Locale.US, "%.2f", totalWithdrawnCoins.toDouble() / COINS_PER_INR.toDouble())
    val withdrawalCount = remember(transactions) {
        transactions.count { resolveEffectiveCoins(it) < 0 }
    }
    val referralBonusCoins = remember(transactions) {
        transactions.filter {
            it.id.startsWith("ref_withdraw_bonus_") || it.title.contains("Referral Withdraw Bonus")
        }.sumOf { it.coins.coerceAtLeast(0) }
    }

    if (showHistoryPage) {
        WalletHistoryPage(
            transactions = transactions,
            onBack = { showHistoryPage = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkVaultBg)
            .testTag("wallet_screen")
    ) {
        // High-Budget Executive Header Bar
        Surface(
            color = Color(0xFF0D1424),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        KingoLogoBadge(
                            isAdmin = false,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "KINGO WALLET",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldAccent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .border(0.8.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = EmeraldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = "Instant UPI Disbursal • 1000 C = ₹10",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Passbook / Statement Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showHistoryPage = true }
                            .testTag("wallet_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Passbook",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Passbook",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminPostsBannerSection(
                posts = adminPosts,
                currentTab = "WALLET",
                dismissedIds = dismissedPostIds,
                onDismissPost = { viewModel.dismissAdminPost(it) }
            )

            // Ultra-Luxury Master Vault Balance Card (CRED / Revolut inspired)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_hero_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    Color(0xFF070B14)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    GoldAccent.copy(alpha = 0.6f),
                                    IndigoAccent.copy(alpha = 0.4f),
                                    EmeraldAccent.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card Top Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(GoldAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Instant Payout",
                                        color = EmeraldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Big Prominent Balance Counter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(GoldAccent, AmberDark)
                                        ),
                                        CircleShape
                                    )
                                    .shadow(6.dp, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coin",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format(Locale.US, "%,d", walletBalance),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coins",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                        }

                        // Rupee Cash Conversion Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cash Value:",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "₹$formattedInr INR",
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color.White.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "1000 C = ₹10",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // High-Tech Action Button
                        Button(
                            onClick = { showWithdrawDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .testTag("withdraw_action_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GoldAccent, AmberPrimary, Color(0xFFEA580C))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "WITHDRAW CASH VIA UPI",
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4-Column Live Financial Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Total Earned
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmeraldAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Total Earned",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "+$totalEarnedCoins C",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent
                        )
                    }
                }

                // Metric 2: Total Withdrawn
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Total Payouts",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹$totalWithdrawnInr",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick Payout Channels & Trust Assurance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Supported Instant Payout Channels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "0% Fee",
                            color = EmeraldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }

                    // Payment Channels Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI ID", "PhonePe", "Paytm", "Google Pay").forEach { channel ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = channel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Min withdrawal ₹10 (1000 Coins). Direct transfer to your account.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Recent Transactions Preview Card (with view full history)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Passbook Activity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "View All (${transactions.size})",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { showHistoryPage = true }
                        )
                    }

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No transactions yet",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Complete your first video task to earn coins!",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transactions.take(5).forEach { tx ->
                                TransactionRowItem(tx = tx)
                            }
                        }
                    }
                }
            }
        }
    }

    // Payout Dialog
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentBalance = walletBalance,
            onDismiss = { showWithdrawDialog = false },
            onSubmitWithdrawal = { amount, method, dest ->
                viewModel.withdrawCoins(
                    coins = amount,
                    method = method,
                    destination = dest
                ) { success, msg ->
                    showWithdrawDialog = false
                    if (success) {
                        withdrawalSuccessMessage = msg
                    }
                }
            }
        )
    }

    // Payout Confirmation Dialog
    withdrawalSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { withdrawalSuccessMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(38.dp)
                )
            },
            title = {
                Text(
                    "Payout Submitted!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { withdrawalSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TransactionRowItem(tx: WalletTransaction) {
    val effectiveCoins = resolveEffectiveCoins(tx)
    val isCredit = effectiveCoins >= 0
    val isWithdrawal = effectiveCoins < 0
    val formattedTime = TimeFormatter.formatTimestamp(tx.timestampMillis)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isCredit) EmeraldAccent.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isCredit) EmeraldAccent else GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) "+$effectiveCoins" else "$effectiveCoins",
                    fontWeight = FontWeight.Black,
                    color = if (isCredit) EmeraldAccent else Color(0xFFF87171),
                    fontSize = 14.sp
                )
                Text(
                    text = "Coins",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun WalletHistoryPage(
    transactions: List<WalletTransaction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Withdrawals, 2 = Task Earnings

    val filteredTransactions = remember(transactions, filterTab) {
        when (filterTab) {
            1 -> transactions.filter { resolveEffectiveCoins(it) < 0 }
            2 -> transactions.filter { resolveEffectiveCoins(it) >= 0 }
            else -> transactions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkVaultBg)
            .testTag("wallet_history_page")
    ) {
        // Dedicated History Page Top Header Bar
        Surface(
            color = Color(0xFF0D1424),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("wallet_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Wallet",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Passbook & Payout Statements",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${filteredTransactions.size} total activity records",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "All (${transactions.size})",
                        "Payouts (${transactions.count { resolveEffectiveCoins(it) < 0 }})",
                        "Earnings (${transactions.count { resolveEffectiveCoins(it) >= 0 }})"
                    )
                    tabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = filterTab == idx,
                            onClick = { filterTab = idx },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (filterTab == idx) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = Color.White.copy(alpha = 0.75f)
                            )
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No records found in this category",
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(tx = tx)
                }
            }
        }
    }
}
