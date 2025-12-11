# TransPlayer 技术设计文档（TDD）

**版本**: v1.0  
**创建日期**: 2025  
**最后更新**: 2025  
**对应PRD版本**: v1.0

---

## 1. 文档概述

### 1.1 文档目的
本文档详细描述TransPlayer项目的技术实现方案，包括系统架构、模块设计、接口定义、数据流设计等，为开发团队提供技术实现指导。

### 1.2 适用范围
- 开发团队
- 技术评审
- 代码审查
- 后续维护

### 1.3 技术栈版本
- **最低SDK**: API 26 (Android 8.0 Oreo)
- **目标SDK**: API 34 (Android 14)
- **编译SDK**: API 34
- **Kotlin版本**: 1.9.10+
- **Gradle版本**: 8.0+
- **Android Gradle Plugin**: 8.1.0+

---

## 2. 系统架构设计

### 2.1 整体架构

TransPlayer采用**MVVM + Clean Architecture**架构模式，分为三层：

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer                    │
│  ┌──────────────┐  ┌────────────────────────┐ │
│  │ Compose UI   │  │     ViewModels          │ │
│  │              │  │  (State Management)     │ │
│  └──────────────┘  └────────────────────────┘ │
└─────────────────────────────────────────────────┘
                    ↕ (Data Flow)
┌─────────────────────────────────────────────────┐
│            Domain Layer                         │
│  ┌──────────────┐  ┌────────────────────────┐ │
│  │  Use Cases   │  │   Domain Models        │ │
│  │  (Business   │  │   (Entities)           │ │
│  │   Logic)     │  │                        │ │
│  └──────────────┘  └────────────────────────┘ │
└─────────────────────────────────────────────────┘
                    ↕ (Repository Interface)
┌─────────────────────────────────────────────────┐
│             Data Layer                          │
│  ┌──────────────┐  ┌────────────────────────┐ │
│  │ Repositories │  │   Data Sources         │ │
│  │              │  │  (Local/Remote)        │ │
│  └──────────────┘  └────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

### 2.2 架构原则

1. **依赖倒置**: Domain层不依赖Data层，通过接口定义依赖关系
2. **单一职责**: 每个模块/类只负责一个功能
3. **开闭原则**: 对扩展开放，对修改关闭
4. **接口隔离**: 使用小粒度接口
5. **依赖注入**: 使用Hilt进行依赖注入

### 2.3 模块划分

```
app/
├── :core:common          # 通用工具和扩展
├── :core:ui              # UI组件和主题
├── :core:data            # 数据层基础
├── :feature:player       # 视频播放功能
├── :feature:subtitle     # 字幕生成功能
├── :feature:translation  # 翻译功能
└── :feature:settings     # 设置功能
```

---

## 3. 核心模块设计

### 3.1 视频播放模块

#### 3.1.1 模块结构

```
:feature:player/
├── data/
│   ├── repository/
│   │   └── VideoRepositoryImpl.kt
│   └── local/
│       ├── VideoDao.kt
│       └── PlaybackHistoryEntity.kt
├── domain/
│   ├── model/
│   │   ├── VideoSource.kt
│   │   └── PlaybackState.kt
│   └── usecase/
│       ├── PlayVideoUseCase.kt
│       └── SavePlaybackHistoryUseCase.kt
└── presentation/
    ├── ui/
    │   ├── PlayerScreen.kt
    │   ├── VideoPlayerView.kt
    │   └── PlayerControls.kt
    └── viewmodel/
        └── PlayerViewModel.kt
```

#### 3.1.2 核心类设计

**VideoSource (Domain Model)**
```kotlin
sealed class VideoSource {
    data class Local(val uri: Uri, val path: String) : VideoSource()
    data class Remote(val url: String, val type: StreamType) : VideoSource()
    
    enum class StreamType {
        HTTP, HLS, DASH
    }
}
```

