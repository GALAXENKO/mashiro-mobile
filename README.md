# Mashiro Mobile

Неофициальный Android-клиент Яндекс Музыки — порт десктопного приложения
[aeviww/yandex-music-client](https://github.com/aeviww/yandex-music-client)
(Tauri 2 + Vue 3) на нативный Android (Java, только фреймворк, без AndroidX и Gradle).

- **Пакет:** `com.mashiro.mobile`
- **Версия:** 1.0.0 · minSdk 23 · target 34
- **Язык интерфейса:** русский
- **Стиль:** монохромный Mashiro, тёмная/светлая тема, акцентные цвета

## Сборка APK

Не нужен Gradle/NDK — используется прямой цепочкой aapt2 → javac → d8 → zipalign → apksigner
из Android SDK/JDK (те же инструменты, что применяются в проекте NovaMobile).

Требования:
- JDK 17: `%LOCALAPPDATA%\NovaBrowserBuild\android\jdk\jdk-17.0.20.1+1`
- Android SDK: `%LOCALAPPDATA%\NovaBrowserBuild\android\sdk` (build-tools 35.0.0, platform android-35)

Сборка:

```powershell
powershell -ExecutionPolicy Bypass -File MashiroMobile\build-apk.ps1
```

Результат: `MashiroMobile\build\Mashiro-1.0.0.apk` (подписан ключом
`build\mashiro.keystore`, генерируется автоматически при первом запуске).

Установка:

```powershell
adb install -r MashiroMobile\build\Mashiro-1.0.0.apk
```

## Возможности

- **Вход:** OAuth по коду с устройства (device code, без браузера) либо ручной ввод
  токена; сессия сохраняется локально.
- **Дом / Моя волна / Поиск / Медиатека** — нижняя навигация, адаптированная под телефон.
- **Поиск:** треки, артисты, альбомы, плейлисты, подсказки, история запросов.
- **Медиатека:** «Мне нравится», свои плейлисты, скачанные треки, офлайн-поиск по своей музыке.
- **Каталог:** страницы альбома, артиста, плейлиста (включая чужие публичные).
- **Плеер:** полноэкранный экран, очередь (bottom-sheet), перемешивание, повторы,
  таймер сна, синхронизированные тексты песен (LRC), мини-плеер.
- **Фоновая музыка:** служба с уведомлением и медиа-сессией (mediaPlayback
  foreground service), кнопки на уведомлении и гарнитуре.
- **Моя волна:** станции, сессии ротора, фидбек like/dislike/skip/trackFinished,
  автоматическое продление очереди.
- **Загрузки:** треки сохраняются в приватную папку Music приложения, локальные
  файлы получают приоритет при воспроизведении; кэш обложек (LruCache + диск).
- **Настройки:** тема, акцентный цвет, качество потока (low/normal/high/lossless),
  статистика прослушиваний, очистка кэша и истории.

## API и авторские права

Приложение использует неофициальный API Яндекс Музыки (зеркало логики из
оригинального Rust-клиента: авторизация, теги для стримов с MD5-подписью, LRC
через lrclib/supplement). Все треки, обложки и тексты принадлежат их
правообладателям и загружаются через официальный сервис. Приложение не связано
с Яндексом и требует аккаунт с активной подпиской.

## Что не перенесено из десктопной версии

Discord RPC, системный трей, глобальные горячие клавиши, комнаты
«Слушать вместе», кроссфейд, эквалайзер, отдельное окно мини-плеера, пометки
«на NN-й секунде цензура» / AI-контент (документировано как справочная доработка).

## Структура

```
MashiroMobile/
├── AndroidManifest.xml
├── build-apk.ps1            # скрипт сборки без Gradle
├── res/                     # стили, строки, векторные иконки, лаунчер
└── src/com/mashiro/mobile/
    ├── api/                 # Models.java (DTO), YandexApi.java (сетевой клиент)
    ├── audio/               # Playback.java, PlayerService.java, MediaButtonReceiver.java
    ├── screens/             # Home, Wave, Search, Library, Liked, Album, Artist,
    │                        # Playlist, Login, Settings, Stats, LibrarySearch
    ├── ui/                  # PlayerScreen, TrackMenu, PlaylistMenu, LyricsScreen, QueueSheet
    ├── App.java             # точка входа, инициализация
    ├── Downloader.java      # загрузка треков
    ├── ImageLoader.java     # загрузка обложек с кэшем
    ├── MainActivity.java    # навигация, мини-плеер, помощники
    ├── Screen.java          # базовый экран
    ├── Store.java           # сессия/настройки/лайки/очередь/статистика
    ├── Theme.java, Ui.java, Widgets.java   # тема и UI-тулкит
```

Логика клиента (tracks/session, wave/feedback, download-info, lyrics, likes,
playlists CRUD) зеркалирует `desktop/src-tauri/src/yandex/` из оригинального
репозитория.