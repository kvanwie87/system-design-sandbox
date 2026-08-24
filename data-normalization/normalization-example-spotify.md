# Normalization Walkthrough: Spotify Song Catalog (PostgreSQL)

This document takes the song catalog metadata from a Spotify-like music streaming platform — the data explicitly stored in PostgreSQL — and walks through the normalization process from an unnormalized flat table to BCNF.

---

## Scope: What Lives in PostgreSQL?

Per the system design, PostgreSQL stores the **song catalog metadata**:

> "Song, artist, and album data has natural relationships: songs belong to albums, albums belong to artists, artists can collaborate on songs. A relational database handles these relationships well with foreign keys and joins."

This includes:
- Song metadata (title, duration, genre, play count, audio path)
- Artist profiles (name, bio, image, monthly listeners, verified status)
- Album data (title, track lists, artwork, release info)

Everything else (playlists → Cassandra, listening history → Cassandra, sessions → Redis, search → Elasticsearch) is out of scope for this exercise.

---

## Step 0: Raw, Unnormalized Data

Imagine the catalog was originally tracked in a single flat export from a content ingestion system:

| SongID | SongTitle | Duration | Genres | AudioPath | PlayCount | ArtistID | ArtistName | ArtistBio | ArtistImage | MonthlyListeners | Verified | AlbumID | AlbumTitle | AlbumArt | ReleaseDate | TrackNumber |
|--------|-----------|----------|--------|-----------|-----------|----------|------------|-----------|-------------|-----------------|----------|---------|------------|----------|-------------|-------------|
| S1 | After Hours | 3:23 | R&B, Pop | songs/s1/ | 2500000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 11 |
| S2 | Blinding Lights | 3:20 | Synth-pop, R&B | songs/s2/ | 4000000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 9 |
| S3 | Save Your Tears | 3:35 | Pop, R&B | songs/s3/ | 3000000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 5 |
| S4 | Levitating | 3:23 | Disco, Pop | songs/s4/ | 2000000000 | AR2 | Dua Lipa | English-Albanian singer... | /img/ar2.jpg | 70000000 | true | AL2 | Future Nostalgia | /img/al2.jpg | 2020-03-27 | 5 |
| S5 | Don't Start Now | 3:03 | Disco, Pop | songs/s5/ | 2800000000 | AR2 | Dua Lipa | English-Albanian singer... | /img/ar2.jpg | 70000000 | true | AL2 | Future Nostalgia | /img/al2.jpg | 2020-03-27 | 3 |
| S6 | Peaches | 2:21 | Pop, R&B | songs/s6/ | 1800000000 | AR3 | Justin Bieber | Canadian singer... | /img/ar3.jpg | 90000000 | true | AL3 | Justice | /img/al3.jpg | 2021-03-19 | 3 |
| S7 | Stay | 3:18 | Pop | songs/s7/ | 2200000000 | AR3 | Justin Bieber | Canadian singer... | /img/ar3.jpg | 90000000 | true | AL3 | Justice | /img/al3.jpg | 2021-03-19 | 5 |
| S8 | Montero | 2:17 | Hip-Hop, Pop | songs/s8/ | 1500000000 | AR4 | Lil Nas X | American rapper... | /img/ar4.jpg | 55000000 | true | AL4 | Montero | /img/al4.jpg | 2021-09-17 | 1 |

### Problems with this table

- **Repeating groups:** `Genres` contains comma-separated lists (violates atomicity).
- **Redundancy:**
  - The Weeknd's artist info (name, bio, image, monthly listeners, verified) is duplicated across 3 rows (S1, S2, S3).
  - The album "After Hours" info (title, art, release date) is duplicated across 3 rows.
  - Dua Lipa's info is duplicated across 2 rows.
  - "Justice" album info is duplicated across 2 rows.
- **Anomalies possible:**
  - **Update:** The Weeknd's monthly listener count changes. We must update 3 rows (and every other song row they release). Miss one and data is inconsistent.
  - **Insertion:** We sign a new artist but they haven't released any songs yet. We can't add them without fabricating song data.
  - **Deletion:** If we remove all songs from album AL4, we lose the album's metadata entirely.