**PlayerViewModel**
```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playVideoUseCase: PlayVideoUseCase,
    private val saveHistoryUseCase: SavePlaybackHistoryUseCase
) : ViewModel() {
    
    val uiState: StateFlow<PlayerUiState>
    val playbackState: StateFlow<PlaybackState>
    
    fun playVideo(source: VideoSource)
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun setPlaybackSpeed(speed: Float)
    // ...
}
```

**ExoPlayer集成**
```kotlin
class ExoPlayerManager @Inject constructor(
    private val context: Context
) {
    private var player: ExoPlayer? = null
    
    fun initializePlayer(): ExoPlayer {
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(createMediaSourceFactory())
            .build()
        return player!!
    }
    
    private fun createMediaSourceFactory(): MediaSourceFactory {
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, "TransPlayer")
        )
        return DefaultMediaSourceFactory(dataSourceFactory)
    }
}
```

#### 3.1.3 数据流

```
User Action (点击播放)
    ↓
PlayerViewModel.playVideo()
    ↓
PlayVideoUseCase.execute()
    ↓
VideoRepository.getVideoSource()
    ↓
ExoPlayerManager.loadMedia()
    ↓
ExoPlayer播放
    ↓
StateFlow更新 → UI更新
```

### 3.2 字幕生成模块

#### 3.2.1 模块结构

```
:feature:subtitle/
├── data/
│   ├── repository/
│   │   └── SubtitleRepositoryImpl.kt
│   ├── local/
│   │   ├── SubtitleDao.kt
│   │   └── SubtitleEntity.kt
│   └── mlkit/
│       └── SpeechRecognitionManager.kt
├── domain/
│   ├── model/
│   │   └── Subtitle.kt
│   └── usecase/
│       ├── GenerateSubtitleUseCase.kt
│       └── SaveSubtitleUseCase.kt
└── presentation/
    ├── ui/
    │   ├── SubtitleOverlay.kt
    │   └── SubtitleSettingsDialog.kt
    └── viewmodel/
        └── SubtitleViewModel.kt
```

#### 3.2.2 核心类设计

**Subtitle (Domain Model)**
```kotlin
data class Subtitle(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startTime: Long,  // 毫秒
    val endTime: Long,    // 毫秒
    val language: String,
    val videoId: String
)
```

**SpeechRecognitionManager**
```kotlin
class SpeechRecognitionManager @Inject constructor(
    private val context: Context
) {
    private var recognizer: SpeechRecognizer? = null
    private val audioProcessor = AudioProcessor()
    
    suspend fun startRecognition(
        audioSource: AudioSource,
        language: String,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ): Flow<RecognitionResult> = flow {
        recognizer = Speech.getClient(
            SpeechRecognitionOptions.Builder()
                .setLanguage(language)
                .setModel(ModelType.OFFLINE)  // 强制离线模式
                .build()
        )
        
        val audioStream = audioProcessor.extractAudio(audioSource)
        recognizer?.startListening(audioStream)?.addOnSuccessListener { result ->
            emit(RecognitionResult.Success(result.text))
        }?.addOnFailureListener { e ->
            emit(RecognitionResult.Error(e))
        }
    }
}
```

**AudioProcessor (音频提取)**
```kotlin
class AudioProcessor @Inject constructor() {
    
    suspend fun extractAudioFromVideo(
        videoUri: Uri,
        sampleRate: Int = 16000
    ): Flow<ByteArray> = flow {
        // 使用MediaExtractor提取音频
        val extractor = MediaExtractor()
        extractor.setDataSource(context, videoUri, null)
        
        val trackIndex = findAudioTrack(extractor)
        extractor.selectTrack(trackIndex)
        
        val format = extractor.getTrackFormat(trackIndex)
        val buffer = ByteBuffer.allocate(1024 * 64)
        
        while (extractor.sampleTime >= 0) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize > 0) {
                val audioData = ByteArray(sampleSize)
                buffer.get(audioData)
                emit(audioData)
            }
            extractor.advance()
        }
        
        extractor.release()
    }
    
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
}
```

#### 3.2.3 字幕生成流程

