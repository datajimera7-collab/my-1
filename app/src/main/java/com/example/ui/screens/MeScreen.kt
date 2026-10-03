package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.generateSixDigitReferralCode
import com.example.ui.components.AdminPostsBannerSection
import com.example.ui.components.KingoLogoBadge
import com.example.ui.components.SupportChatDialog
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import java.util.Locale

private val DarkMeBg = Color(0xFF090D16)
private val CardSurfaceDark = Color(0xFF111827)
private val GoldAccent = Color(0xFFFFB800)
private val EmeraldAccent = Color(0xFF10B981)
private val PrimaryBlue = Color(0xFF3B82F6)
private val IndigoAccent = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val walletBalance by viewModel.walletBalance.collectAsState()
    val videoTasks by viewModel.videoTasks.collectAsState()
    val adminPosts by viewModel.adminPosts.collectAsState()
    val dismissedPostIds by viewModel.dismissedPostIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val supportMessages by viewModel.supportMessages.collectAsState()
    val appDownloadUrl by viewModel.appDownloadUrl.collectAsState()
    val cloudStatus by viewModel.cloudServerStatus.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSupportChatDialog by remember { mutableStateOf(false) }

    val availableTasks = remember(videoTasks) {
        videoTasks.filter { !it.isCompletionLimitReached || it.isCompleted }
    }
    val completedCount = availableTasks.count { it.isCompleted }

    val myEmail = currentUser?.email?.lowercase() ?: ""
    val myUserId = currentUser?.userId ?: ""
    val myReferralCode = remember(currentUser, myEmail) {
        currentUser?.referralCode?.ifBlank { generateSixDigitReferralCode(myEmail) }
            ?: generateSixDigitReferralCode(myEmail)
    }
    val referredFriendsCount = remember(allUsers, myReferralCode, myEmail) {
        allUsers.count {
            !it.email.equals(myEmail, ignoreCase = true) && it.referredByCode == myReferralCode
        }
    }
    val totalReferralBonusCoins = remember(transactions) {
        transactions.filter {
            it.id.startsWith("ref_withdraw_bonus_") || it.title.contains("Referral Withdraw Bonus")
        }.sumOf { it.coins.coerceAtLeast(0) }
    }
    val mySupportMessages = remember(supportMessages, myEmail, myUserId) {
        supportMessages.filter {
            (myEmail.isNotBlank() && it.userEmail.equals(myEmail, ignoreCase = true)) ||
                (myUserId.isNotBlank() && it.userId == myUserId)
        }.sortedBy { it.timestampMillis }
    }

    // Authentication form state
    var authTabIndex by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Sign Up
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }

    val isUserLoggedIn = currentUser != null && currentUser?.email != "guest@watchearn.com"

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0D1424),
                shadowElevation = 4.dp
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
                                        text = "ACCOUNT & VIP HUB",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(GoldAccent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                            .border(0.8.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isUserLoggedIn) "VIP" else "GUEST",
                                            color = GoldAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Text(
                                    text = "Affiliate Royalties • Security • 24/7 Desk",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // VIP Help Chat Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSupportChatDialog = true }
                                .testTag("me_top_support_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "Support Chat",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Help Desk",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        },
        containerColor = DarkMeBg,
        modifier = modifier.testTag("me_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdminPostsBannerSection(
                        posts = adminPosts,
                        currentTab = "ME",
                        dismissedIds = dismissedPostIds,
                        onDismissPost = { viewModel.dismissAdminPost(it) }
                    )
                }

                // Profile Header Section
                if (isUserLoggedIn) {
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("me_profile_header_card")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF1E293B),
                                                Color(0xFF0F172A),
                                                Color(0xFF090D16)
                                            )
                                        )
                                    )
                                    .border(
                                        1.2.dp,
                                        Brush.horizontalGradient(
                                            listOf(
                                                GoldAccent.copy(alpha = 0.6f),
                                                PrimaryBlue.copy(alpha = 0.4f),
                                                EmeraldAccent.copy(alpha = 0.5f)
                                            )
                                        ),
                                        RoundedCornerShape(26.dp)
                                    )
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // High-End Avatar with VIP Ring
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(GoldAccent, AmberDark)
                                                ),
                                                CircleShape
                                            )
                                            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            .shadow(6.dp, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = currentUser?.name?.ifBlank { "VIP Member" } ?: "Member",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(EmeraldAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .border(0.8.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "VERIFIED",
                                                    color = EmeraldAccent,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = currentUser?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // ID Copy Strip
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.07f),
                                            modifier = Modifier.clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("Kingo User ID", currentUser?.userId ?: ""))
                                                Toast.makeText(context, "User ID copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ID: ${currentUser?.userId}",
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy ID",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Logout Button
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.logout()
                                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = "Logout",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Logout",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Modern Executive Sign In / Sign Up Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("me_auth_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(GoldAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (authTabIndex == 0) "Sign In to Kingo Account" else "Create Kingo VIP Account",
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Sync coins, watch tasks & instant UPI payouts",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                TabRow(
                                    selectedTabIndex = authTabIndex,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                                ) {
                                    Tab(
                                        selected = authTabIndex == 0,
                                        onClick = { authTabIndex = 0; authError = null },
                                        text = { Text("Sign In", fontWeight = FontWeight.Bold, color = if (authTabIndex == 0) GoldAccent else Color.White.copy(alpha = 0.6f)) }
                                    )
                                    Tab(
                                        selected = authTabIndex == 1,
                                        onClick = { authTabIndex = 1; authError = null },
                                        text = { Text("Create Account", fontWeight = FontWeight.Bold, color = if (authTabIndex == 1) GoldAccent else Color.White.copy(alpha = 0.6f)) }
                                    )
                                }

                                if (authTabIndex == 1) {
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it; authError = null },
                                        label = { Text("Your Full Name", color = Color.White.copy(alpha = 0.7f)) },
                                        leadingIcon = { Icon(Icons.Default.Person, null, tint = GoldAccent) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = GoldAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it; authError = null },
                                    label = { Text("Email Address", color = Color.White.copy(alpha = 0.7f)) },
                                    leadingIcon = { Icon(Icons.Default.Email, null, tint = GoldAccent) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it; authError = null },
                                    label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = GoldAccent) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                                )

                                authError?.let { err ->
                                    Text(
                                        text = err,
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                                            authError = "Please enter a valid email."
                                            return@Button
                                        }
                                        if (passwordInput.length < 4) {
                                            authError = "Password must be at least 4 characters."
                                            return@Button
                                        }

                                        isAuthLoading = true
                                        if (authTabIndex == 0) {
                                            viewModel.login(emailInput.trim(), passwordInput) { success, msg ->
                                                isAuthLoading = false
                                                if (success) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    authError = null
                                                } else {
                                                    authError = msg
                                                }
                                            }
                                        } else {
                                            viewModel.signUp(emailInput.trim(), passwordInput, nameInput.trim()) { success, msg ->
                                                isAuthLoading = false
                                                if (success) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    authError = null
                                                } else {
                                                    authError = msg
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isAuthLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_submit_btn")
                                ) {
                                    if (isAuthLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                                    } else {
                                        Text(
                                            text = if (authTabIndex == 0) "Sign In with Email" else "Create Account & Sign In",
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3-Column Live Performance Metric Strip
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%,d", walletBalance),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Coins Balance",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$completedCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "Completed",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$referredFriendsCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Invited Friends",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Completely Recreated VIP Referral & Affiliate Network Hub (Institutional Quality)
                if (isUserLoggedIn) {
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("me_refer_and_earn_card")
                        ) {
                            Column(
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
                                        1.5.dp,
                                        Brush.horizontalGradient(
                                            listOf(
                                                GoldAccent.copy(alpha = 0.8f),
                                                IndigoAccent.copy(alpha = 0.5f),
                                                EmeraldAccent.copy(alpha = 0.7f)
                                            )
                                        ),
                                        RoundedCornerShape(26.dp)
                                    )
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header Row with Glowing Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(GoldAccent.copy(alpha = 0.18f), CircleShape)
                                                .border(1.2.dp, GoldAccent.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "VIP PARTNER PROGRAM",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = "10% Lifetime Royalty on All Friend Payouts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GoldAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(GoldAccent, Color(0xFFEA580C))
                                                ),
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "10% ROYALTY",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // 6-Digit Monospace Referral Code Vault Box
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                        .border(1.2.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "YOUR EXCLUSIVE REFERRAL CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Big Stylized Digits
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            viewModel.recordSharedReferralCode(myReferralCode)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("Kingo Referral Code", myReferralCode))
                                            Toast.makeText(context, "Referral code $myReferralCode copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = myReferralCode,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GoldAccent,
                                                letterSpacing = 6.sp,
                                                modifier = Modifier.testTag("me_referral_code_text")
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Action Buttons Row (Share & Copy Link)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.recordSharedReferralCode(myReferralCode)
                                                val cleanDownloadUrl = com.example.data.DataStoreManager.normalizeAppDownloadUrl(appDownloadUrl)
                                                val shareMsg = "👑 Join Kingo King & Earn Real UPI Cash by Watching Videos!\n\n" +
                                                        "🎁 Use my 6-digit Referral Code: $myReferralCode to get +100 Welcome Bonus Coins instantly!\n\n" +
                                                        "📲 Download Kingo King App:\n$cleanDownloadUrl\n\n" +
                                                        "🚀 Fast 60-Sec UPI Withdrawals • 100% Free & Verified!"

                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Invite Friends to Kingo King"))
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("me_share_referral_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Invite Friends",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val cleanDownloadUrl = com.example.data.DataStoreManager.normalizeAppDownloadUrl(appDownloadUrl)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("Kingo Download Link", cleanDownloadUrl))
                                                Toast.makeText(context, "App download link copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("me_open_app_download_link_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Link",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Copy App Link",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                // 2-Column Live Affiliate Stats
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$referredFriendsCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Partners Joined",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 10.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(30.dp)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "+$totalReferralBonusCoins Coins",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = EmeraldAccent
                                        )
                                        Text(
                                            text = "10% Royalty Credited",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // 3-Step Clean Visual Guide (Crystal Clear & Professional - Zero confusing placeholders)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "How the Royalty Program Works",
                                            fontWeight = FontWeight.Black,
                                            color = GoldAccent,
                                            fontSize = 13.sp
                                        )
                                    }

                                    // Step 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(GoldAccent.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("1", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Share Your Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Send your 6-digit code or link to friends and community.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    }

                                    // Step 2
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("2", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Friend Gets Instant +100 Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("When your friend enters your code during sign-up, they receive 100 Bonus Coins.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    }

                                    // Step 3
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(EmeraldAccent.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("3", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("10% Lifetime Cash Royalty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Every time your invited friend withdraws cash, 10% bonus coins are instantly added to your wallet for life!", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Account Hub & Shortcuts
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Account Hub & Quick Shortcuts",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )

                        // Wallet Shortcut Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(AppScreen.WALLET) }
                                .testTag("me_wallet_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(GoldAccent.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "My Wallet & Payout Passbook",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Manage balance, request UPI withdrawal & view records",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Tasks Shortcut Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.switchTab(AppScreen.TASKS) }
                                .testTag("me_tasks_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(EmeraldAccent.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Video Watch Tasks",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "$completedCount of ${availableTasks.size} tasks completed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Change Password Card
                        if (isUserLoggedIn) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showChangePasswordDialog = true }
                                    .testTag("me_change_password_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Security & Change Password",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Verify via 6-digit OTP to securely update password",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cloud Status & Version Information Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EmeraldAccent, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cloud Sync & Server",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "v2.5.0 • Live",
                                    color = EmeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = cloudStatus.ifBlank { "Live Synced with Google Drive Cloud" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSupportChatDialog) {
        SupportChatDialog(
            title = "VIP Support Chat",
            subtitle = "ID: ${currentUser?.userId ?: "Guest"} • 24/7 Priority Desk",
            messages = mySupportMessages,
            isAdminViewer = false,
            onSendMessage = { text ->
                viewModel.sendSupportMessage(text)
            },
            onDismiss = { showSupportChatDialog = false }
        )
    }

    if (showChangePasswordDialog && currentUser != null) {
        var otpCodeInput by remember { mutableStateOf("") }
        var generatedCode by remember { mutableStateOf<String?>(null) }
        var otpSent by remember { mutableStateOf(false) }
        var newPass by remember { mutableStateOf("") }
        var feedbackMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Account: ${currentUser?.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.sendEmailVerificationOtp(
                                email = currentUser?.email ?: "",
                                isPasswordReset = true
                            ) { ok, msg, code ->
                                feedbackMsg = msg
                                if (ok) {
                                    otpSent = true
                                    generatedCode = code
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (otpSent) "Resend 6-Digit OTP" else "Send Verification OTP")
                    }
                    generatedCode?.let { code ->
                        Text(
                            text = "Verified OTP: $code (Tap to Auto-Fill)",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { otpCodeInput = code }
                        )
                    }
                    if (otpSent) {
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { if (it.length <= 6) otpCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("6-Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password (min 4 chars)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    feedbackMsg?.let {
                        Text(it, fontSize = 12.sp, color = AmberDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPasswordWithOtp(
                            email = currentUser?.email ?: "",
                            enteredOtp = otpCodeInput,
                            newPassword = newPass
                        ) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) showChangePasswordDialog = false
                            else feedbackMsg = msg
                        }
                    },
                    enabled = otpSent && otpCodeInput.length == 6 && newPass.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Update Password", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
