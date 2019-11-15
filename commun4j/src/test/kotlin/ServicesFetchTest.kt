import io.golos.commun4j.model.FeedTimeFrame
import io.golos.commun4j.model.FeedType
import io.golos.commun4j.services.model.ReportRequestContentType
import io.golos.commun4j.services.model.ReportsRequestStatus
import io.golos.commun4j.services.model.ReportsRequestTimeSort
import io.golos.commun4j.services.model.TransactionDirection
import io.golos.commun4j.sharedmodel.CyberName
import io.golos.commun4j.sharedmodel.Either
import io.golos.commun4j.utils.StringSigner
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ServicesFetchTest {
    private val client = getClient(CONFIG_TYPE.DEV, false, false)

    @Test
    fun testAuth() {
        val clintWithUser = getClient(CONFIG_TYPE.DEV, true, false)
        val usr = clintWithUser.keyStorage.getActiveAccount()
        val key = clintWithUser.keyStorage.getActiveAccountKeys().first().second
        val profile = client.getUserProfile(usr, null).getOrThrow()

        val secret = client.getAuthSecret().getOrThrow().secret
        client.authWithSecret(
                profile.username!!,
                secret,
                StringSigner.signString(secret, key)
        ).getOrThrow()
    }

    @Test
    fun getCommunitiesTest() {
        val getCommunitiesResult = client.getCommunitiesList(0, 20)
        assertTrue(getCommunitiesResult is Either.Success)

        client.getCommunitiesList(null, null).getOrThrow()
        client.getCommunitiesList(null, 1).getOrThrow()
        client.getCommunitiesList(1, null).getOrThrow()


        val community = (getCommunitiesResult as Either.Success).value.items[0]

        val getComnityResult = client.getCommunity(community.communityId)

        assertTrue(getComnityResult is Either.Success)
    }

    @Test
    fun userMetadataFetchTest() {
        val clintWithUser = getClient(CONFIG_TYPE.DEV, true, false)
        val usr = clintWithUser.keyStorage.getActiveAccount()
        val key = clintWithUser.keyStorage.getActiveAccountKeys().first().second

        val profile = client.getUserProfile(usr, null).getOrThrow()

        val response = client
                .getUserProfile(usr, null)
        assertTrue(response is Either.Success)
        println(response)

        val secret = client.getAuthSecret().getOrThrow().secret
        client.authWithSecret(profile.username!!, secret, StringSigner.signString(secret, key)).getOrThrow()

        val posts = client.getPosts(type = FeedType.TOP_LIKES, timeframe = FeedTimeFrame.MONTH, limit = 10).getOrThrow()
        posts.items.forEach {
            client.getUserProfile(it.author.userId, null)
        }
    }



    @Test
    fun getPost() {
        val posts = client.getPosts(type = FeedType.TOP_LIKES,
                allowNsfw = true, timeframe = FeedTimeFrame.MONTH, limit = 1) as Either.Success
        val firstItem = posts.value.items.first()

        val getPostResp = client.getPost(firstItem.author.userId, firstItem.community.communityId, firstItem.contentId.permlink)
        assertTrue(getPostResp is Either.Success)

        val postsRaw = client.getPostsRaw(type = FeedType.TOP_LIKES,
                allowNsfw = true, timeframe = FeedTimeFrame.MONTH, limit = 100) as Either.Success

        val firstItemRaw = postsRaw.value.items.first()
        println(firstItemRaw.document)
        val getPostRespRaw = client.getPostRaw(firstItemRaw.author.userId, firstItemRaw.community.communityId, firstItemRaw.contentId.permlink)
        assertTrue(getPostRespRaw is Either.Success)
    }

    @Test
    fun walletTest() {
        val getWalletResult = client.getBalance(CyberName("tst5vmhjuxie"))
        assertTrue(getWalletResult is Either.Success)
        println(getWalletResult)

        val tranferHistoryResponse = client.getTransferHistory(CyberName("tst5vmhjuxie"),
                TransactionDirection.ALL)
        assertTrue(tranferHistoryResponse is Either.Success)
        println(tranferHistoryResponse)

        val getTokens = client.getTokensInfo(getWalletResult.getOrThrow().balances.map { it.symbol })
        assertTrue(getTokens is Either.Success)
        println(getTokens)
    }

    @Test
    fun leadersTest() {
        val getCommunitiesResult = client.getCommunitiesList(0, 20).getOrThrow()
        getCommunitiesResult.items.forEach {
            client.getLeaders(it.communityId, getRandomNullableInt(), null).getOrThrow()
        }
    }

//    @Test
//    fun getCommunityBlacklistTest() {
//        val getCommunitiesResult = client.getCommunitiesList(0, 25).getOrThrow()
//        getCommunitiesResult.items.forEach {
//            client.getCommunityBlacklist(it.communityId, it.alias, null, getRandomNullableInt()).getOrThrow()
//        }
//    }

    @Test
    fun blacklistTest() {
        val posts = client.getPosts(type = FeedType.TOP_LIKES, timeframe = FeedTimeFrame.MONTH, limit = 20).getOrThrow()
        posts.items.forEach {
            client.getBlacklistedCommunities(it.author.userId).getOrThrow()
            client.getBlacklistedUsers(it.author.userId).getOrThrow()
        }
    }

    @Test
    fun subscribersTest() {
        val posts = client.getPosts(type = FeedType.TOP_LIKES, timeframe = FeedTimeFrame.MONTH, limit = 20).getOrThrow()
        posts.items.forEach {
            client.getSubscribers(it.author.userId, null, getRandomNullableInt(), null).getOrThrow()
            client.getSubscribers(null, it.contentId.communityId, getRandomNullableInt(), null).getOrThrow()
        }
    }

    @Test
    fun subscriptionsTest() {
        val posts = client.getPosts(type = FeedType.TOP_LIKES, timeframe = FeedTimeFrame.MONTH, limit = 20).getOrThrow()
        posts.items.forEach {
            client.getUserSubscriptions(it.author.userId, null, getRandomNullableInt()).getOrThrow()
            client.getCommunitySubscriptions(it.author.userId, getRandomNullableInt(), getRandomNullableInt()).getOrThrow()
        }
    }

    @Test
    fun getReportsTest() {
        val communitites = client.getCommunitiesList(null, limit = 100).getOrThrow().items.map { it.communityId }
        client.getReports(communitites, null, ReportRequestContentType.POST, null, 5, 0).getOrThrow()
        client.getReports(communitites, ReportsRequestStatus.OPEN, ReportRequestContentType.COMMENT, null, 5, 0).getOrThrow()
        client.getReports(communitites, ReportsRequestStatus.CLOSED, ReportRequestContentType.POST, null, 5, 0).getOrThrow()
        client.getReports(communitites, null, ReportRequestContentType.COMMENT, ReportsRequestTimeSort.REPORTS_COUNT, 5, 0).getOrThrow()
        client.getReports(communitites, null, ReportRequestContentType.POST, ReportsRequestTimeSort.TIME, 5, 0).getOrThrow()
        client.getReports(communitites, null, ReportRequestContentType.COMMENT, ReportsRequestTimeSort.TIME_DESC, 5, 0).getOrThrow()
    }

    @Test
    fun suggestNames() {
        val name = client.getPosts(type = FeedType.TOP_LIKES, timeframe = FeedTimeFrame.MONTH, limit = 20).getOrThrow().items
                .map { it.author.username?.substring(0..3) ?: "unknown" }
        name.forEach {
            client.suggestNames(it).getOrThrow()
        }
    }

    private fun getRandomNullableInt() = if (Random.nextDouble() > 0.5) (100 * Random.nextDouble()).toInt().let {
        if (it == 0)1 else it
    } else null
}