---

## Step 1: Identify Entities and Keys

The catalog contains three distinct entities with natural relationships:

| Entity | Key | Attributes |
|--------|-----|------------|
| Artist | ArtistID | ArtistName, ArtistBio, ArtistImage, MonthlyListeners, Verified |
| Album | AlbumID | AlbumTitle, AlbumArt, ReleaseDate, ArtistID |
| Song | SongID | SongTitle, Duration, Genres, AudioPath, PlayCount, ArtistID, AlbumID, TrackNumber |

Relationships:
- An artist has many albums
- An album has many songs
- A song belongs to one album and one artist

---

## Step 2: Remove Repeating Groups → 1NF

**Rule:** Every column must hold a single atomic value. No lists, no repeating groups.

The `Genres` column contains comma-separated values like `"R&B, Pop"`. We have two options:

**Option A:** Create a separate `Song_Genres` junction table (one row per song-genre pair).
**Option B:** Use PostgreSQL's native `VARCHAR[]` array type.

The Spotify design doc schema uses `genres VARCHAR[]` — a PostgreSQL array. This is a pragmatic choice: genres are queried as a set (filter songs by genre) but never need their own attributes. PostgreSQL arrays are atomic from the DB engine's perspective and support indexing with GIN indexes.

We'll follow the design doc and use `VARCHAR[]`, but acknowledge this is a PostgreSQL-specific relaxation of strict 1NF. The table after removing true repeating groups:

| SongID | SongTitle | Duration | Genres | AudioPath | PlayCount | ArtistID | ArtistName | ArtistBio | ArtistImage | MonthlyListeners | Verified | AlbumID | AlbumTitle | AlbumArt | ReleaseDate | TrackNumber |
|--------|-----------|----------|--------|-----------|-----------|----------|------------|-----------|-------------|-----------------|----------|---------|------------|----------|-------------|-------------|
| S1 | After Hours | 3:23 | {R&B, Pop} | songs/s1/ | 2500000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 11 |
| S2 | Blinding Lights | 3:20 | {Synth-pop, R&B} | songs/s2/ | 4000000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 9 |
| S3 | Save Your Tears | 3:35 | {Pop, R&B} | songs/s3/ | 3000000000 | AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true | AL1 | After Hours | /img/al1.jpg | 2020-03-20 | 5 |
| S4 | Levitating | 3:23 | {Disco, Pop} | songs/s4/ | 2000000000 | AR2 | Dua Lipa | English-Albanian singer... | /img/ar2.jpg | 70000000 | true | AL2 | Future Nostalgia | /img/al2.jpg | 2020-03-27 | 5 |
| S5 | Don't Start Now | 3:03 | {Disco, Pop} | songs/s5/ | 2800000000 | AR2 | Dua Lipa | English-Albanian singer... | /img/ar2.jpg | 70000000 | true | AL2 | Future Nostalgia | /img/al2.jpg | 2020-03-27 | 3 |
| S6 | Peaches | 2:21 | {Pop, R&B} | songs/s6/ | 1800000000 | AR3 | Justin Bieber | Canadian singer... | /img/ar3.jpg | 90000000 | true | AL3 | Justice | /img/al3.jpg | 2021-03-19 | 3 |
| S7 | Stay | 3:18 | {Pop} | songs/s7/ | 2200000000 | AR3 | Justin Bieber | Canadian singer... | /img/ar3.jpg | 90000000 | true | AL3 | Justice | /img/al3.jpg | 2021-03-19 | 5 |
| S8 | Montero | 2:17 | {Hip-Hop, Pop} | songs/s8/ | 1500000000 | AR4 | Lil Nas X | American rapper... | /img/ar4.jpg | 55000000 | true | AL4 | Montero | /img/al4.jpg | 2021-09-17 | 1 |

**Primary Key:** `SongID`

