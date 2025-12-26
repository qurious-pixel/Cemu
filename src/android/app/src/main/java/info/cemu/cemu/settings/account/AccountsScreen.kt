@file:OptIn(ExperimentalMaterial3Api::class)

package info.cemu.cemu.settings.account

import android.view.KeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.R
import info.cemu.cemu.common.input.handleGamepadFocus
import info.cemu.cemu.common.string.parseHexOrNull
import info.cemu.cemu.common.ui.components.Header
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SelectField
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.SpinboxDatePickerDialog
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeAccount
import info.cemu.cemu.nativeinterface.NativeAccount.AccountGender
import info.cemu.cemu.nativeinterface.NativeAccount.MAX_ACCOUNT_COUNT
import info.cemu.cemu.nativeinterface.NativeAccount.MIN_ACCOUNT_COUNT
import info.cemu.cemu.nativeinterface.NativeSettings
import info.cemu.cemu.nativeinterface.NativeSettings.NetworkService
import java.text.DateFormat
import java.util.Date

private val Countries = NativeAccount.getAccountCountries().toList()
private val CountriesIndices = Countries.map { it.index }
private val CountriesMap = Countries.associate { it.index to it.name }

@Composable
fun AccountSettingsScreen(
    navigateBack: () -> Unit,
    accountsViewModel: AccountsViewModel = viewModel(),
) {
    val accounts by accountsViewModel.accounts.collectAsState()
    val activeAccountData by accountsViewModel.activeAccountData.collectAsState()
    val activeAccount = activeAccountData.account
    val onlineFullyValid =
        activeAccount.isValid && accountsViewModel.onlineFilesStatus.hasRequiredOnlineFiles
    val hasCustomNetworkConfiguration = remember { NativeSettings.hasCustomNetworkConfiguration() }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            accountsViewModel.saveActiveAccount()
        }
    }

    ScreenContent(
        appBarText = tr("Account settings"),
        navigateBack = navigateBack,
    ) {
        SingleSelection(
            label = tr("Active account"),
            choice = activeAccount,
            choices = accounts,
            choiceToString = { String.format("%s (%x)", it.miiName, it.persistentId) },
            onChoiceChanged = { accountsViewModel.setActiveAccount(it.persistentId) })

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = accounts.size < MAX_ACCOUNT_COUNT,
                onClick = { showCreateAccountDialog = true }) {
                Text(tr("Create"))
            }

            OutlinedButton(
                enabled = accounts.size > MIN_ACCOUNT_COUNT,
                onClick = { showDeleteAccountDialog = true }) {
                Text(tr("Delete"))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SingleSelection(
            label = tr("Network service") + " (${activeAccount.miiName})",
            enabled = onlineFullyValid,
            choice = if (onlineFullyValid) activeAccountData.networkService else NetworkService.OFFLINE,
            choiceToString = { networkServiceToString(it) },
            choices = listOf(
                NetworkService.OFFLINE,
                NetworkService.NINTENDO,
                NetworkService.PRETENDO,
                NetworkService.CUSTOM,
            ),
            isChoiceEnabled = { it != NetworkService.CUSTOM || hasCustomNetworkConfiguration },
            onChoiceChanged = accountsViewModel::setNetworkServiceForActiveAccount
        )

        OnlinePlayRequirements(
            account = activeAccount,
            onlineFilesStatus = accountsViewModel.onlineFilesStatus,
            onlineFullyValid = onlineFullyValid,
            onGetOnlineValidationErrors = accountsViewModel::getActiveAccountValidationErrors
        )

        AccountInformation(
            account = activeAccount,
            onDataChange = accountsViewModel::updateActiveAccount,
        )
    }

    if (showCreateAccountDialog) {
        CreateAccountDialog(
            onDismissRequest = { showCreateAccountDialog = false },
            onCreateAccount = {
                accountsViewModel.createAccount(it)
                showCreateAccountDialog = false
            },
            onValidateCreateAccount = accountsViewModel::validateCreateAccount
        )
    }

    if (showDeleteAccountDialog) {
        DeleteActiveAccountConfirmationDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            onConfirmation = {
                accountsViewModel.deleteActiveAccount()
                showDeleteAccountDialog = false
            },
            account = activeAccount,
        )
    }
}

@Composable
private fun AccountStatusIcon(isValid: Boolean) {
    val resId: Int
    val tint: Color

    if (isValid) {
        resId = R.drawable.ic_check_circle
        tint = MaterialTheme.colorScheme.primary
    } else {
        resId = R.drawable.ic_warning
        tint = MaterialTheme.colorScheme.error
    }

    Icon(
        painter = painterResource(resId),
        modifier = Modifier.padding(end = 8.dp),
        tint = tint,
        contentDescription = null
    )
}

