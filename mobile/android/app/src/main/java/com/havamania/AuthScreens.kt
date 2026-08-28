package com.havamania

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = HavamaniaTheme.typography.screenTitle.copy(
                fontSize = 32.sp,
                letterSpacing = 1.sp
            ),
            color = themeColors.textPrimary
        )

        if (showSlogan) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_welcome_slogan),
                style = HavamaniaTheme.typography.bodyMedium,
                color = themeColors.textSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        } else if (title != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = themeColors.accent
            )
        }
    }
}

@Composable
fun AuthWelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val configuration = LocalConfiguration.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val openLegal = { url: String ->
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı: $url", Toast.LENGTH_SHORT).show()
        }
    }

    HavamaniaScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(themeStyles.pagePadding),
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
                verticalArrangement = Arrangement.spacedBy(themeStyles.spacingMD)
            ) {
                HavamaniaPrimaryButton(
                    text = stringResource(R.string.login_title),
                    onClick = onNavigateToLogin
                )

                HavamaniaSecondaryButton(
                    text = stringResource(R.string.register_title),
                    onClick = onNavigateToRegister
                )
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingXXL))

            // Legal Links
            Row(
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegalLink("KVKK") { openLegal(LegalUrls.KVKK) }
                LegalDivider()
                LegalLink("Gizlilik Politikası") { openLegal(LegalUrls.PRIVACY_POLICY) }
                LegalDivider()
                LegalLink("Kullanım Koşulları") { openLegal(LegalUrls.TERMS_OF_USE) }
            }

            Spacer(modifier = Modifier.height(themeStyles.spacingMD))
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
                AuthHeader(title = stringResource(R.string.login_title))
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
                            text = stringResource(R.string.forgot_password_link),
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
                        text = stringResource(R.string.login_title).uppercase(),
                        onClick = { viewModel.signIn(email, password) },
                        isLoading = authState is AuthState.Loading,
                        enabled = email.isNotEmpty() && password.isNotEmpty()
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.no_account_text), color = themeColors.textSecondary)
                    Text(
                        text = " " + stringResource(R.string.register_title),
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val openLegal = { url: String ->
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı: $url", Toast.LENGTH_SHORT).show()
        }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles

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
                AuthHeader(title = stringResource(R.string.new_account_title))
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClickLabel = stringResource(R.string.legal_consent_part1),
                                onClick = { termsAccepted = !termsAccepted }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.legal_consent_part1),
                                style = HavamaniaTheme.typography.bodySmall,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.legal_consent_part2),
                                style = HavamaniaTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = themeColors.accent,
                                modifier = Modifier.clickable(
                                    onClickLabel = stringResource(R.string.privacy_policy_title),
                                    onClick = { openLegal(LegalUrls.PRIVACY_POLICY) }
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val passwordsMatch = password == confirmPassword && password.isNotEmpty()

                    HavamaniaPrimaryButton(
                        text = stringResource(R.string.register_title).uppercase(),
                        onClick = { viewModel.signUp(name, email, password) },
                        isLoading = authState is AuthState.Loading,
                        enabled = name.isNotEmpty() && email.isNotEmpty() && passwordsMatch && termsAccepted
                    )

                    if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.passwords_do_not_match),
                            color = themeColors.error.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.has_account_text), color = themeColors.textSecondary)
                    Text(
                        text = " " + stringResource(R.string.login_title),
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
                AuthHeader(title = stringResource(R.string.forgot_password_title))
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
                            style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "E-posta adresinizi girin, size bir sıfırlama bağlantısı gönderelim.",
                            style = HavamaniaTheme.typography.bodySmall,
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
                            text = stringResource(R.string.send_link_btn),
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
                                text = stringResource(R.string.email_sent_title),
                                style = HavamaniaTheme.typography.screenTitle.copy(fontWeight = FontWeight.Black),
                                color = themeColors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.email_sent_desc),
                                style = HavamaniaTheme.typography.bodyMedium,
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
fun LegalLink(text: String, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = HavamaniaTheme.colors.textMuted.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
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
