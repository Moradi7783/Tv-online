package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Channel
import com.example.data.ChannelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChannelViewModel(private val repository: ChannelRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedPresetChannels()
        }
    }

    val channelsState: StateFlow<List<Channel>> = combine(
        repository.allChannels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        var filteredList = channels

        // Filter by category
        if (category != "All") {
            filteredList = if (category == "Favorites") {
                filteredList.filter { it.isFavorite }
            } else {
                filteredList.filter { it.category == category }
            }
        }

        // Filter by search query
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }
        }

        filteredList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Collect all unique categories to display as filter chips dynamically
    val categoriesState: StateFlow<List<String>> = repository.allChannels
        .map { list ->
            val cats = list.map { it.category }.distinct().toMutableList()
            cats.add(0, "All")
            cats.add("Favorites")
            cats
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("All", "Favorites")
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectChannel(channel: Channel?) {
        _activeChannel.value = channel
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.update(channel.copy(isFavorite = !channel.isFavorite))
        }
    }

    fun addChannel(name: String, url: String, category: String) {
        viewModelScope.launch {
            val formattedCategory = category.trim().ifEmpty { "Manual Streams 🔗" }
            repository.insert(
                Channel(
                    name = name.trim(),
                    url = url.trim(),
                    category = formattedCategory,
                    isPreset = false
                )
            )
        }
    }

    fun deleteChannel(channel: Channel) {
        viewModelScope.launch {
            repository.delete(channel)
            if (_activeChannel.value?.id == channel.id) {
                _activeChannel.value = null
            }
        }
    }
}

class ChannelViewModelFactory(private val repository: ChannelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChannelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