@Composable
private fun OnlinePlayRequirements(
    account: NativeAccount.Account,
    onlineFilesStatus: OnlineFilesStatus,
    onlineFullyValid: Boolean,
    onGetOnlineValidationErrors: () -> Array<NativeAccount.OnlineValidationError>,
) {
    var onlineValidationErrors by remember {
        mutableStateOf<Array<NativeAccount.OnlineValidationError>?>(
            null
        )
    }

    Header(tr("Online play requirements"))

    Row(
        modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        AccountStatusIcon(onlineFullyValid)
        Text(getAccountStatus(account, onlineFilesStatus))
    }

    if (!onlineFullyValid) {
        Button(
            onClick = {
                onlineValidationErrors = onGetOnlineValidationErrors()
            }, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            Text(tr("Show online status"))
        }
    }

    OnlineTutorial()

    onlineValidationErrors?.let {
        OnlineErrorsDialog(
            validationErrors = it, onDismissRequest = { onlineValidationErrors = null })
    }
}

@Composable
private fun OnlineErrorsDialog(
    validationErrors: Array<NativeAccount.OnlineValidationError>,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(tr("Online status")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                validationErrors.forEachIndexed { index, error ->
                    Text(
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                        text = getErrorMessage(error)
                    )

                    if (index < validationErrors.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(tr("OK")) } },
    )
}

private fun getErrorMessage(error: NativeAccount.OnlineValidationError): String {
    return when (error) {
        is NativeAccount.AccountError -> getAccountErrorMessage(error.accountError)
        is NativeAccount.CorruptedOTP -> tr("otp.bin is invalid")
        is NativeAccount.CorruptedSEEPROM -> tr("seeprom.bin is invalid")
        is NativeAccount.MissingFile -> tr("Missing certificate and key files:") + "\n${error.file}"
        is NativeAccount.MissingOTP -> tr("otp.bin missing in Cemu directory")
        is NativeAccount.MissingSEEPROM -> tr("seeprom.bin missing in Cemu directory")
    }
}

private fun getAccountErrorMessage(error: Int): String = when (error) {
    NativeAccount.OnlineAccountError.NO_ACCOUNT_ID -> tr("AccountId missing (The account is not connected to a NNID/PNID)")
    NativeAccount.OnlineAccountError.NO_PASSWORD_CACHED -> tr("IsPasswordCacheEnabled is set to false (The remember password option on your Wii U must be enabled for this account before dumping it)")
    NativeAccount.OnlineAccountError.PASSWORD_CACHE_EMPTY -> tr("AccountPasswordCache is empty (The remember password option on your Wii U must be enabled for this account before dumping it)")
    NativeAccount.OnlineAccountError.NO_PRINCIPAL_ID -> tr("PrincipalId missing")
    else -> throw IllegalArgumentException("Unexpected account error $error")
}

private fun getAccountStatus(
    account: NativeAccount.Account,
    onlineFilesStatus: OnlineFilesStatus,
): String {
    if (onlineFilesStatus.hasRequiredOnlineFiles) {
        return if (account.isValid) tr("Selected account is a valid online account")
        else tr("Selected account is not linked to a NNID or PNID")
    }

    return when {
        onlineFilesStatus.isOTPPresent && onlineFilesStatus.isSEEPREOMPresent -> tr("OTP and SEEPROM present but no certificate files were found")
        onlineFilesStatus.isOTPPresent != onlineFilesStatus.isSEEPREOMPresent -> tr("OTP.bin or SEEPROM.bin is missing")
        else -> tr("Online play is not set up. Follow the guide below to get started")
    }
}

@Composable
private fun OnlineTutorial() {
    Text(
        modifier = Modifier.padding(8.dp), text = buildAnnotatedString {
            withLink(
                LinkAnnotation.Url(
                    stringResource(R.string.cemu_online_guide), TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                )
            ) {
                append(tr("Online play tutorial"))
            }
        })
}

@Composable
fun ClickToEditTextField(
    value: String,
    label: @Composable (() -> Unit),
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    singleLine: Boolean = true,
    onClick: (() -> Unit)? = null, 
    colors: androidx.compose.material3.TextFieldColors = androidx.compose.material3.TextFieldDefaults.colors()
) {
    val focusManager = LocalFocusManager.current
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        colors = colors, // Now correctly passed to the internal TextField
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (!state.isFocused) isEditing = false
            }
            .onKeyEvent { keyEvent ->
                val isCenterClick = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A
                
                if (isCenterClick && keyEvent.type == KeyEventType.KeyUp) {
                    if (onClick != null) {
                        onClick()
                    } else if (!isEditing) {
                        isEditing = true
                        focusRequester.requestFocus()
                    }
                    true 
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (onClick != null) {
                        onClick()
                    } else {
                        isEditing = true
                        focusRequester.requestFocus()
                    }
                }
            },
        readOnly = if (onClick != null) true else !isEditing,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = {
            isEditing = false
            focusManager.clearFocus()
        })
    )
}

