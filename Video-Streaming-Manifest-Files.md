# Video Streaming — Manifest Files

## What Is a Manifest File?

A manifest file in video streaming is a metadata document that tells the player what's available and how to assemble the stream. It doesn't contain video data — it's an index that points to the actual media segments.

### What It Contains

- Available quality levels (bitrates, resolutions, codecs)
- URLs to each media segment (small chunks, typically 2–10 seconds each)
- Duration of each segment
- Audio track options (languages, formats)
- Subtitle tracks
- Encryption/DRM info (if protected)

### How It Works

```
1. Player requests the manifest file from the server
2. Manifest lists all available qualities: 360p, 720p, 1080p, 4K
3. Player picks a quality based on current bandwidth
4. Player downloads segments sequentially from the URLs in the manifest
5. If bandwidth drops, player switches to a lower quality mid-stream (adaptive bitrate)
```

---

## Why Manifest Files Exist

The core reason: you can't stream a single monolithic video file adaptively over HTTP. The manifest solves several problems that a direct file download can't.

### 1. Adaptive Bitrate (the main reason)

Without a manifest, the player has one option: download the whole file at one quality. If bandwidth drops mid-stream, you buffer and stall.

With a manifest, the player knows all available qualities upfront and can switch segment-by-segment. Watching on WiFi? Grab 1080p segments. Walked into an elevator? Next segment comes as 360p. No rebuffering.

### 2. Chunked Delivery

A 2-hour movie as one file means:
- Can't start playing until enough is downloaded
- Seeking to minute 90 requires downloading or range-requesting blind byte offsets
- CDN can't cache it efficiently

Manifest + segments means:
- Player fetches segment 1, starts playing immediately
- Seeking to minute 90 = look up segment 540 in the manifest, fetch directly
- Each segment is a small, independent, cacheable file

### 3. CDN Compatibility

CDNs are designed to cache static files. A manifest architecture turns video into thousands of small static files that cache perfectly at edge nodes worldwide. No special streaming server needed — just a regular HTTP file server behind a CDN.

### 4. Multi-Track Support

One video might have:
- 5 quality levels
- 3 audio languages
- 4 subtitle tracks

The manifest is the single entry point that describes all combinations. The player picks what it needs without downloading everything.

### 5. Live Streaming

For live content, the manifest is a sliding window that the server updates every few seconds with new segments. The player polls the manifest, sees new segments, fetches them. This works over plain HTTP — no persistent connection, no custom protocol.

### 6. DRM and Encryption

The manifest tells the player: "these segments are encrypted with key X, fetch the key from this license server." Without it, the player wouldn't know how to decrypt.

---

## Two Main Formats

| Protocol | Manifest Format | Extension | Created By |
|----------|----------------|-----------|------------|
| HLS (HTTP Live Streaming) | M3U8 playlist | `.m3u8` | Apple |
| DASH (Dynamic Adaptive Streaming over HTTP) | Media Presentation Description | `.mpd` (XML) | MPEG consortium |

### HLS — Master Playlist Example

The master playlist lists all available quality levels:

```
#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
quality_360p/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2400000,RESOLUTION=1280x720
quality_720p/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
quality_1080p/playlist.m3u8
```

### HLS — Media Playlist Example

Each quality-level playlist lists individual segment URLs:

```
#EXTM3U
#EXT-X-TARGETDURATION:6
#EXTINF:6.0,
segment_001.ts
#EXTINF:6.0,
segment_002.ts
#EXTINF:4.5,
segment_003.ts
#EXT-X-ENDLIST
```

### DASH — MPD Example (simplified)

```xml
<MPD type="static" mediaPresentationDuration="PT1H30M">
  <Period>
    <AdaptationSet mimeType="video/mp4">
      <Representation bandwidth="800000" width="640" height="360">
        <SegmentTemplate media="360p/segment_$Number$.m4s" startNumber="1"/>
      </Representation>
      <Representation bandwidth="2400000" width="1280" height="720">
        <SegmentTemplate media="720p/segment_$Number$.m4s" startNumber="1"/>
      </Representation>
      <Representation bandwidth="5000000" width="1920" height="1080">
        <SegmentTemplate media="1080p/segment_$Number$.m4s" startNumber="1"/>
      </Representation>
    </AdaptationSet>
    <AdaptationSet mimeType="audio/mp4" lang="en">
      <Representation bandwidth="128000">
        <SegmentTemplate media="audio_en/segment_$Number$.m4s" startNumber="1"/>
      </Representation>
    </AdaptationSet>
  </Period>
</MPD>
```

---

## HLS vs DASH

| Aspect | HLS | DASH |
|--------|-----|------|
| Format | Text-based playlist (.m3u8) | XML document (.mpd) |
| Segment format | `.ts` (MPEG-TS) or `.fmp4` | `.m4s` (fragmented MP4) |
| Browser support | Safari native; others via hls.js | All modern browsers via MSE |
| Live latency | ~15–30s (standard), ~2s (Low-Latency HLS) | ~3–5s (standard), <2s (CMAF low-latency) |
| DRM | FairPlay (Apple) | Widevine (Google), PlayReady (Microsoft) |
| Adoption | iOS, Apple TV, HLS everywhere | YouTube, Netflix, most non-Apple platforms |
| Standardization | Apple proprietary (RFC 8216) | ISO/IEC 23009 (open standard) |

In practice, most services encode once and generate both HLS and DASH manifests pointing to the same underlying segments (using CMAF — Common Media Application Format).

---

## The Pre-Manifest Era

Before manifest-based streaming (pre-2009):
- **RTMP** (Real-Time Messaging Protocol) required special Flash-based servers
- **RTSP** (Real-Time Streaming Protocol) needed persistent connections and custom infrastructure
- Neither worked with CDNs
- No adaptive bitrate — pick a quality and hope for the best
- Flash dependency for browser playback

Manifest-based streaming (HLS/DASH) replaced all of this with plain HTTP, standard CDNs, and adaptive quality — using only the technology web servers already had.

---

## System Design Relevance

When designing a video streaming system, the manifest is the coordination point:

```
                          ┌─────────────────┐
                          │  Origin Server  │
                          │  (manifests +   │
                          │   segments)     │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │       CDN       │
                          │  (caches both   │
                          │   manifests +   │
                          │   segments)     │
                          └────────┬────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
     ┌────────▼──────┐   ┌───────▼───────┐   ┌───────▼───────┐
     │  Player A     │   │  Player B     │   │  Player C     │
     │  (1080p WiFi) │   │  (360p 3G)    │   │  (720p 4G)    │
     └───────────────┘   └───────────────┘   └───────────────┘
```

- **Manifest TTL:** For live streams, set short cache TTL on manifests (2–6s) but long TTL on segments (they don't change)
- **Failover:** Players can request manifests from fallback CDNs if primary fails
- **Personalization:** Manifests can be generated per-user to include/exclude tracks based on subscription tier or geographic restrictions
- **Ad insertion:** Server-side ad insertion (SSAI) works by modifying the manifest to splice ad segments into the content timeline