```
视频播放开始
    ↓
SubtitleViewModel.startGeneration()
    ↓
AudioProcessor.extractAudioFromVideo()
    ↓
SpeechRecognitionManager.startRecognition()
    ↓
ML Kit识别结果
    ↓
SubtitleGenerator.createSubtitle() (时间轴同步)
    ↓
SubtitleRepository.save()
    ↓
StateFlow更新 → UI显示字幕
```

### 3.3 翻译模块

#### 3.3.1 模块结构

```
:feature:translation/
├── data/
│   ├── repository/
│   │   └── TranslationRepositoryImpl.kt
│   ├── local/
│   │   └── TranslationModelManager.kt
│   └── tflite/
│       └── TranslationEngine.kt
├── domain/
│   ├── model/
│   │   └── Translation.kt
│   └── usecase/
│       └── TranslateSubtitleUseCase.kt
└── presentation/
    └── ui/
        └── TranslationSettings.kt
```

#### 3.3.2 核心类设计

**TranslationEngine (TensorFlow Lite)**
```kotlin
class TranslationEngine @Inject constructor(
    private val context: Context,
    private val modelManager: TranslationModelManager
) {
    private var interpreter: Interpreter? = null
    private val translationCache = TranslationCache()
    
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.Default) {
        // 检查缓存
        translationCache.get(text, sourceLang, targetLang)?.let {
            return@withContext Result.success(it)
        }
        
        // 加载模型
        val model = modelManager.loadModel(sourceLang, targetLang)
            ?: return@withContext Result.failure(
                ModelNotFoundException("Translation model not found")
            )
        
        // 初始化Interpreter
        if (interpreter == null) {
            interpreter = Interpreter(model)
        }
        
        // 执行翻译
        try {
            val input = preprocessText(text)
            val output = ByteArray(MAX_OUTPUT_LENGTH)
            
            interpreter?.run(input, output)
            
            val translatedText = postprocessText(output)
            
            // 缓存结果
            translationCache.put(text, sourceLang, targetLang, translatedText)
            
            Result.success(translatedText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun preprocessText(text: String): ByteBuffer {
        // 文本预处理：编码、分词等
        // ...
    }
    
    private fun postprocessText(output: ByteArray): String {
        // 文本后处理：解码、去噪等
        // ...
    }
}
```

**TranslationModelManager**
```kotlin
class TranslationModelManager @Inject constructor(
    private val context: Context,
    private val fileManager: ModelFileManager
) {
    private val modelsDir = File(context.filesDir, "models/translation")
    
    suspend fun loadModel(
        sourceLang: String,
        targetLang: String
    ): MappedByteBuffer? = withContext(Dispatchers.IO) {
        val modelFile = File(modelsDir, "$sourceLang-$targetLang.tflite")
        
        if (!modelFile.exists()) {
            return@withContext null
        }
        
        FileInputStream(modelFile).use { input ->
            input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                modelFile.length()
            )
        }
    }
    
    suspend fun downloadModel(
        sourceLang: String,
        targetLang: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        // 模型下载逻辑
        // ...
    }
}
```

#### 3.3.3 翻译流程

```
字幕生成/更新
    ↓
SubtitleViewModel.translateSubtitle()
    ↓
TranslateSubtitleUseCase.execute()
    ↓
TranslationEngine.translate()
    ↓
[检查缓存] → 命中则返回
    ↓
[加载模型] → 未安装则提示下载
    ↓
TensorFlow Lite推理
    ↓
结果缓存
    ↓
StateFlow更新 → UI显示翻译
```

### 3.4 模型管理模块

#### 3.4.1 模块结构

```
:core:data/
└── model/
    ├── ModelFileManager.kt
    ├── ModelDownloader.kt
    └── ModelMetadata.kt
```

#### 3.4.2 核心类设计

