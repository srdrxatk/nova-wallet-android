package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal
import java.math.BigInteger

data class AmountModel(
    val token: String,
    val fiat: String
)

fun mapAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset
): AmountModel = mapAmountToAmountModel(
    amount = asset.token.amountFromPlanks(amountInPlanks),
    asset = asset
)

fun mapAmountToAmountModel(
    amount: BigDecimal,
    token: Token,
): AmountModel {
    val fiatAmount = token.fiatAmount(amount)

    return AmountModel(
        token = amount.formatTokenAmount(token.configuration),
        fiat = fiatAmount.formatAsCurrency()
    )
}

fun mapAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
): AmountModel = mapAmountToAmountModel(amount, asset.token)
