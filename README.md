# TransPlayer

**即时字幕播放器** - 支持实时字幕生成和翻译的Android视频播放器

## 项目简介

TransPlayer是一个功能强大的Android视频播放器，主要特性包括：

- ✅ 本地视频文件播放
- ✅ 网络流媒体播放
- ✅ 实时本地语音识别生成字幕
- ✅ 实时本地翻译字幕
- ✅ 完全离线运行
- ✅ 字幕管理和导出

## 技术栈

- **开发语言**: Kotlin 100%
- **架构模式**: MVVM + Clean Architecture
- **UI框架**: Jetpack Compose + Material Design 3
- **依赖注入**: Hilt
- **视频播放**: ExoPlayer
- **语音识别**: ML Kit Speech Recognition (离线模式)
- **翻译引擎**: TensorFlow Lite
- **数据存储**: Room Database + DataStore
- **异步处理**: Kotlin Coroutines + Flow

## 开发环境要求

- **最低SDK**: API 26 (Android 8.0 Oreo)
- **目标SDK**: API 34 (Android 14)
- **编译SDK**: API 34
- **Android Studio**: Hedgehog+
- **Gradle**: 8.0+
- **Kotlin**: 1.9.10+

## 项目结构

```
app/
├── src/main/java/com/transplayer/app/
│   ├── data/              # 数据层
│   │   ├── local/         # 本地数据源（Room、DataStore）
│   │   └── repository/    # Repository实现
│   ├── domain/            # 领域层
│   │   ├── model/         # 领域模型
│   │   └── usecase/       # 业务用例
│   ├── presentation/      # 表现层
│   │   ├── ui/           # Compose UI组件
│   │   └── viewmodel/    # ViewModel
│   ├── di/               # 依赖注入模块
│   ├── ui/theme/         # UI主题配置
│   └── util/             # 工具类
└── src/main/res/         # 资源文件
```

## 文档

项目文档位于 `docs/` 目录：

- `docs/01-requirements/` - 需求文档（PRD）
- `docs/02-design/` - 技术设计文档（TDD）
- `docs/03-iteration-plan/` - 迭代开发计划
- `docs/04-development/` - 迭代开发文档

## 开发计划

项目采用迭代开发方式，共10个迭代：

1. **Iteration 1**: 项目基础搭建 ✅
2. **Iteration 2**: 基础播放器
3. **Iteration 3**: 播放控制增强
4. **Iteration 4**: 字幕生成基础
5. **Iteration 5**: 字幕显示优化
6. **Iteration 6**: 翻译功能基础
7. **Iteration 7**: 翻译功能完善
8. **Iteration 8**: 字幕管理功能
9. **Iteration 9**: 性能优化
10. **Iteration 10**: 测试和发布

## 构建和运行

1. 克隆项目
```bash
git clone <repository-url>
cd TransPlayer
```

2. 使用Android Studio打开项目

3. 同步Gradle依赖

4. 运行项目

## 许可证

[待定]

## 贡献

[待定]





