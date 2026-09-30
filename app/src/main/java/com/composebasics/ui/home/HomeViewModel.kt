package com.composebasics.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composebasics.data.local.PostWithAuthor
import com.composebasics.data.repository.AuthRepository
import com.composebasics.data.repository.PostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(val posts: List<PostWithAuthor> = emptyList(), val isLoading: Boolean = true)

class HomeViewModel(
    postRepository: PostRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = postRepository.observePosts()
        .map { HomeUiState(posts = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun logout() = auth.logout()
}
