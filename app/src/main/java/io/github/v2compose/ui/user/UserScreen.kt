package io.github.v2compose.ui.user

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import io.github.v2compose.Constants
import io.github.v2compose.R
import io.github.v2compose.core.share
import io.github.v2compose.network.bean.UserReplies
import io.github.v2compose.network.bean.UserTopics
import io.github.v2compose.ui.HandleSnackbarMessage
import io.github.v2compose.ui.common.*
import io.github.v2compose.ui.user.composables.UserToolbar
import io.github.v2compose.V2exUri
import kotlinx.coroutines.launch
import me.onebone.toolbar.*

@Composable
fun UserScreenRoute(
    onBackClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit,
    openUri: (String) -> Unit,
    viewModel: UserViewModel = hiltViewModel(),
    screenState: UserScreenState = rememberUserScreenState(),
) {
    val context = LocalContext.current

    val userArgs = viewModel.userArgs
    val topicTitleOverview by viewModel.topicTitleOverview.collectAsStateWithLifecycle()
    val userUiState by viewModel.userUiState.collectAsStateWithLifecycle()
    val userTopics = viewModel.userTopics.collectAsLazyPagingItems()
    val userReplies = viewModel.userReplies.collectAsLazyPagingItems()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    HandleSnackbarMessage(viewModel, screenState)

    UserScreen(
        userUiState = userUiState,
        userTopics = userTopics,
        userReplies = userReplies,
        topicTitleOverview = topicTitleOverview,
        isLoggedIn = isLoggedIn,
        snackbarHostState = screenState.snackbarHostState,
        onBackClick = onBackClick,
        onShareClick = {
            context.share(userArgs.userName, V2exUri.userUrl(userArgs.userName))
        },
        onRetryClick = { viewModel.retry() },
        onFollowClick = viewModel::followUser,
        onBlockClick = viewModel::blockUser,
        onTopicClick = onTopicClick,
        onNodeClick = onNodeClick,
        openUri = openUri,
    )
}

@Composable
private fun UserScreen(
    userUiState: UserUiState,
    userTopics: LazyPagingItems<UserTopics.Item>,
    userReplies: LazyPagingItems<UserReplies.Item>,
    topicTitleOverview: Boolean,
    isLoggedIn : Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit,
    onFollowClick: () -> Unit,
    onBlockClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit,
    openUri: (String) -> Unit,
) {
    val scaffoldState = rememberCollapsingToolbarScaffoldState()

    Surface(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Box {
            CollapsingToolbarScaffold(modifier = Modifier.fillMaxSize(),
                state = scaffoldState,
                scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
                enabled = true,
                toolbar = {
                    UserToolbar(
                        userUiState = userUiState,
                        isLoggedIn = isLoggedIn,
                        scaffoldState = scaffoldState,
                        onBackClick = onBackClick,
                        onShareClick = onShareClick,
                        onFollowClick = onFollowClick,
                        onBlockClick = onBlockClick,
                    )
                }) {
                UserContent(
                    userUiState = userUiState,
                    userTopics = userTopics,
                    userReplies = userReplies,
                    topicTitleOverview = topicTitleOverview,
                    onRetryClick = onRetryClick,
                    onTopicClick = onTopicClick,
                    onNodeClick = onNodeClick,
                    openUri = openUri
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

}


@Composable
private fun UserContent(
    userUiState: UserUiState,
    userTopics: LazyPagingItems<UserTopics.Item>,
    userReplies: LazyPagingItems<UserReplies.Item>,
    topicTitleOverview: Boolean,
    onRetryClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit,
    openUri: (String) -> Unit,
) {
    when (userUiState) {
        is UserUiState.Success -> {
            UserPager(
                userTopics = userTopics,
                userReplies = userReplies,
                topicTitleOverview = topicTitleOverview,
                onTopicClick = onTopicClick,
                onNodeClick = onNodeClick,
                openUri = openUri,
            )
        }
        is UserUiState.Loading -> {
            Loading()
        }
        is UserUiState.Error -> {
            LoadError(error = userUiState.error, onRetryClick = onRetryClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserPager(
    userTopics: LazyPagingItems<UserTopics.Item>,
    userReplies: LazyPagingItems<UserReplies.Item>,
    topicTitleOverview: Boolean,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit,
    openUri: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    val tabNames = listOf(stringResource(R.string.user_topic), stringResource(R.string.user_reply))

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions: List<TabPosition> ->
                UserTabIndicator(tabPosition = tabPositions[pagerState.currentPage])
            }) {
            tabNames.forEachIndexed { index, name ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected, onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page = index)
                        }
                    }, modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        name,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    )
                }
            }
        }

        HorizontalPager(pageCount = 2, state = pagerState) {
            when (it) {
                0 -> UserTopicsList(
                    items = userTopics,
                    topicTitleOverview = topicTitleOverview,
                    onTopicClick = onTopicClick,
                    onNodeClick = onNodeClick
                )
                1 -> UserRepliesList(
                    items = userReplies, onTopicClick = onTopicClick, openUri = openUri
                )
            }
        }
    }
}

