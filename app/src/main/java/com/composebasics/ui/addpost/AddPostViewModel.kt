package com.composebasics.ui.addpost

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composebasics.data.repository.AuthRepository
import com.composebasics.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddPostUiState(
    val text: String = "",
    val imageUri: Uri? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

class AddPostViewModel(
    private val posts: PostRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddPostUiState())
    val state: StateFlow<AddPostUiState> = _state.asStateFlow()

    fun onTextChange(v: String) = _state.update { it.copy(text = v, error = null) }
    fun onImagePicked(uri: Uri?) = _state.update { it.copy(imageUri = uri, error = null) }
    fun removeImage() = _state.update { it.copy(imageUri = null) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        if (s.text.isBlank() && s.imageUri == null) {
            _state.update { it.copy(error = "Write something or add a photo") }
            return
        }
        val authorId = auth.currentUserId ?: run {
            _state.update { it.copy(error = "You are not logged in") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching { posts.addPost(authorId, s.text, s.imageUri) }
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Could not save post") } }
        }
    }
}
