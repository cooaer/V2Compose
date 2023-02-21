package io.github.v2compose.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.v2compose.network.V2exService
import io.github.v2compose.network.bean.NotificationInfo
import kotlin.math.ceil

class NotificationPagingSource(
    private val v2exService: V2exService,
    private val appPreferences: AppPreferences
) :
    PagingSource<Int, NotificationInfo.Reply>() {

    companion object {
        const val FirstPageIndex = 1
        const val ItemCountOfPage = 50
    }

    override fun getRefreshKey(state: PagingState<Int, NotificationInfo.Reply>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationInfo.Reply> {
        val page = params.key ?: FirstPageIndex
        return try {
            val result = v2exService.notifications(page)
            appPreferences.updateAccount(unreadNotifications = result.unreadCount)

            val pageCount = ceil(result.total.toFloat() / ItemCountOfPage).toInt()
            val prevKey = if (page > FirstPageIndex) page - 1 else null
            val nextKey = if (page < pageCount) page + 1 else null
            LoadResult.Page(data = result.replies, prevKey = prevKey, nextKey = nextKey)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }

    }
}