**ModelFileManager**
```kotlin
class ModelFileManager @Inject constructor(
    private val context: Context
) {
    private val modelsDir = File(context.filesDir, "models")
    
    fun getModelPath(type: ModelType, language: String? = null): File {
        return when (type) {
            ModelType.SPEECH_RECOGNITION -> {
                File(modelsDir, "speech/$language")
            }
            ModelType.TRANSLATION -> {
                val langPair = language ?: "default"
                File(modelsDir, "translation/$langPair.tflite")
            }
        }
    }
    
    suspend fun checkModelExists(type: ModelType, language: String): Boolean {
        return withContext(Dispatchers.IO) {
            getModelPath(type, language).exists()
        }
    }
    
    suspend fun getModelSize(type: ModelType, language: String): Long {
        return withContext(Dispatchers.IO) {
            getModelPath(type, language).length()
        }
    }
}
```

**ModelDownloader**
```kotlin
class ModelDownloader @Inject constructor(
    private val context: Context,
    private val fileManager: ModelFileManager,
    private val okHttpClient: OkHttpClient
) {
    suspend fun downloadModel(
        url: String,
        destination: File,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Download failed: ${response.code}")
                )
            }
            
            val body = response.body ?: return@withContext Result.failure(
                IOException("Response body is null")
            )
            
            val contentLength = body.contentLength()
            var downloadedBytes = 0L
            
            destination.parentFile?.mkdirs()
            destination.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = ((downloadedBytes * 100) / contentLength).toInt()
                        onProgress(progress)
                    }
                }
            }
            
            // 验证文件完整性（MD5/SHA256）
            verifyModelFile(destination)?.let { error ->
                return@withContext Result.failure(error)
            }
            
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun verifyModelFile(file: File): Exception? {
        // MD5/SHA256校验
        // ...
        return null
    }
}
```

---

## 4. 数据层设计

### 4.1 数据库设计

#### 4.1.1 Room数据库配置

```kotlin
@Database(
    entities = [
        PlaybackHistoryEntity::class,
        SubtitleEntity::class,
        VideoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TransPlayerDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun subtitleDao(): SubtitleDao
    abstract fun videoDao(): VideoDao
}
```

#### 4.1.2 数据表设计

**播放历史表 (playback_history)**
```kotlin
@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val videoUri: String,
    val videoTitle: String,
    val lastPosition: Long,  // 毫秒
    val duration: Long,       // 毫秒
    val lastPlayedAt: Long,   // 时间戳
    val videoType: String     // "local" or "remote"
)
```

**字幕表 (subtitles)**
```kotlin
@Entity(
    tableName = "subtitles",
    indices = [Index(value = ["videoId"])]
)
data class SubtitleEntity(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val language: String,
    val translatedText: String? = null,
    val targetLanguage: String? = null
)
```

**视频表 (videos)**
```kotlin
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val title: String,
    val duration: Long,
    val thumbnailPath: String? = null,
    val createdAt: Long
)
```

#### 4.1.3 DAO接口

```kotlin
@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<PlaybackHistoryEntity>
    
    @Query("SELECT * FROM playback_history WHERE videoId = :videoId")
    suspend fun getHistory(videoId: String): PlaybackHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)
    
    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface SubtitleDao {
    @Query("SELECT * FROM subtitles WHERE videoId = :videoId ORDER BY startTime ASC")
    suspend fun getSubtitles(videoId: String): List<SubtitleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: SubtitleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitles(subtitles: List<SubtitleEntity>)
    
    @Query("DELETE FROM subtitles WHERE videoId = :videoId")
    suspend fun deleteSubtitles(videoId: String)
    
    @Update
    suspend fun updateSubtitle(subtitle: SubtitleEntity)
}
```

### 4.2 DataStore配置

