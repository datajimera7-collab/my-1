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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.util.TimeFormatter
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import java.util.Locale

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
    var selectedFilterChip by remember { mutableIntStateOf(0) } // 0 = All, 1 = Earnings, 2 = Payouts

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
        transactions.map { resolveEffectiveCoins(it) }.filter { it < 0 }.map { Math.abs(it) }.sum()
    }
    val totalWithdrawnInr = String.format(Locale.US, "%.2f", totalWithdrawnCoins.toDouble() / COINS_PER_INR.toDouble())

    val totalReferralCoins = remember(transactions) {
        transactions.filter {
            it.id.startsWith("ref_withdraw_bonus_") || it.title.contains("Referral", ignoreCase = true)
        }.map { resolveEffectiveCoins(it) }.filter { it > 0 }.sum()
    }

    val withdrawalCount = remember(transactions) {
        transactions.count { resolveEffectiveCoins(it) < 0 }
    }

    // Progress to next 1000 Coins threshold
    val progressToThreshold = remember(walletBalance) {
        val target = if (walletBalance < 1000) 1000f else (Math.ceil(walletBalance / 1000.0) * 1000).toFloat()
        (walletBalance / target).coerceIn(0f, 1f)
    }
    val coinsNeededForNext = remember(walletBalance) {
        if (walletBalance < 1000) (1000 - walletBalance).coerceAtLeast(0) else 0
    }

    val filteredTransactions = remember(transactions, selectedFilterChip) {
        when (selectedFilterChip) {
            1 -> transactions.filter { resolveEffectiveCoins(it) > 0 }
            2 -> transactions.filter { resolveEffectiveCoins(it) < 0 }
            else -> transactions
        }
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
            .background(Color(0xFF070B12))
            .testTag("wallet_screen")
    ) {
        // Luxury Translucent Top Bar (Edge-to-Edge Status Bar Safe)
        Surface(
            color = Color(0xFF0B111E).copy(alpha = 0.95f),
            tonalElevation = 0.dp,
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
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kingo Gold Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Wallet",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(SuccessGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Instant 24x7 UPI Payouts Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Top-Right Passbook / History Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showHistoryPage = true }
                            .testTag("wallet_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Passbook",
                                tint = AmberPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Passbook",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberLight
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Announcements / Tab Banners
            item {
                AdminPostsBannerSection(
                    posts = adminPosts,
                    currentTab = "WALLET",
                    dismissedIds = dismissedPostIds,
                    onDismissPost = { viewModel.dismissAdminPost(it) }
                )
            }

            // Titanium Black-Gold Luxury Debit Card (Hero)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = AmberPrimary.copy(alpha = 0.2f), spotColor = AmberPrimary.copy(alpha = 0.3f))
                        .testTag("wallet_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF161F30),
                                        Color(0xFF0B111E)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        AmberPrimary.copy(alpha = 0.6f),
                                        Color(0xFFFBBF24).copy(alpha = 0.3f),
                                        AmberPrimary.copy(alpha = 0.7f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Card Top Row: EMV Chip + NFC Symbol + Card Brand
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 34.dp, height = 24.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFD97706), Color(0xFFFBBF24))
                                                ),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = "Contactless",
                                        tint = AmberLight.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(AmberPrimary.copy(alpha = 0.15f), RoundedCornerShape(50))
                                        .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(50))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "VIP MEMBER",
                                        color = AmberLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Balance Display
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "AVAILABLE EARNINGS BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = "Coins",
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$walletBalance",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Coins",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    }

                                    // Real Cash Translation Pill
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "≈ ₹$formattedInr",
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Payout Progress Bar & Quick Status
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (coinsNeededForNext > 0) "Need $coinsNeededForNext more Coins for ₹10 Payout" else "🎉 Payout Threshold Unlocked (₹10+ ready)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (coinsNeededForNext > 0) Color(0xFFCBD5E1) else SuccessGreen
                                    )
                                    Text(
                                        text = "1000 C = ₹10",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AmberPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressToThreshold },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (coinsNeededForNext > 0) AmberPrimary else SuccessGreen,
                                    trackColor = Color(0xFF334155)
                                )
                            }

                            // Primary Action Button (High-Budget Glossy Gold Gradient)
                            Button(
                                onClick = { showWithdrawDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("withdraw_action_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Instant Cashout (UPI / Paytm / Bank)",
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Payout Gateways Showcase (Fintech Tier-1 Style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Supported Instant Payout Channels",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "0% Fee • 1-Min Credit",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentChannelPill(
                                icon = Icons.Default.QrCode2,
                                title = "UPI ID",
                                subtitle = "PhonePe/GPay",
                                modifier = Modifier.weight(1f)
                            )
                            PaymentChannelPill(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Paytm",
                                subtitle = "Instant Wallet",
                                modifier = Modifier.weight(1f)
                            )
                            PaymentChannelPill(
                                icon = Icons.Default.AccountBalance,
                                title = "Bank IMPS",
                                subtitle = "Direct Transfer",
                                modifier = Modifier.weight(1f)
                            )
                            PaymentChannelPill(
                                icon = Icons.Default.CardGiftcard,
                                title = "Gift Card",
                                subtitle = "Play / Amazon",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Earnings Overview Analytics (3 Stats Tiles)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Earned Tile
                    StatsSummaryTile(
                        icon = Icons.Default.ArrowUpward,
                        iconTint = SuccessGreen,
                        iconBg = SuccessGreen.copy(alpha = 0.15f),
                        title = "Lifetime Earned",
                        value = "+$totalEarnedCoins c",
                        valueColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )

                    // Total Withdrawn Tile
                    StatsSummaryTile(
                        icon = Icons.Default.ArrowDownward,
                        iconTint = AmberPrimary,
                        iconBg = AmberPrimary.copy(alpha = 0.15f),
                        title = "Total Withdrawn",
                        value = "₹$totalWithdrawnInr",
                        valueColor = AmberLight,
                        modifier = Modifier.weight(1f)
                    )

                    // Referral Royalties Tile
                    StatsSummaryTile(
                        icon = Icons.Default.CardGiftcard,
                        iconTint = Color(0xFF38BDF8),
                        iconBg = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        title = "10% Refer Bonus",
                        value = "+$totalReferralCoins c",
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Transactions Header & Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "${transactions.size}",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Text(
                        text = "See All ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary,
                        modifier = Modifier.clickable { showHistoryPage = true }
                    )
                }
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterChip == 0,
                        onClick = { selectedFilterChip = 0 },
                        label = { Text("All", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (selectedFilterChip == 0) AmberPrimary else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = selectedFilterChip == 1,
                        onClick = { selectedFilterChip = 1 },
                        label = { Text("Earnings (+)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (selectedFilterChip == 1) SuccessGreen else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = selectedFilterChip == 2,
                        onClick = { selectedFilterChip = 2 },
                        label = { Text("Withdrawals (-)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF43F5E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, if (selectedFilterChip == 2) Color(0xFFF43F5E) else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            // Transaction Items List
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AmberPrimary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "No Transactions Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Watch videos from the Tasks tab to start earning real coins!",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions.take(8), key = { it.id }) { tx ->
                    TransactionItemRow(tx = tx)
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
                    tint = SuccessGreen,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Withdrawal Submitted!",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = msg,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            containerColor = Color(0xFF0F172A),
            confirmButton = {
                Button(
                    onClick = { withdrawalSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK, GOT IT", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        )
    }
}

@Composable
private fun PaymentChannelPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.6f),
        border = BorderStroke(0.8.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberLight,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatsSummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TransactionItemRow(tx: WalletTransaction) {
    val effectiveCoins = resolveEffectiveCoins(tx)
    val isPositive = effectiveCoins >= 0
    val formattedTime = TimeFormatter.formatTimestamp(tx.timestampMillis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isPositive) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFF43F5E).copy(alpha = 0.15f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isPositive) SuccessGreen.copy(alpha = 0.4f) else Color(0xFFF43F5E).copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) SuccessGreen else Color(0xFFF43F5E),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        if (!isPositive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "UPI Payout",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberLight,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+$effectiveCoins" else "$effectiveCoins",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPositive) SuccessGreen else Color(0xFFF43F5E)
                )
                Text(
                    text = "Coins",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
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
            .background(Color(0xFF070B12))
            .testTag("wallet_history_page")
    ) {
        // History Page Top Header Bar
        Surface(
            color = Color(0xFF0B111E),
            tonalElevation = 0.dp,
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
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Passbook & Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${transactions.size} Total Lifetime Transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTab == 0,
                        onClick = { filterTab = 0 },
                        label = { Text("All Records", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = filterTab == 1,
                        onClick = { filterTab = 1 },
                        label = { Text("Payouts Only", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF43F5E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = filterTab == 2,
                        onClick = { filterTab = 2 },
                        label = { Text("Earnings Only", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No records in this category",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemRow(tx = tx)
                }
            }
        }
    }
}
