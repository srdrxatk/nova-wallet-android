package io.novafoundation.nova.feature_proxy_api.domain.validators

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class MaximumProxiesNotReachedValidation<P, E>(
    private val chain: (P) -> Chain,
    private val accountId: (P) -> AccountId,
    private val newProxiedQuantity: (P) -> Int,
    private val error: (P, Int) -> E,
    private val proxyRepository: GetProxyRepository
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val newProxiesQuantity = newProxiedQuantity(value)
        val maximumProxiesQuantiy = proxyRepository.maxProxiesQuantity(chain(value))

        return validOrError(newProxiesQuantity <= maximumProxiesQuantiy) {
            error(value, maximumProxiesQuantiy)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.maximumProxiesNotReached(
    chain: (P) -> Chain,
    accountId: (P) -> AccountId,
    newProxiedQuantity: (P) -> Int,
    error: (P, Int) -> E,
    proxyRepository: GetProxyRepository
) {
    validate(
        MaximumProxiesNotReachedValidation(
            chain = chain,
            accountId = accountId,
            newProxiedQuantity = newProxiedQuantity,
            error = error,
            proxyRepository = proxyRepository
        )
    )
}