@Composable
private fun AccountInformation(
    account: NativeAccount.Account,
    onDataChange: (NativeAccount.Account) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    val dateString = remember(account.birthday) {
        java.text.DateFormat.getDateInstance().format(java.util.Date(account.birthday))
    }
    
    Header(tr("Account information"))

    TextField(
        value = account.persistentId.toUInt().toString(16),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .handleGamepadFocus(focusManager),
        singleLine = true,
        onValueChange = {},
        readOnly = true,
        label = { Text(tr("PersistentId")) },
    )

    ClickToEditTextField(
        value = account.miiName,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .handleGamepadFocus(focusManager),
        singleLine = true,
        onValueChange = { onDataChange(account.copy(miiName = it)) },
        label = { Text(tr("Mii name")) },
    )

	ClickToEditTextField(
        value = dateString,
        onValueChange = {},
        onClick = { showDatePicker = true }, // Triggers dialog instead of keyboard
        label = { Text(tr("Birthday")) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .handleGamepadFocus(focusManager),
        colors = TextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline
        )
    )

    if (showDatePicker) {
        SpinboxDatePickerDialog(
            initialDateMillis = account.birthday,
            onDismissRequest = { showDatePicker = false },
            onConfirm = { selectedMillis ->
                onDataChange(account.copy(birthday = selectedMillis))
                showDatePicker = false
            }
        )
    }

    SelectField(
        label = tr("Gender"),
        choice = account.gender,
        choiceToString = { accountGenderToString(it) },
        choices = listOf(
            AccountGender.FEMALE,
            AccountGender.MALE,
        ),
        onChoiceChanged = { onDataChange(account.copy(gender = it)) }
    )

    ClickToEditTextField(
        value = account.email,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .handleGamepadFocus(focusManager),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        onValueChange = { onDataChange(account.copy(email = it)) },
        label = { Text(tr("Email")) },
    )

    SelectField(
        label = tr("Country"),
        choice = account.country,
        choiceToString = { CountriesMap[it] ?: it.toString() },
        choices = CountriesIndices,
        onChoiceChanged = { onDataChange(account.copy(country = it)) }
    )
}

@Composable
private fun DeleteActiveAccountConfirmationDialog(
    account: NativeAccount.Account,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(tr("Confirmation")) },
        text = {
            Text(
                tr(
                    "Are you sure you want to delete the account {0} with id {1}?",
                    account.miiName,
                    account.persistentId.toUInt().toString(16)
                )
            )
        },
        confirmButton = { TextButton(onClick = onConfirmation) { Text(tr("Yes")) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(tr("No")) } },
    )
}

@Composable
private fun CreateAccountDialog(
    onDismissRequest: () -> Unit,
    onValidateCreateAccount: (CreateAccount) -> CreateAccountError?,
    onCreateAccount: (CreateAccount) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var createAccount by remember {
        mutableStateOf(
            CreateAccount(
                persistentId = NativeAccount.MIN_PERSISTENT_ID.toInt(),
                miiName = "",
            )
        )
    }

    val createError by remember { derivedStateOf { onValidateCreateAccount(createAccount) } }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(tr("Create new account")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = createAccount.miiName,
                    modifier = Modifier.handleGamepadFocus(focusManager),
                    singleLine = true,
                    isError = createError != null,
                    supportingText = {
                        if (createError is CreateAccountError.EmptyMiiName) {
                            Text(tr("Account name may not be empty!"))
                        }
                    },
                    onValueChange = { createAccount = createAccount.copy(miiName = it) },
                    label = { Text(tr("Mii name")) },
                )
                TextField(
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    value = createAccount.persistentId?.toUInt()?.toString(16) ?: "",
                    modifier = Modifier.handleGamepadFocus(focusManager),
                    singleLine = true,
                    isError = createError != null,
                    supportingText = {
                        val errorMessage = when (val currentError = createError) {
                            is CreateAccountError.ConflictingPersistentId -> tr(
                                "The persistent id {0} is already in use by account {1}!",
                                currentError.existingPersistentId.toUInt().toString(16),
                                currentError.existingMiiName,
                            )

                            CreateAccountError.EmptyPersistentId -> tr("No persistent id entered!")
                            CreateAccountError.InvalidPersistentId -> tr(
                                "The persistent id must be greater than {0}!",
                                NativeAccount.MIN_PERSISTENT_ID.toString(16)
                            )

                            else -> return@TextField
                        }

                        Text(errorMessage)
                    },
                    onValueChange = {
                        if (it.isEmpty()) {
                            createAccount = createAccount.copy(persistentId = null)
                            return@TextField
                        }
                        val hexValue = it.parseHexOrNull() ?: return@TextField
                        createAccount = createAccount.copy(persistentId = hexValue.toInt())
                    },
                    label = { Text(tr("PersistentId")) },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = createError == null,
                onClick = { onCreateAccount(createAccount) },
            ) {
                Text(tr("OK"))
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(tr("Cancel")) } })
}

private fun accountGenderToString(gender: Byte) = when (gender) {
    AccountGender.FEMALE -> tr("Female")
    AccountGender.MALE -> tr("Male")
    else -> throw IllegalArgumentException("Invalid account gender: $gender")
}

private fun networkServiceToString(networkService: Int) = when (networkService) {
    NetworkService.OFFLINE -> tr("Offline")
    NetworkService.NINTENDO -> tr("Nintendo")
    NetworkService.PRETENDO -> tr("Pretendo")
    NetworkService.CUSTOM -> tr("Custom")
    else -> throw IllegalArgumentException("Invalid network service: $networkService")
}
