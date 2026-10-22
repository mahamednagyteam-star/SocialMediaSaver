# Third-party notices

## yt-dlp-android
- Project: https://github.com/yausername/youtubedl-android
- License: GNU General Public License v3.0 (GPL-3.0)
- Used by Media Saver as the local media extraction and download engine.
- This app's integration is a new adapter written for Media Saver; the upstream library's source and notices remain available at the linked project and through Maven Central.

## FFmpeg
- The separate FFmpeg artifact is not included in the current app build to reduce APK size.
- Audio-only downloads save an available audio stream directly; MP3 transcoding and merging separate audio/video streams are not enabled.

## FetchIt
- https://github.com/tharunbirla/FetchIt
- MIT license; tracked as an unmodified Git submodule under upstream/FetchIt.

## Hazel
- https://github.com/SibtainOcn/Hazel
- GPL-3.0-or-later; tracked as an unmodified Git submodule under upstream/Hazel.

## SuperX Video Downloader
- https://github.com/alexch33/super-video-downloader
- GPL-3.0; tracked as an unmodified Git submodule under upstream/SuperX.

The upstream submodules retain their own source files and license notices. Media Saver's new adapter code is Copyright (c) 2026 SocialMediaSaver contributors and distributed under GPL-3.0.

## FetchIt fallback adapter
- Source: https://github.com/tharunbirla/FetchIt
- License: MIT; copyright notice: Copyright (c) 2021 Felvin Search.
- Selected URL-resolution logic was adapted and modified in `FetchItFallbackResolver.kt` for fallback use only. The original FetchIt application is not embedded.
- Full MIT license text is in `licenses/FETCHIT-MIT.txt`.


## XREFS0 social-media-downloader
- Source: https://github.com/XREFS0/social-media-downloader
- License: MIT; copyright notice: Copyright (c) 2026 XREFS0.
- The Android app adapts the upstream yt-dlp workflow, platform coverage, quality selection, and audio-only download approach. The Telegram bot and Telegram-specific handlers are not included.
- Full MIT license text is in `licenses/XREFS0-SOCIAL-MEDIA-DOWNLOADER-MIT.txt`.
