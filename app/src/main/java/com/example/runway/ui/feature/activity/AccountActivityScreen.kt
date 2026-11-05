package com.example.runway.ui.feature.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.usecase.AccountActivity
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.ErrorState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.MoneyKind
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.feature.home.nothingYetLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import kotlin.math.absoluteValue

@Composable
fun AccountActivityRoute(onBack: () -> Unit, viewModel: AccountActivityViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasReadSms()) }
    val activity = LocalActivity.current
    var blocked by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        // After a second refusal Android stops asking, so only settings can change it
        blocked = !it && activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) == false
    }
    // The answer may change in system settings while the user is away
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = context.hasReadSms() }

    Column(Modifier.fillMaxSize().background(RunwayTheme.colors.bg)) {
        SmallTopBar(stringResource(R.string.account_activity_title), onBack)
        if (granted) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val month by viewModel.month.collectAsStateWithLifecycle()
            MonthSwitcher(month, viewModel.canGoNext(month), viewModel::onPreviousMonth, viewModel::onNextMonth)
            Box(Modifier.weight(1f)) { AccountActivityBody(state, viewModel::retry) }
        } else {
            SmsPermissionPrompt(
                blocked = blocked,
                onAllow = {
                    if (blocked) context.openAppSettings() else request.launch(Manifest.permission.READ_SMS)
                },
            )
        }
    }
}

private fun Context.hasReadSms(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

@Composable
private fun SmsPermissionPrompt(blocked: Boolean, onAllow: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            lockup = nothingYetLockup(),
            title = stringResource(R.string.account_activity_permission_title),
            body = stringResource(R.string.account_activity_permission_body),
            actionLabel = stringResource(
                if (blocked) R.string.account_activity_open_settings else R.string.account_activity_allow,
            ),
            onAction = onAllow,
        )
    }
}

@Composable
private fun AccountActivityBody(state: UiState<List<AccountActivity>>, onRetry: () -> Unit) {
    when (state) {
        UiState.Loading -> LoadingState()
        UiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                lockup = nothingYetLockup(),
                title = stringResource(R.string.account_activity_empty_title),
                body = stringResource(R.string.account_activity_empty_body),
            )
        }
        is UiState.Error -> ErrorState(state.error, stringResource(R.string.action_retry), onRetry)
        is UiState.Success -> LazyColumn(
            contentPadding = PaddingValues(start = Dimens.screenMargin, end = Dimens.screenMargin, bottom = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            items(state.data, key = { it.accountSuffix }) { AccountCard(it) }
            item(key = "note") {
                Text(
                    text = stringResource(R.string.account_activity_note),
                    style = RunwayType.caption,
                    color = RunwayTheme.colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun AccountCard(account: AccountActivity) {
    val c = RunwayTheme.colors
    RunwayCard {
        CardTitle(stringResource(R.string.account_activity_account, account.accountSuffix))
        AmountRow(stringResource(R.string.account_activity_in)) { MoneyText(account.inMinor, kind = MoneyKind.Income) }
        AmountRow(stringResource(R.string.account_activity_out)) { MoneyText(account.outMinor, kind = MoneyKind.Expense) }
        AmountRow(stringResource(R.string.account_activity_net)) {
            MoneyText(
                account.netMinor.absoluteValue,
                kind = if (account.netMinor < 0) MoneyKind.Expense else MoneyKind.Income,
            )
        }
        Text(
            text = pluralStringResource(R.plurals.account_activity_count, account.count, account.count) + " · " +
                pluralStringResource(R.plurals.account_activity_days, account.activeDays, account.activeDays),
            style = RunwayType.bodySm,
            color = c.ink,
        )
        Text(
            text = MovementChannel.entries
                .mapNotNull { channel -> account.countByChannel[channel]?.let { channel to it } }
                .map { (channel, count) -> "${stringResource(channel.labelRes())} $count" }
                .joinToString(" · "),
            style = RunwayType.caption,
            color = c.inkMuted,
        )
    }
}

@Composable
private fun AmountRow(label: String, amount: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RunwayType.body, color = RunwayTheme.colors.inkMuted)
        amount()
    }
}

private fun MovementChannel.labelRes(): Int = when (this) {
    MovementChannel.CARD -> R.string.account_activity_channel_card
    MovementChannel.ATM -> R.string.account_activity_channel_atm
    MovementChannel.ONLINE -> R.string.account_activity_channel_online
    MovementChannel.OTHER -> R.string.account_activity_channel_other
}

@Preview(showBackground = true)
@Composable
private fun AccountCardPreview() {
    RunwayTheme {
        AccountCard(
            AccountActivity(
                "1234", 8_000_000, 5_250_050, 14, 9,
                mapOf(MovementChannel.CARD to 8, MovementChannel.ATM to 2, MovementChannel.OTHER to 4),
            ),
        )
    }
}
