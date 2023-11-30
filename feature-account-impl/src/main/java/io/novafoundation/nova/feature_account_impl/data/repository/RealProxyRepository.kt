package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxies
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

class RealProxyRepository(
    private val remoteSource: StorageDataSource
) : ProxyRepository {

    override suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, metaAccountIds: List<MetaAccountId>): List<ProxiedWithProxies> {
        val delegatorToProxies = receiveAllProxies(chainId)

        val accountIdToMetaAccounts = metaAccountIds.associateBy { it.accountId.intoKey() }

        return delegatorToProxies
            .mapNotNull { (delegator, proxies) ->
                val matchedProxies = matchProxiesToAccountAccountsAndMap(proxies, accountIdToMetaAccounts)

                if (matchedProxies.isEmpty()) return@mapNotNull null

                delegator to matchedProxies
            }.map { (delegator, proxies) ->
                mapToProxiedWithProxies(chainId, delegator, proxies)
            }
    }

    private suspend fun receiveAllProxies(chainId: ChainId): Map<AccountIdKey, Map<AccountIdKey, String>> {
        return remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .entries(
                    keyExtractor = { (accountId: AccountId) -> AccountIdKey(accountId) },
                    binding = { result, _ ->
                        bindProxyAccounts(result)
                    },
                    onDecodeException = { }
                )
        }
    }

    private fun bindProxyAccounts(dynamicInstance: Any?): Map<AccountIdKey, String> {
        val root = dynamicInstance.castToList()
        val proxies = root[0].castToList()

        return proxies.map {
            val proxy = it.castToStruct()
            val proxyAccountId: ByteArray = proxy.getTyped("delegate")
            val proxyType = proxy.get<Any?>("proxyType").castToDictEnum()
            proxyAccountId.intoKey() to proxyType.name
        }.toMap()
    }

    private fun mapToProxiedWithProxies(
        chainId: ChainId,
        delegator: AccountIdKey,
        proxies: List<ProxiedWithProxies.Proxy>
    ): ProxiedWithProxies {
        return ProxiedWithProxies(
            accountId = delegator.value,
            chainId = chainId,
            proxies = proxies
        )
    }

    private fun matchProxiesToAccountAccountsAndMap(
        proxies: Map<AccountIdKey, String>,
        accountIdToMetaAccounts: Map<AccountIdKey, MetaAccountId>
    ): List<ProxiedWithProxies.Proxy> {
        return proxies.mapNotNull { (proxyAccountId, proxyType) ->
            val matchedAccount = accountIdToMetaAccounts[proxyAccountId] ?: return@mapNotNull null

            ProxiedWithProxies.Proxy(
                accountId = proxyAccountId.value,
                metaId = matchedAccount.metaId,
                proxyType = proxyType
            )
        }
    }
}