```kotlin
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    val subtitleSettings: Flow<SubtitleSettings> = dataStore.data
        .map { preferences ->
            SubtitleSettings(
                fontSize = preferences[PreferencesKeys.SUBTITLE_FONT_SIZE] ?: 16f,
                fontColor = preferences[PreferencesKeys.SUBTITLE_FONT_COLOR] ?: Color.White.value.toLong(),
                position = preferences[PreferencesKeys.SUBTITLE_POSITION] ?: SubtitlePosition.BOTTOM.name,
                showBackground = preferences[PreferencesKeys.SUBTITLE_SHOW_BACKGROUND] ?: true
            )
        }
    
    suspend fun updateSubtitleSettings(settings: SubtitleSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_FONT_SIZE] = settings.fontSize
            preferences[PreferencesKeys.SUBTITLE_FONT_COLOR] = settings.fontColor
            preferences[PreferencesKeys.SUBTITLE_POSITION] = settings.position
            preferences[PreferencesKeys.SUBTITLE_SHOW_BACKGROUND] = settings.showBackground
        }
    }
}
```

---

## 5. UI层设计

### 5.1 Compose UI架构

#### 5.1.1 主题配置

```kotlin
@Composable
fun TransPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### 5.1.2 播放器界面

```kotlin
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 视频播放器
        VideoPlayerView(
            player = viewModel.player,
            modifier = Modifier.fillMaxSize()
        )
        
        // 字幕叠加层
        SubtitleOverlay(
            subtitles = uiState.currentSubtitles,
            currentPosition = playbackState.currentPosition,
            settings = uiState.subtitleSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // 播放控制
        PlayerControls(
            isPlaying = playbackState.isPlaying,
            currentPosition = playbackState.currentPosition,
            duration = playbackState.duration,
            onPlayPause = { viewModel.togglePlayPause() },
            onSeek = { position -> viewModel.seekTo(position) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
```

#### 5.1.3 字幕叠加组件

```kotlin
@Composable
fun SubtitleOverlay(
    subtitles: List<Subtitle>,
    currentPosition: Long,
    settings: SubtitleSettings,
    modifier: Modifier = Modifier
) {
    val currentSubtitle = subtitles.find {
        currentPosition >= it.startTime && currentPosition <= it.endTime
    }
    
    currentSubtitle?.let { subtitle ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            contentAlignment = when (settings.position) {
                SubtitlePosition.TOP -> Alignment.TopCenter
                SubtitlePosition.CENTER -> Alignment.Center
                SubtitlePosition.BOTTOM -> Alignment.BottomCenter
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = if (settings.showBackground) {
                            Color.Black.copy(alpha = 0.7f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 原文
                Text(
                    text = subtitle.text,
                    fontSize = settings.fontSize.sp,
                    color = Color(settings.fontColor),
                    textAlign = TextAlign.Center
                )
                
                // 译文（如果存在）
                subtitle.translatedText?.let { translated ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translated,
                        fontSize = (settings.fontSize * 0.9f).sp,
                        color = Color(settings.fontColor).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
```

---

## 6. 依赖注入设计

### 6.1 Hilt模块配置

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TransPlayerDatabase {
        return Room.databaseBuilder(
            context,
            TransPlayerDatabase::class.java,
            "transplayer.db"
        ).build()
    }
    
    @Provides
    fun providePlaybackHistoryDao(database: TransPlayerDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }
    
    @Provides
    fun provideSubtitleDao(database: TransPlayerDatabase): SubtitleDao {
        return database.subtitleDao()
    }
}
```

### 6.2 Repository模块

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindVideoRepository(
        videoRepositoryImpl: VideoRepositoryImpl
    ): VideoRepository
    
    @Binds
    abstract fun bindSubtitleRepository(
        subtitleRepositoryImpl: SubtitleRepositoryImpl
    ): SubtitleRepository
    
    @Binds
    abstract fun bindTranslationRepository(
        translationRepositoryImpl: TranslationRepositoryImpl
    ): TranslationRepository
}
```

---

## 7. 关键技术实现细节

### 7.1 音频提取和实时处理

**实现方案**: 使用MediaExtractor + MediaCodec进行音频提取和解码

```kotlin
class AudioExtractor @Inject constructor(
    private val context: Context
) {
    suspend fun extractAudioStream(
        videoUri: Uri,
        sampleRate: Int = 16000
    ): Flow<AudioFrame> = flow {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, videoUri, null)
        
        val audioTrackIndex = findAudioTrack(extractor)
        if (audioTrackIndex == -1) {
            throw NoAudioTrackException()
        }
        
        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@flow
        val codec = MediaCodec.createDecoderByType(mime)
        
        codec.configure(format, null, null, 0)
        codec.start()
        
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        
        var inputEOS = false
        var outputEOS = false
        
        while (!outputEOS) {
            // 输入数据
            if (!inputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = inputBuffers[inputBufferIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, sampleSize,
                            presentationTimeUs, 0
                        )
                        extractor.advance()
                    }
                }
            }
            
            // 输出数据
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = outputBuffers[outputBufferIndex]
                
                if (bufferInfo.size > 0) {
                    val audioData = ByteArray(bufferInfo.size)
                    outputBuffer.get(audioData)
                    
                    // 重采样到目标采样率（如果需要）
                    val resampledData = resampleAudio(
                        audioData,
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        sampleRate
                    )
                    
                    emit(AudioFrame(
                        data = resampledData,
                        timestamp = bufferInfo.presentationTimeUs / 1000  // 转为毫秒
                    ))
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
                
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEOS = true
                }
            }
        }
        
        codec.stop()
        codec.release()
        extractor.release()
    }
}
```

### 7.2 字幕时间轴同步

**实现方案**: 基于音频时间戳和识别结果的时间戳进行同步

```kotlin
class SubtitleSynchronizer @Inject constructor() {
    
    fun createSubtitle(
        recognizedText: String,
        audioTimestamp: Long,
        previousSubtitle: Subtitle?,
        videoDuration: Long
    ): Subtitle {
        val startTime = previousSubtitle?.endTime ?: audioTimestamp
        val endTime = calculateEndTime(
            startTime = startTime,
            textLength = recognizedText.length,
            videoDuration = videoDuration
        )
        
        return Subtitle(
            text = recognizedText,
            startTime = startTime,
            endTime = endTime,
            language = "auto-detected"
        )
    }
    
    private fun calculateEndTime(
        startTime: Long,
        textLength: Int,
        videoDuration: Long
    ): Long {
        // 根据文本长度估算显示时长
        // 平均阅读速度: 约3-4字/秒
        val estimatedDuration = (textLength / 3.5 * 1000).toLong()
        
        val minDuration = 2000L  // 最小2秒
        val maxDuration = 10000L // 最大10秒
        
        val duration = estimatedDuration.coerceIn(minDuration, maxDuration)
        val endTime = (startTime + duration).coerceAtMost(videoDuration)
        
        return endTime
    }
}
```

### 7.3 翻译缓存策略

**实现方案**: 使用LRU缓存存储翻译结果

```kotlin
class TranslationCache @Inject constructor() {
    private val cache = object : LinkedHashMap<String, String>(
        100, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    
    private val lock = Mutex()
    private val MAX_CACHE_SIZE = 500
    
    suspend fun get(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? = withContext(Dispatchers.Default) {
        lock.withLock {
            cache[buildKey(text, sourceLang, targetLang)]
        }
    }
    
    suspend fun put(
        text: String,
        sourceLang: String,
        targetLang: String,
        translatedText: String
    ) = withContext(Dispatchers.Default) {
        lock.withLock {
            cache[buildKey(text, sourceLang, targetLang)] = translatedText
        }
    }
    
    private fun buildKey(text: String, sourceLang: String, targetLang: String): String {
        return "$sourceLang|$targetLang|${text.hashCode()}"
    }
}
```

### 7.4 模型文件管理

**实现方案**: 使用WorkManager进行后台下载

```kotlin
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelType = inputData.getString(KEY_MODEL_TYPE) ?: return@withContext Result.failure()
        val language = inputData.getString(KEY_LANGUAGE) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        
        try {
            val downloader = ModelDownloader(applicationContext)
            val fileManager = ModelFileManager(applicationContext)
            
            val destination = fileManager.getModelPath(
                ModelType.valueOf(modelType),
                language
            )
            
            downloader.downloadModel(
                url = downloadUrl,
                destination = destination,
                onProgress = { progress ->
                    setProgressAsync(
                        workDataOf(KEY_PROGRESS to progress)
                    )
                }
            )
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        const val KEY_MODEL_TYPE = "model_type"
        const val KEY_LANGUAGE = "language"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PROGRESS = "progress"
    }
}
```

---

## 8. 性能优化策略

### 8.1 内存优化

1. **字幕数据分页加载**
   - 只加载当前播放位置附近的字幕
   - 使用滑动窗口机制

2. **模型内存管理**
   - 按需加载模型
   - 使用后及时释放
   - 使用MappedByteBuffer减少内存拷贝

3. **图片缓存**
   - 使用Coil进行图片加载和缓存
   - 限制缓存大小

### 8.2 CPU优化

1. **后台处理**
   - 使用Dispatchers.Default进行CPU密集型任务
   - 使用Dispatchers.IO进行IO操作

2. **批量处理**
   - 字幕翻译批量处理
   - 减少模型推理次数

3. **GPU加速**
   - TensorFlow Lite GPU delegate（如果可用）
   - 检查设备GPU支持情况

### 8.3 电池优化

1. **WorkManager策略**
   - 模型下载使用BatteryNotLow约束
   - 避免在低电量时进行大量计算

2. **智能降频**
   - 检测设备电量
   - 低电量时降低处理频率

3. **后台限制**
   - 使用ForegroundService进行长时间处理
   - 合理使用WakeLock

---

## 9. 错误处理和日志

### 9.1 错误处理策略

```kotlin
sealed class AppError {
    data class NetworkError(val message: String) : AppError()
    data class ModelNotFoundError(val modelType: String) : AppError()
    data class TranslationError(val cause: Throwable) : AppError()
    data class RecognitionError(val cause: Throwable) : AppError()
    data class VideoLoadError(val message: String) : AppError()
}

class ErrorHandler @Inject constructor() {
    fun handleError(error: AppError): UserMessage {
        return when (error) {
            is NetworkError -> UserMessage(
                title = "网络错误",
                message = "请检查网络连接",
                action = "重试"
            )
            is ModelNotFoundError -> UserMessage(
                title = "模型未安装",
                message = "请先下载${error.modelType}模型",
                action = "下载"
            )
            // ...
        }
    }
}
```

### 9.2 日志系统

```kotlin
object AppLogger {
    private const val TAG = "TransPlayer"
    
    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
        // 可以在这里添加崩溃报告（如Firebase Crashlytics）
    }
}
```

---

## 10. 测试策略

### 10.1 单元测试

- **UseCase测试**: 测试业务逻辑
- **Repository测试**: Mock数据源，测试数据转换
- **工具类测试**: 测试工具函数

### 10.2 UI测试

- **Compose UI测试**: 使用ComposeTestRule
- **集成测试**: 测试完整用户流程

### 10.3 性能测试

- **内存泄漏检测**: 使用LeakCanary
- **性能分析**: 使用Android Profiler
- **基准测试**: 测试关键操作耗时

---

## 11. 构建配置

### 11.1 build.gradle.kts (Module: app)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.transplayer.app"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.transplayer.app"
        minSdk = 26  // Android 8.0
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    
    // ML Kit
    implementation("com.google.mlkit:speech-recognition:17.0.1")
    
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

---

## 12. 安全考虑

### 12.1 数据安全

- **模型文件校验**: 下载后验证MD5/SHA256
- **敏感数据加密**: 使用Android Keystore
- **文件访问控制**: 使用应用私有目录

### 12.2 代码安全

- **代码混淆**: ProGuard/R8
- **API密钥保护**: 不在代码中硬编码
- **网络安全**: 使用HTTPS

---

## 13. 部署和发布

### 13.1 版本管理

- **版本号规则**: MAJOR.MINOR.PATCH
- **版本代码**: 递增整数

### 13.2 发布流程

1. 代码审查
2. 自动化测试
3. 构建Release APK/AAB
4. 内部测试
5. Google Play审核
6. 分阶段发布

---

**文档结束**

