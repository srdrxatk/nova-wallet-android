package jp.co.soramitsu.feature_wallet_impl.domain

import jp.co.soramitsu.feature_wallet_api.domain.TokenUseCase
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.TokenRepository
import jp.co.soramitsu.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.runtime.state.chainAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class TokenUseCaseImpl(
    private val tokenRepository: TokenRepository,
    private val sharedState: SingleAssetSharedState
) : TokenUseCase {

    override suspend fun currentToken(): Token {
        val chainAsset = sharedState.chainAsset()

        return tokenRepository.getToken(chainAsset)
    }

    override fun currentTokenFlow(): Flow<Token> {
        return sharedState.selectedAsset.flatMapLatest { (_, chainAsset) ->
            tokenRepository.observeToken(chainAsset)
        }
    }
}
