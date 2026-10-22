# Upstream open-source projects

This repository tracks the following projects as Git submodules under `upstream/`.
Their upstream source is pinned to a specific commit so changes are reviewable and repeatable.

- **FetchIt** — https://github.com/tharunbirla/FetchIt — MIT License.
  Useful reference for URL-based multi-platform downloads and share intents.
- **Hazel** — https://github.com/SibtainOcn/Hazel — GPL-3.0-or-later.
  Useful reference for yt-dlp-based extraction, queues, format selection, and background downloads.
- **SuperX Video Downloader** — https://github.com/alexch33/super-video-downloader — GPL-3.0-only.
  Useful reference for yt-dlp, HLS/DASH streams, browser integration, and download management.

## Important integration status

These upstreams are tracked as source submodules, not merged Android modules. The current
Media Saver APK still builds only the `:app` module and currently supports direct media-file
URLs. The upstream projects have different architectures, dependencies, native build
requirements, and license obligations; their features must be ported or integrated deliberately
before claiming that they are part of the APK. Any copied or adapted code must retain the
required notices and comply with the applicable license, including GPL requirements where
applicable.

Initialize the sources locally with:

```bash
git submodule update --init --recursive
```
