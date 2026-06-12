package com.example.data

import kotlinx.coroutines.flow.Flow

class ChannelRepository(private val channelDao: ChannelDao) {
    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels()

    fun getChannelsByCategory(category: String): Flow<List<Channel>> {
        return channelDao.getChannelsByCategory(category)
    }

    suspend fun insert(channel: Channel) {
        channelDao.insertChannel(channel)
    }

    suspend fun update(channel: Channel) {
        channelDao.updateChannel(channel)
    }

    suspend fun delete(channel: Channel) {
        channelDao.deleteChannel(channel)
    }

    suspend fun deleteById(id: Int) {
        channelDao.deleteChannelById(id)
    }

    suspend fun seedPresetChannels() {
        if (channelDao.getChannelCount() == 0) {
            val presets = listOf(
                Channel(
                    name = "France 24 Persian (فرانس ۲۴ فارسی)",
                    url = "https://static.france24.com/live/F24_FA_LO_HLS/live_tv.m3u8",
                    category = "Persian 🇮🇷 & News",
                    isPreset = true
                ),
                Channel(
                    name = "CGTN Persian Live (سی جی تی ان فارسی)",
                    url = "https://cgtn-fa.hd.cgtn.com/fa/fa_live/index.m3u8",
                    category = "Persian 🇮🇷 & News",
                    isPreset = true
                ),
                Channel(
                    name = "TRT Spor (پخش زنده مسابقات فوتبال و ورزشی)",
                    url = "https://trtspor.live.trt.com.tr/hls/trtspor_live.m3u8",
                    category = "Sports ⚽ & World Cup",
                    isPreset = true
                ),
                Channel(
                    name = "Red Bull TV Live (ورزش های هیجانی ردبول)",
                    url = "https://rbmn-live.akamaized.net/hls/live/590964/bo-bello/master.m3u8",
                    category = "Sports ⚽ & World Cup",
                    isPreset = true
                ),
                Channel(
                    name = "Sky News Live (اسکای نیوز انگلیسی)",
                    url = "https://skynews-live.akamaized.net/hls/live/2007802/skynewshd/master.m3u8",
                    category = "News 📰",
                    isPreset = true
                ),
                Channel(
                    name = "Al Jazeera English (الجزیره ورلد)",
                    url = "https://live-amg-01.live.stream.aljazeera.com/aljazeera/edge/aljazeera_en_high.m3u8",
                    category = "News 📰",
                    isPreset = true
                ),
                Channel(
                    name = "TRT World HD (تی آر تی ورلد)",
                    url = "https://trtworld.live.trt.com.tr/hls/trtworld_live.m3u8",
                    category = "News 📰",
                    isPreset = true
                ),
                Channel(
                    name = "DW News Live (دویچه وله انگلیسی)",
                    url = "https://dwamdstream-lh.akamaihd.net/i/dwamd_en@328114/master.m3u8",
                    category = "News 📰",
                    isPreset = true
                ),
                Channel(
                    name = "NASA TV Live (شبکه زنده فضایی ناسا)",
                    url = "https://nasa-otv.akamaized.net/hls/live/2034177/NASA-OTV/master.m3u8",
                    category = "Entertainment & Science 🚀",
                    isPreset = true
                )
            )
            channelDao.insertChannels(presets)
        }
    }
}