@Composable
private fun UserTopicsList(
    items: LazyPagingItems<UserTopics.Item>,
    topicTitleOverview: Boolean,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        pagingRefreshItem(lazyPagingItems = items)

        itemsIndexed(items = items, key = { index, item -> item.link }) { index, item ->
            if (item == null) return@itemsIndexed
            UserTopicItem(
                topic = item,
                topicTitleOverview = topicTitleOverview,
                onTopicClick = onTopicClick,
                onNodeClick = onNodeClick
            )
        }

        pagingAppendMoreItem(lazyPagingItems = items)
    }
}

@Composable
fun UserTopicItem(
    topic: UserTopics.Item,
    topicTitleOverview: Boolean,
    onTopicClick: (String) -> Unit,
    onNodeClick: (String, String) -> Unit,
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable { onTopicClick(topic.link) }) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(
                    topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = if (topicTitleOverview) Constants.topicTitleOverviewMaxLines else Integer.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                NodeTag(
                    nodeName = topic.nodeName, nodeId = topic.nodeLink, onItemClick = onNodeClick
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                topic.lastReply,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = ContentAlpha.medium),
            )
        }
        ListDivider(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun UserRepliesList(
    items: LazyPagingItems<UserReplies.Item>,
    onTopicClick: (String) -> Unit,
    openUri: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "refresh", contentType = "loadState") {
            PagingLoadState(state = items.loadState.refresh, onRetryClick = { items.retry() })
        }

        itemsIndexed(
            items = items,
            key = { index, item -> item.dock.link + item.content.content }) { index, item ->
            if (item == null) return@itemsIndexed
            UserReplyItem(reply = item, onTopicClick = onTopicClick, openUri = openUri)
        }

        item(key = "append", contentType = "loadState") {
            PagingLoadState(state = items.loadState.append, onRetryClick = { items.retry() })
        }
    }
}

@Composable
fun UserReplyItem(
    reply: UserReplies.Item, onTopicClick: (String) -> Unit, openUri: (String) -> Unit
) {
    val contentColor = LocalContentColor.current
    Box(modifier = Modifier
        .fillMaxSize()
        .clickable { onTopicClick(reply.dock.link) }) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                reply.dock.title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = ContentAlpha.medium),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                reply.dock.time,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = ContentAlpha.disabled),
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(modifier = Modifier.height(8.dp))

            val backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            val leftBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            HtmlContent(content = reply.content.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(color = backgroundColor)
                        drawRect(
                            color = leftBorderColor, size = size.copy(width = 4.dp.toPx())
                        )
                    }
                    .padding(start = 8.dp),
                onUriClick = openUri)
        }
        ListDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun UserTabIndicator(tabPosition: TabPosition, modifier: Modifier = Modifier) {
    val tabWidth = 32.dp
    val leftSpace = (tabPosition.width - tabWidth) / 2
    val currentTabWidth by animateDpAsState(
        targetValue = tabWidth,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )
    val indicatorOffset by animateDpAsState(
        targetValue = tabPosition.left + leftSpace,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .width(currentTabWidth)
            .height(2.dp)
            .background(color = MaterialTheme.colorScheme.primary)
    )
}