**What we fixed:**
- Genres are stored as a PostgreSQL array (atomic from the engine's perspective).
- Each row is uniquely identified by `SongID`.

**What's still wrong:**
- `ArtistName`, `ArtistBio`, `ArtistImage`, `MonthlyListeners`, `Verified` depend only on `ArtistID` — not on `SongID`. This is a transitive dependency since `SongID` is the only key here.
- `AlbumTitle`, `AlbumArt`, `ReleaseDate` depend only on `AlbumID` — not on `SongID`. Also transitive.

> Note: With a single-column primary key, there are no *partial* dependencies (those only exist with composite keys). We jump straight to addressing transitive dependencies.

---

## Step 3: Skip 2NF (Not Applicable)

**Rule:** 2NF addresses partial dependencies — where a non-key column depends on *part* of a composite primary key.

Our primary key is a single column (`SongID`), so partial dependencies are impossible. The table is already in 2NF by definition.

We proceed directly to 3NF.

---

## Step 4: Remove Transitive Dependencies → 3NF

**Rule:** No non-key column should depend on another non-key column.

Transitive dependency chains:
```
SongID → ArtistID → ArtistName, ArtistBio, ArtistImage, MonthlyListeners, Verified
SongID → AlbumID  → AlbumTitle, AlbumArt, ReleaseDate
```

The artist attributes describe the *artist*, not the song. The album attributes describe the *album*, not the song. Extract them.

**Decompose into:**

### Artists Table

| ArtistID | ArtistName | ArtistBio | ArtistImage | MonthlyListeners | Verified |
|----------|------------|-----------|-------------|-----------------|----------|
| AR1 | The Weeknd | Canadian singer... | /img/ar1.jpg | 85000000 | true |
| AR2 | Dua Lipa | English-Albanian singer... | /img/ar2.jpg | 70000000 | true |
| AR3 | Justin Bieber | Canadian singer... | /img/ar3.jpg | 90000000 | true |
| AR4 | Lil Nas X | American rapper... | /img/ar4.jpg | 55000000 | true |

**Primary Key:** `ArtistID`

### Albums Table

| AlbumID | AlbumTitle | AlbumArt | ReleaseDate | ArtistID |
|---------|------------|----------|-------------|----------|
| AL1 | After Hours | /img/al1.jpg | 2020-03-20 | AR1 |
| AL2 | Future Nostalgia | /img/al2.jpg | 2020-03-27 | AR2 |
| AL3 | Justice | /img/al3.jpg | 2021-03-19 | AR3 |
| AL4 | Montero | /img/al4.jpg | 2021-09-17 | AR4 |

**Primary Key:** `AlbumID`
**Foreign Key:** `ArtistID` → Artists

### Songs Table (revised)

| SongID | SongTitle | Duration | Genres | AudioPath | PlayCount | ArtistID | AlbumID | TrackNumber |
|--------|-----------|----------|--------|-----------|-----------|----------|---------|-------------|
| S1 | After Hours | 3:23 | {R&B, Pop} | songs/s1/ | 2500000000 | AR1 | AL1 | 11 |
| S2 | Blinding Lights | 3:20 | {Synth-pop, R&B} | songs/s2/ | 4000000000 | AR1 | AL1 | 9 |
| S3 | Save Your Tears | 3:35 | {Pop, R&B} | songs/s3/ | 3000000000 | AR1 | AL1 | 5 |
| S4 | Levitating | 3:23 | {Disco, Pop} | songs/s4/ | 2000000000 | AR2 | AL2 | 5 |
| S5 | Don't Start Now | 3:03 | {Disco, Pop} | songs/s5/ | 2800000000 | AR2 | AL2 | 3 |
| S6 | Peaches | 2:21 | {Pop, R&B} | songs/s6/ | 1800000000 | AR3 | AL3 | 3 |
| S7 | Stay | 3:18 | {Pop} | songs/s7/ | 2200000000 | AR3 | AL3 | 5 |
| S8 | Montero | 2:17 | {Hip-Hop, Pop} | songs/s8/ | 1500000000 | AR4 | AL4 | 1 |

**Primary Key:** `SongID`
**Foreign Keys:** `ArtistID` → Artists, `AlbumID` → Albums

**What we fixed:**
- Artist info stored exactly once. Updating The Weeknd's monthly listeners is a single-row change.
- Album info stored exactly once. Updating album art is a single-row change.
- No non-key column depends on another non-key column.

---

## Step 5: Verify All Determinants Are Candidate Keys → BCNF

**Rule:** For every functional dependency `X → Y`, `X` must be a superkey.

### Artists Table
- `ArtistID → ArtistName, ArtistBio, ArtistImage, MonthlyListeners, Verified`
- `ArtistID` is the primary key. ✓ BCNF satisfied.

### Albums Table
- `AlbumID → AlbumTitle, AlbumArt, ReleaseDate, ArtistID`
- `AlbumID` is the primary key. ✓ BCNF satisfied.

### Songs Table
- `SongID → SongTitle, Duration, Genres, AudioPath, PlayCount, ArtistID, AlbumID, TrackNumber`
- `SongID` is the primary key. ✓ BCNF satisfied.

All determinants are candidate keys. The schema is in BCNF.

---

## Final Schema

```sql
CREATE TABLE artists (
    artist_id       UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    bio             TEXT,
    image_url       VARCHAR(500),
    monthly_listeners INTEGER DEFAULT 0,
    verified        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE albums (
    album_id        UUID PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    art_url         VARCHAR(500),
    release_date    DATE,
    artist_id       UUID NOT NULL REFERENCES artists(artist_id),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE songs (
    song_id         UUID PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    artist_id       UUID NOT NULL REFERENCES artists(artist_id),
    album_id        UUID NOT NULL REFERENCES albums(album_id),
    duration_ms     INTEGER NOT NULL,
    genres          VARCHAR[] DEFAULT '{}',
    audio_path      VARCHAR(500) NOT NULL,
    play_count      BIGINT DEFAULT 0,
    track_number    INTEGER,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### Indexes (from the design doc)

```sql
CREATE INDEX idx_songs_artist ON songs(artist_id);
CREATE INDEX idx_songs_album ON songs(album_id);
CREATE INDEX idx_songs_genre ON songs USING GIN(genres);
CREATE INDEX idx_albums_artist ON albums(artist_id);
CREATE INDEX idx_albums_release ON albums(release_date DESC);
```

### Relationships

```
Artists ||--< Albums    (One artist has many albums)
Artists ||--< Songs     (One artist has many songs)
Albums  ||--< Songs    (One album contains many songs)
```

---

## Before and After Comparison

| Concern | Before (Unnormalized) | After (BCNF) |
|---------|----------------------|---------------|
| Update artist's monthly listeners | Change every row where that artist has a song (could be hundreds) | Update one row in Artists |
| Update album artwork | Change every song row on that album | Update one row in Albums |
| Add a new artist (no songs yet) | Impossible without fabricating song data | Insert into Artists alone |
| Add a new album (no songs yet) | Impossible without fabricating song data | Insert into Albums alone |
| Remove all songs from an album | Lose the album's metadata | Songs deleted; Albums row remains |
| Storage (100M songs, 10M artists, 20M albums) | ~100M rows × all columns = massive duplication | 100M song rows (lean) + 10M artist rows + 20M album rows |

---

## How This Fits the Larger Spotify Architecture

The PostgreSQL catalog is the **source of truth** for song metadata. Other systems derive from it:

| Downstream System | What It Gets | How |
|-------------------|-------------|-----|
| **Elasticsearch** | Denormalized search index (song + artist + album fields flattened) | Change Data Capture (CDC) pipeline |
| **Redis** | Hot song metadata for sub-ms lookups during playback authorization | Cache-aside pattern |
| **CDN** | Audio file paths resolved from `songs.audio_path` | Playback Service reads path, generates signed URL |
| **Recommendation ML Pipeline** | Song features (genre, duration, play count) for model training | Batch export |

The normalized PostgreSQL schema remains clean and anomaly-free, while downstream systems denormalize as needed for their specific access patterns.
