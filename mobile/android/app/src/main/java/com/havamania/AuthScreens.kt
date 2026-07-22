package com.havamania

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*

@Composable
fun AuthHeader(
    title: String? = null,
    showSlogan: Boolean = false
) {
    val themeColors = HavamaniaTheme.colors
    val context = LocalContext.current
    val logoResId = context.resources.getIdentifier("havamania_logo_clean", "drawable", context.packageName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        if (logoResId != 0) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Havamania Logo",
                modifier = Modifier.size(140.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Havamania",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = themeColors.textPrimary
        )

        if (showSlogan) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hava durumunu akıllıca takip et,\nseyahatlerini akıllıca planla.",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        } else if (title != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = themeColors.accent
            )
        }
    }
}

@Composable
fun AuthWelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLegal: (String, String) -> Unit = { _, _ -> }
) {
    val themeColors = HavamaniaTheme.colors
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    HavamaniaScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.widthIn(max = 500.dp),
                contentAlignment = Alignment.Center
            ) {
                AuthHeader(showSlogan = true)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HavamaniaPrimaryButton(
                    text = "GİRİŞ YAP",
                    onClick = onNavigateToLogin
                )

                HavamaniaSecondaryButton(
                    text = "HESAP OLUŞTUR",
                    onClick = onNavigateToRegister
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Legal Links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegalLink("KVKK") { onNavigateToLegal("KVKK AYDINLATMA METNİ", LegalUrls.KVKK) }
                LegalDivider()
                LegalLink("Gizlilik Politikası") { onNavigateToLegal("GİZLİLİK POLİTİKASI", LegalUrls.PRIVACY_POLICY) }
                LegalDivider()
                LegalLink("Kullanım Koşulları") { onNavigateToLegal("KULLANIM KOŞULLARI", LegalUrls.TERMS_OF_USE) }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val themeColors = HavamaniaTheme.colors

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(title = "", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.widthIn(max = 500.dp)) {
                AuthHeader(title = "Hoş Geldiniz")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.widthIn(max = 450.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authState is AuthState.Error) {
                    HavamaniaErrorCard(
                        message = (authState as AuthState.Error).message,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                HavamaniaGlassCard {
                    HavamaniaAuthField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HavamaniaAuthPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Şifre",
                        imeAction = ImeAction.Done
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Şifremi Unuttum",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = themeColors.accent,
                            modifier = Modifier.clickable { onNavigateToForgotPassword() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HavamaniaPrimaryButton(
                        text = "GİRİŞ YAP",
                        onClick = { viewModel.signIn(email, password) },
                        isLoading = authState is AuthState.Loading,
                        enabled = email.isNotEmpty() && password.isNotEmpty()
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Henüz hesabın yok mu?", color = themeColors.textSecondary)
                    Text(
                        text = " Kayıt Ol",
                        color = themeColors.accent,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val themeColors = HavamaniaTheme.colors

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(title = "", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.widthIn(max = 500.dp)) {
                AuthHeader(title = "Yeni Hesap Oluştur")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.widthIn(max = 450.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authState is AuthState.Error) {
                    HavamaniaErrorCard(
                        message = (authState as AuthState.Error).message,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                HavamaniaGlassCard {
                    HavamaniaAuthField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Ad Soyad",
                        leadingIcon = Icons.Rounded.Person,
                        capitalization = KeyboardCapitalization.Words
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HavamaniaAuthField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HavamaniaAuthPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Şifre"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HavamaniaAuthPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Şifre Tekrar"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val passwordsMatch = password == confirmPassword && password.isNotEmpty()

                    HavamaniaPrimaryButton(
                        text = "KAYIT OL",
                        onClick = { viewModel.signUp(name, email, password) },
                        isLoading = authState is AuthState.Loading,
                        enabled = name.isNotEmpty() && email.isNotEmpty() && passwordsMatch
                    )

                    if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                        Text(
                            text = "Şifreler uyuşmuyor",
                            color = themeColors.error.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zaten hesabın var mı?", color = themeColors.textSecondary)
                    Text(
                        text = " Giriş Yap",
                        color = themeColors.accent,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val themeColors = HavamaniaTheme.colors
    var successSent by remember { mutableStateOf(false) }

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(title = "", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.widthIn(max = 500.dp)) {
                AuthHeader(title = "Şifre Sıfırlama")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.widthIn(max = 450.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authState is AuthState.Error) {
                    HavamaniaErrorCard(
                        message = (authState as AuthState.Error).message,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                HavamaniaGlassCard {
                    if (!successSent) {
                        Text(
                            text = "Şifrenizi mi Unuttunuz?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "E-posta adresinizi girin, size bir sıfırlama bağlantısı gönderelim.",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        HavamaniaAuthField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardType = KeyboardType.Email
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        HavamaniaPrimaryButton(
                            text = "BAĞLANTI GÖNDER",
                            onClick = {
                                viewModel.resetPassword(email)
                                successSent = true
                            },
                            isLoading = authState is AuthState.Loading,
                            enabled = email.isNotEmpty()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.MarkEmailRead,
                                null,
                                tint = themeColors.accent,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "E-posta Gönderildi",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = themeColors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lütfen e-posta kutunuzu kontrol edin ve talimatları izleyin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            HavamaniaPrimaryButton(
                                text = "GERİ DÖN",
                                onClick = onBack
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Internal Helper Components ---

@Composable
fun HavamaniaAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    val colors = HavamaniaTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(if (isFocused) 0.8f else 0.1f)
    val containerAlpha by animateFloatAsState(if (isFocused) 0.6f else 0.4f)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                colors.accent.copy(alpha = borderAlpha),
                RoundedCornerShape(20.dp)
            ),
        placeholder = { Text(placeholder, color = colors.textMuted.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(leadingIcon, null, tint = if (isFocused) colors.accent else colors.textMuted, modifier = Modifier.size(20.dp)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceGlass.copy(alpha = containerAlpha),
            unfocusedContainerColor = colors.surfaceGlass.copy(alpha = containerAlpha),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.accent,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        singleLine = true
    )
}

@Composable
fun HavamaniaAuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Next
) {
    val colors = HavamaniaTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(if (isFocused) 0.8f else 0.1f)
    val containerAlpha by animateFloatAsState(if (isFocused) 0.6f else 0.4f)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                colors.accent.copy(alpha = borderAlpha),
                RoundedCornerShape(20.dp)
            ),
        placeholder = { Text(placeholder, color = colors.textMuted.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = if (isFocused) colors.accent else colors.textMuted, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(image, null, tint = colors.textMuted.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceGlass.copy(alpha = containerAlpha),
            unfocusedContainerColor = colors.surfaceGlass.copy(alpha = containerAlpha),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.accent,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        singleLine = true
    )
}

@Composable
fun HavamaniaSecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    val colors = HavamaniaTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceGlass.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textPrimary
                )
            )
        }
    }
}

@Composable
fun LegalLink(text: String, onClick: () -> Unit = {}) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = HavamaniaTheme.colors.textMuted.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun LegalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(HavamaniaTheme.colors.textMuted.copy(alpha = 0.3f))
    )
}
