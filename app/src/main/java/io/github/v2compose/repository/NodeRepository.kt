package io.github.v2compose.repository

import androidx.paging.PagingData
import io.github.v2compose.network.bean.*
import kotlinx.coroutines.flow.Flow

interface NodeRepository {

    suspend fun getNodes(): NodesInfo
    suspend fun getNodesNavInfo(): NodesNavInfo

    suspend fun getNodeInfo(nodeId: String): NodeInfo
    fun getNodeTopicInfoFlow(nodeId: String): Flow<PagingData<Any>>

    suspend fun doNodeAction(nodeId: String, actionUrl: String): NodeTopicInfo

}