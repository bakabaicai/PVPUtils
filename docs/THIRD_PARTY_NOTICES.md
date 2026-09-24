# Third-Party Notices

**Last updated:** 2026-09-24  
**Aligned with:** `build.gradle` dependency set as of this date (includes `top.fpsmaster:music-api:0.1.1` / Cadence).

The PVPUtils Source-Available Non-Commercial License applies only to code, assets, and materials owned by Nachoneko_miao and PVPUtils contributors. Third-party components remain under their own licenses. This file is the project notice index; full license texts that are required to accompany distributions live under [`LICENSES/`](./LICENSES/).

| Path | Contents |
|---|---|
| [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) | **Full text** — Apache License 2.0 |
| [`LICENSES/MIT.txt`](./LICENSES/MIT.txt) | **Full text** — MIT template (per-component copyright lines in §1) |
| [`LICENSES/commons-io-NOTICE.txt`](./LICENSES/commons-io-NOTICE.txt) | Upstream NOTICE for Apache Commons IO |
| [`LICENSES/GPL-3.0-only.txt`](./LICENSES/GPL-3.0-only.txt) | **Full text** — GNU GPLv3 + PVPUtils component appendix |
| [`LICENSES/LGPL-2.1-or-later.txt`](./LICENSES/LGPL-2.1-or-later.txt) | **Full text** — GNU LGPLv2.1 + PVPUtils component appendix (sources, SPDX) |
| [`LICENSES/LGPL-3.0-or-later.txt`](./LICENSES/LGPL-3.0-or-later.txt) | **Full text** — GNU LGPLv3 + PVPUtils component appendix (incorporates GPLv3) |
| [`LICENSES/BSD-2-Clause-JCodec.txt`](./LICENSES/BSD-2-Clause-JCodec.txt) | **Full text** — BSD-2-Clause (JCodec “FreeBSD License”) + component list |
| [`SHADERS.md`](./SHADERS.md) | Per-shader license inventory |

All files under `LICENSES/` are UTF-8 (no BOM).

---

## 1. Permissively Licensed Components (MIT / Apache-2.0 / BSD)

Section title covers **permissive** licenses only. Each entry lists SPDX, packaging, source, license file location, and NOTICE status.

### 1.1 MIT

Permission notices for MIT components are the standard text in [`LICENSES/MIT.txt`](./LICENSES/MIT.txt). Each component’s required copyright line is below.

#### Cadence (`top.fpsmaster:music-api`)

| Field | Value |
|---|---|
| SPDX | `MIT` |
| Coordinate | `top.fpsmaster:music-api:0.1.1` |
| Bundled | Yes — jar-in-jar (`include` in `build.gradle`) |
| Source | https://github.com/FPSMasterTeam/Cadence |
| License text | [`LICENSES/MIT.txt`](./LICENSES/MIT.txt) |
| NOTICE file | None in artifact |
| Modified by PVPUtils | No (binary/composite-build dependency) |
| Copyright | Copyright (c) 2026 FPSMaster Team |
| Role | Netease Cloud Music (and QQ Music capability) data client: search, stream URL, lyrics, playlists, recommendations, QR login, credential store. Replaces the previously bundled Node.js NeteaseCloudMusicApi runtime. |

#### QRCodeGen (`io.nayuki:qrcodegen`)

| Field | Value |
|---|---|
| SPDX | `MIT` |
| Coordinate | `io.nayuki:qrcodegen:1.8.0` |
| Bundled | Yes — jar-in-jar |
| Source | https://www.nayuki.io/page/qr-code-generator-library · https://github.com/nayuki/QR-Code-generator |
| License text | [`LICENSES/MIT.txt`](./LICENSES/MIT.txt) |
| NOTICE file | None in artifact |
| Modified by PVPUtils | No |
| Copyright | Project Nayuki |
| Role | QR code image generation for Netease login |

#### OSHI (`com.github.oshi:oshi-core`)

| Field | Value |
|---|---|
| SPDX | `MIT` |
| Coordinate | `com.github.oshi:oshi-core:6.6.5` |
| Bundled | Yes — jar-in-jar |
| Source | https://github.com/oshi/oshi |
| License text | [`LICENSES/MIT.txt`](./LICENSES/MIT.txt) |
| NOTICE file | None in artifact |
| Modified by PVPUtils | No |
| Copyright | The OSHI Project contributors |
| Role | System / hardware information (HUD, diagnostics) |

#### MixinExtras (`io.github.llamalad7:mixinextras`)

| Field | Value |
|---|---|
| SPDX | `MIT` |
| Coordinate | `io.github.llamalad7:mixinextras-fabric:0.4.1`, `mixinextras-common:0.4.1` |
| Bundled | **No** — compile/annotation processor and Mod linking dependency only; **not** `include`d into the published jar |
| Source | https://github.com/LlamaLad7/MixinExtras |
| License text | [`LICENSES/MIT.txt`](./LICENSES/MIT.txt) |
| NOTICE file | None in artifact |
| Modified by PVPUtils | No |
| Copyright | LlamaLad7 |
| Role | Enhanced Mixin injection helpers |

### 1.2 Apache-2.0

Full license text: [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) (also: https://www.apache.org/licenses/LICENSE-2.0).

#### Skija / HumbleUI types

| Field | Value |
|---|---|
| SPDX | `Apache-2.0` |
| Coordinates | `io.github.humbleui:skija-shared:0.143.16`, `io.github.humbleui:skija-windows-x64:0.143.16`, `io.github.humbleui:types:0.2.0` |
| Bundled | Yes — jar-in-jar |
| Source | https://github.com/HumbleUI/Skija |
| License text | [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) |
| NOTICE file | None required/observed in these artifacts |
| Modified by PVPUtils | No |
| Copyright | HumbleUI / Skija contributors (see upstream) |
| Role | GPU / Skia UI rendering (ClickGUI, HUD, music screen) |

#### Gson (`com.google.code.gson:gson`)

| Field | Value |
|---|---|
| SPDX | `Apache-2.0` |
| Coordinate | `com.google.code.gson:gson:2.11.0` |
| Bundled | Yes — jar-in-jar |
| Source | https://github.com/google/gson |
| License text | [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) |
| NOTICE file | None observed in packaged jar |
| Modified by PVPUtils | No |
| Copyright | Google Inc. and Gson contributors |
| Role | JSON parsing (also used by Cadence) |

#### JNA (`net.java.dev.jna:jna`)

| Field | Value |
|---|---|
| SPDX | `Apache-2.0 OR LGPL-2.1-or-later` |
| Coordinate | `net.java.dev.jna:jna:5.14.0` |
| Bundled | Yes — jar-in-jar (`implementation include(...)`) |
| Source | https://github.com/java-native-access/jna |
| License text | Dual: [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) **or** LGPL-2.1-or-later (see [`LICENSES/LGPL-2.1-or-later.txt`](./LICENSES/LGPL-2.1-or-later.txt)); artifact `META-INF/LICENSE` states dual license |
| NOTICE file | None observed |
| Modified by PVPUtils | No |
| Election | PVPUtils exercises the **Apache-2.0** option for distribution of this dependency |
| Role | Native access (used by OSHI and platform helpers) |

#### Commons IO (`commons-io:commons-io`)

| Field | Value |
|---|---|
| SPDX | `Apache-2.0` |
| Coordinate | `commons-io:commons-io:2.20.0` |
| Bundled | Yes — jar-in-jar |
| Source | https://commons.apache.org/proper/commons-io/ · https://github.com/apache/commons-io |
| License text | [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) |
| NOTICE | **Yes** — upstream `META-INF/NOTICE.txt` copied to [`LICENSES/commons-io-NOTICE.txt`](./LICENSES/commons-io-NOTICE.txt) |
| Modified by PVPUtils | No |
| Copyright | Copyright 2002-2025 The Apache Software Foundation |
| Role | File IO helpers |

#### NoSneakAnim (based-on project, not jar-bundled)

| Field | Value |
|---|---|
| SPDX | `Apache-2.0` |
| Bundled | **No** — only adapted implementation inside PVPUtils sources |
| Source | https://sylveon.pet/cyan/nosneakanim/src/branch/main |
| License text | [`LICENSES/Apache-2.0.txt`](./LICENSES/Apache-2.0.txt) |
| NOTICE file | None published with upstream; this section is the attribution notice |
| Copyright | chromonym (NoSneakAnim) |
| Relation | PVPUtils “Sneak Animation” feature is based on NoSneakAnim ideas and implementation details (camera sneak transition handling; client-side eye-height adjustment while sneaking). |

**Modifications made in PVPUtils (Apache-2.0 §4(b) notice):**

- Ported into Mojang-mapped Fabric Mixins under `com.pvp_utils.mixin.client` rather than upstream project structure.
- Primary files: `CameraMixin.java` (sneak pose tracking, eye-height / animation-speed modification, interaction with Motion Camera and Freelook) and `LocalPlayerEyeHeightMixin.java` (`Entity.getEyeHeight` hooks with `Config.sneakDropScale` / `Config.noSneakAnimation`).
- Wired into PVPUtils configuration (`Config.noSneakAnimation`, `sneakDropScale`, `sneakAnimationSpeed`), ResetManager, Settings UI, ClickGUI `RenderPage`, WebGUI module list, and Arraylist HUD.
- Behavior extended with configurable drop scale and animation speed (including instant sneak when speed ≥ 1).
- Unrelated camera features (Freelook, Motion Camera) share `CameraMixin` but are separate PVPUtils functionality.

### 1.3 BSD / FreeBSD

#### JCodec (`org.jcodec:jcodec`)

| Field | Value |
|---|---|
| SPDX | `BSD-2-Clause` (upstream markets as “FreeBSD License”; Maven POM name: FreeBSD) |
| Coordinates | `org.jcodec:jcodec:0.2.5`, `org.jcodec:jcodec-javase:0.2.5` |
| Bundled | Yes — jar-in-jar |
| Source | https://github.com/jcodec/jcodec · http://jcodec.org/lic.html |
| License text | [`LICENSES/BSD-2-Clause-JCodec.txt`](./LICENSES/BSD-2-Clause-JCodec.txt) (**full** BSD-2-Clause text + component list; upstream page: http://jcodec.org/lic.html) |
| NOTICE file | None in artifact |
| Modified by PVPUtils | No |
| Copyright | JCodec contributors (see source headers) |
| Role | Audio/video codec helpers |

---

## 2. Weak Copyleft (LGPL) — Jar-in-Jar

**These libraries are packaged inside the published PVPUtils jar** via Loom `include`. Obligations are not satisfied by “see upstream” alone.

### 2.1 How PVPUtils treats LGPL components

| Topic | Statement |
|---|---|
| Form | Unmodified binary artifacts from Maven Central / declared repositories (not patched in-tree) |
| Linking | Java classpath / jar-in-jar loading at runtime (separate compilation units; not native static linking) |
| Modification | **PVPUtils does not modify** the LGPL library sources listed in this section |
| Source availability | Corresponding source is available from the upstream URLs and Maven Central source JARs listed per component |
| Replace / relink | See §2.4 — nested-jar replacement procedure, rebuild instructions, and source coordinates |
| License copies | Full LGPL texts: [`LICENSES/LGPL-2.1-or-later.txt`](./LICENSES/LGPL-2.1-or-later.txt), [`LICENSES/LGPL-3.0-or-later.txt`](./LICENSES/LGPL-3.0-or-later.txt) |
| Notices | Copyright and license headers inside the nested jars are retained; this document maps them |

### 2.2 Component matrix (LGPL)

| Component | SPDX | Coordinate | Bundled | Source | License file | NOTICE | Modified | Copyright / notes |
|---|---|---|---|---|---|---|---|---|
| MP3SPI | `LGPL-2.1-or-later` | `com.googlecode.soundlibs:mp3spi:1.9.5.4` | Yes | https://github.com/pdudits/soundlibs · https://github.com/umjammer/mp3spi | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | JavaZoom / soundlibs packagers; javax.sound SPI for MP3 |
| JLayer | `LGPL-2.1-or-later` | `com.googlecode.soundlibs:jlayer:1.0.1.4` | Yes | https://github.com/pdudits/soundlibs · upstream JavaZoom | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | MP3 decoder; source notes “moved to LGPL” |
| Tritonus (all) | `LGPL-2.1-or-later` | `com.googlecode.soundlibs:tritonus-all:0.3.7.2` | Yes | https://github.com/pdudits/soundlibs · http://www.tritonus.org | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | Sound SPI infrastructure; parent soundlibs POM = LGPL 2.1 |
| JOrbis | `LGPL-2.1-or-later` | `com.googlecode.soundlibs:jorbis:0.0.17-2` | Yes | https://github.com/pdudits/soundlibs · http://www.jcraft.com/jorbis/ | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | Ogg Vorbis decoder |
| JFLAC | `LGPL-2.1-or-later` | `org.jflac:jflac-codec:1.5.2` | Yes | https://github.com/nguillaumin/jflac | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | Parent POM `jflac-parent:1.5.2` declares **The GNU Lesser General Public License, Version 2.1**; source headers use LGPL v2+ |
| JAudioTagger | `LGPL-2.1-or-later` | `com.github.goxr3plus:jaudiotagger:2.2.7` | Yes | Upstream: https://bitbucket.org/ijabz/jaudiotagger · https://github.com/ijabz/jaudiotagger · Coordinate repo: https://github.com/goxr3plus/jaudiotagger | `LICENSES/LGPL-2.1-or-later.txt` | None in jar | No | **POM for `goxr3plus:jaudiotagger:2.2.7` declares `<name>LGPL</name>`** (gnu.org/copyleft/lesser.html), confirmed from Gradle module cache. Upstream jthink/ijabz is LGPL-2.1-or-later |
| vorbis-support | `LGPL-3.0-or-later` | `com.github.trilarion:vorbis-support:1.1.0` | Yes | https://github.com/Trilarion/java-vorbis-support | `LICENSES/LGPL-3.0-or-later.txt` | **Yes** — `META-INF/license.txt` inside artifact | No | POM: GNU LIBRARY GENERAL PUBLIC LICENSE, Version 3.0; combines JOrbis / VorbisSPI / Tritonus-Share (LGPLv2+ bases) |

### 2.3 Source acquisition (LGPL)

For each component, corresponding source is available as:

| Method | How |
|---|---|
| Maven Central sources JAR | e.g. `https://repo1.maven.org/maven2/<group>/<artifact>/<version>/<artifact>-<version>-sources.jar` |
| Upstream VCS | Clone **Source** URL at the release/tag matching the Maven version |
| Rebuild this project | `./gradlew build` from a full checkout with sibling `../Cadence` (composite build); nested jars are resolved by Gradle/Loom from the coordinates in `build.gradle` |

PVPUtils does **not** modify these LGPL libraries (unmodified published artifacts).

### 2.4 Replacing nested LGPL jars (practical steps)

Loom jar-in-jar nests libraries under the published mod jar. PVPUtils does not patch them, so substitution is possible:

1. **Locate nested jar** — open the published PVPUtils jar; nested artifacts appear under the Fabric Loom / `jij` layout (or the nested path Loom writes for `include`d coordinates).
2. **Replace with same API** — put a different compatible build of the same Maven coordinate (or an interface-compatible LGPL build) in place of the nested file, preserving the nested filename Loom expects.
3. **Rebuild from source instead** — preferred path:
   - Clone PVPUtils
   - Place Cadence at `../Cadence` (see `settings.gradle` `includeBuild`)
   - Run `./gradlew build`
   - Dependency versions are pinned in `build.gradle`; change a coordinate there to upgrade/replace an LGPL library
4. **Obtain library source** — use the `-sources.jar` coordinates in §2.3; PVPUtils ships no modified object code for these libraries.

If a future change **modifies** an LGPL library, PVPUtils must then offer that library’s corresponding source under the LGPL terms; that situation does not apply today.

---

## 3. Strong Copyleft (GPL) — Jar-in-Jar — BLOCKING CONFLICT

> **Release blocker.** `java-stream-player` is **GPL-3.0-only** and **jar-in-jar bundled**.  
> PVPUtils is distributed under the **PVPUtils Source-Available Non-Commercial License**, which imposes a **non-commercial** restriction.  
> **GPL-3.0 section 10 forbids additional restrictions** on recipients’ exercise of GPL rights (including commercial use).  
> Shipping this component inside the published jar **while keeping only the non-commercial license on the combination is not a compliant resolution**.  
> This is a **hard license conflict**, not a documentation footnote.

| Component | SPDX | Coordinate | Bundled | Source | License full text | NOTICE | Modified | Notes |
|---|---|---|---|---|---|---|---|---|
| java-stream-player | `GPL-3.0-only` | `com.github.goxr3plus:java-stream-player:10.0.2` | **Yes** — `include` in `build.gradle` | https://github.com/goxr3plus/java-stream-player | [`LICENSES/GPL-3.0-only.txt`](./LICENSES/GPL-3.0-only.txt) (**full** GPLv3 + appendix) | None in jar | **No** | POM declares GNU GPL v3.0; used by `MusicPlaybackService` (`com.goxr3plus.streamplayer.*`) |

### 3.1 Origin of this dependency

- **Not introduced by the Cadence migration.** Present on `origin/main` before this change set.
- Added in commit `d9d50ed` (“添加内置网易云音乐播放器//TODO”, 2026-07-17, baka_baicai) together with the built-in Netease player.
- This change set only adds Cadence / Gson / qrcodegen; it does not add `java-stream-player`.

### 3.2 Corresponding source for the GPL component

- Repository: https://github.com/goxr3plus/java-stream-player  
- Version: `10.0.2` (Maven coordinate `com.github.goxr3plus:java-stream-player:10.0.2`)  
- Sources JAR present in local Gradle cache: `java-stream-player-10.0.2-sources.jar`  
- PVPUtils does not modify the library.

### 3.3 Required resolution (choose one before release)

| Option | Action | Compatible with non-commercial PVPUtils license? |
|---|---|---|
| **A (preferred if NC license must stay)** | **Remove** `java-stream-player` from `include` (and ideally from `implementation`); replace playback with an Apache-2.0 / MIT / LGPL / BSD library, or a small original player on `javax.sound.sampled` + existing SPI stack | Yes |
| **B (not recommended)** | Ship as optional user-installed dependency only | **Still risky** — Java classpath linking often still creates a combined work under GPL; not a clean fix |
| **C** | Relicense the **entire** PVPUtils distribution as `GPL-3.0-only` (or GPL-compatible) and **drop** the non-commercial restriction | Yes for GPL, **but contradicts** the current non-commercial license direction |

**Until Option A or C is implemented, do not treat this document as certifying a conflict-free release.**

### 3.4 Why “document the risk” is not enough

GPL-3.0 §10: recipients may not be given further restrictions. A non-commercial clause on the combined jar-in-jar distribution is such a further restriction for the GPL-covered component’s effective distribution context. FSF guidance treats aggregation vs. combination carefully; Loom `include` nests the library **inside** the mod artifact as a single distributed unit, which is closer to combination than mere aggregation on a volume of storage. **Legal risk is on the distributor of PVPUtils builds.**

---

## 4. Full runtime dependency audit (license-relevant)

Generated from `./gradlew dependencies --configuration clientRuntimeClasspath` / `runtimeClasspath` on 2026-09-24.

### Jar-in-jar (`include` — redistributed inside PVPUtils)

| Coordinate | SPDX | Path |
|---|---|---|
| `top.fpsmaster:music-api:0.1.1` → `:Cadence` | MIT | direct |
| `com.google.code.gson:gson` (declared 2.11.0, may resolve newer) | Apache-2.0 | direct + Cadence |
| `io.nayuki:qrcodegen:1.8.0` | MIT | direct |
| `io.github.humbleui:types:0.2.0` | Apache-2.0 | direct |
| `io.github.humbleui:skija-shared:0.143.16` | Apache-2.0 | direct |
| `io.github.humbleui:skija-windows-x64:0.143.16` | Apache-2.0 | direct |
| `net.java.dev.jna:jna` | Apache-2.0 OR LGPL-2.1+ (elected Apache-2.0) | direct |
| `com.github.oshi:oshi-core` | MIT | direct |
| `org.jcodec:jcodec:0.2.5` | BSD-2-Clause | direct |
| `org.jcodec:jcodec-javase:0.2.5` | BSD-2-Clause | direct |
| `com.github.goxr3plus:java-stream-player:10.0.2` | **GPL-3.0-only** | direct — **conflict §3** |
| `com.googlecode.soundlibs:mp3spi:1.9.5.4` | LGPL-2.1-or-later | direct + transitive of stream-player |
| `com.googlecode.soundlibs:jlayer:1.0.1.4` | LGPL-2.1-or-later | direct + transitive |
| `com.googlecode.soundlibs:tritonus-all:0.3.7.2` | LGPL-2.1-or-later | direct + transitive |
| `com.googlecode.soundlibs:jorbis:0.0.17-2` | LGPL-2.1-or-later | direct + transitive |
| `org.jflac:jflac-codec:1.5.2` | LGPL-2.1-or-later | direct + transitive |
| `com.github.trilarion:vorbis-support:1.1.0` | LGPL-3.0-or-later | direct + transitive |
| `com.github.goxr3plus:jaudiotagger:2.2.7` | LGPL-2.1-or-later | direct + transitive |
| `commons-io:commons-io:2.20.0` | Apache-2.0 | direct + transitive |

Transitive notes observed on the runtime classpath but **not** separately `include`d by PVPUtils `build.gradle` (may still be present on the **game/loader** classpath, not nested by this mod’s `include` list): `kotlin-stdlib` (Apache-2.0, via Cadence), `org.jetbrains:annotations`, `error_prone_annotations`, `slf4j-api`, `junit` (test scope of jlayer — not an `include`), ASM / Mixin / Minecraft / Fabric / Netty / Log4j / Guava / etc. (loader or Minecraft environment — see §5).

### Not jar-in-jar by PVPUtils

| Coordinate | Why |
|---|---|
| `io.github.llamalad7:mixinextras-*` | `implementation` / `annotationProcessor` only — **no** `include` |
| `net.fabricmc:fabric-loader`, `fabric-api` | `modImplementation` — user environment |
| Minecraft / Mojang libraries | Game environment |

---

## 5. Runtime Environment (not bundled by PVPUtils)

These are **not** jar-in-jar `include`d by PVPUtils. They are provided by the player’s Minecraft / Fabric environment (installed separately).

| Component | How provided | License |
|---|---|---|
| Minecraft | User-installed game (Mojang) | Mojang EULA / proprietary — not redistributed by PVPUtils |
| Fabric Loader | User-installed loader (`modImplementation`) | Apache-2.0 (Fabric Loader) |
| Fabric API | User-installed **mod** (`modImplementation`); not shipped inside the vanilla game and not nested in the PVPUtils jar | Apache-2.0 |
| Mojang official mappings | Build-time via Loom | Mojang / project terms as applicable |

PVPUtils does not re-license these components. “Not bundled” means end users must obtain Fabric Loader / Fabric API themselves; the game environment does not magically supply them.

---

## 6. Shaders

GLSL shaders under `src/client/resources/shaders/` keep their own licenses (including CC0, WTFPL, MIT, CC-BY-3.0, CC-BY-NC-SA-3.0, CC-BY-NC-SA-4.0, MIT OR CC-BY-NC-4.0, and entries marked Unknown).

**Authoritative inventory:** [`SHADERS.md`](./SHADERS.md) — per-file table, fragment attributions for composite shaders, risk flags for Unknown attributions, and maintenance rules.

Do not rely solely on file headers for review; use `SHADERS.md` as the checklist. Snippets with unknown license are high risk and should be replaced or cleared before any change in distribution status.

---

## 7. Online Music APIs, trademarks, and credentials

### 7.1 Services used

PVPUtils (via Cadence) requests data from:

- **Netease Cloud Music** (music.163.com and related endpoints)
- **QQ Music** (available in Cadence; may not be exposed in the current PVPUtils UI)

### 7.2 Trademarks

**Netease Cloud Music**, **网易云音乐**, **QQ Music**, **QQ音乐**, and related marks are trademarks or trade names of their respective owners (NetEase, Tencent, and affiliates). PVPUtils is not affiliated with, endorsed by, or sponsored by those companies. Use of those names is only for identification of the third-party services accessed.

### 7.3 Credentials and session storage

| Topic | Statement |
|---|---|
| Location | Local file under the game directory, e.g. `PVPUtils/netease-session.json` (QR / cookie session material) |
| Sensitivity | May contain authentication cookies/tokens for the music service — treat as a secret |
| User control | The user may delete this file at any time; in-game logout clears the stored session via Cadence |
| Upload | PVPUtils does **not** upload this file to PVPUtils-operated servers. Requests that use the session go only to the music service endpoints themselves (and, for normal API traffic, as required by those endpoints) |
| Telemetry | No PVPUtils telemetry channel is used to exfiltrate music credentials |

### 7.4 Terms of service

No official partnership is claimed. Users are responsible for service terms, rate limits, and regional restrictions.

---

## 8. Preservation of notices

If you redistribute PVPUtils or a Derivative Work, you must:

1. Keep this file (or an equivalent NOTICE index),
2. Keep [`LICENSES/`](./LICENSES/) and [`SHADERS.md`](./SHADERS.md),
3. Keep all MIT / Apache-2.0 / LGPL / GPL copyright and license headers required by those licenses (including upstream NOTICE content such as Commons IO),
4. Keep the project [LICENSE](../LICENSE).

Removing or hiding third-party attribution is not permitted. For LGPL/GPL components, do not strip nested license files from jar-in-jar artifacts.

---

## 9. Summary table (quick reference)

| Component | SPDX | Bundled (jij) | License full text | NOTICE |
|---|---|---|---|---|
| Cadence `music-api` | MIT | Yes | `LICENSES/MIT.txt` + © line §1.1 | — |
| qrcodegen | MIT | Yes | `LICENSES/MIT.txt` + © line §1.1 | — |
| oshi-core | MIT | Yes | `LICENSES/MIT.txt` + © line §1.1 | — |
| MixinExtras | MIT | No | `LICENSES/MIT.txt` + © line §1.1 | — |
| Skija / types | Apache-2.0 | Yes | `LICENSES/Apache-2.0.txt` | — |
| Gson | Apache-2.0 | Yes | `LICENSES/Apache-2.0.txt` | — |
| JNA | Apache-2.0 OR LGPL-2.1+ | Yes | both under `LICENSES/` | — |
| Commons IO | Apache-2.0 | Yes | `LICENSES/Apache-2.0.txt` | `LICENSES/commons-io-NOTICE.txt` |
| JCodec | BSD-2-Clause | Yes | `LICENSES/BSD-2-Clause-JCodec.txt` | — |
| mp3spi / jlayer / tritonus-all / jorbis | LGPL-2.1-or-later | Yes | `LICENSES/LGPL-2.1-or-later.txt` | — |
| jflac-codec | LGPL-2.1-or-later | Yes | `LICENSES/LGPL-2.1-or-later.txt` | — |
| jaudiotagger | LGPL-2.1-or-later | Yes | `LICENSES/LGPL-2.1-or-later.txt` (full) | — |
| vorbis-support | LGPL-3.0-or-later | Yes | `LICENSES/LGPL-3.0-or-later.txt` (full) | in jar `META-INF/license.txt` |
| java-stream-player | GPL-3.0-only | **Yes — conflict §3** | `LICENSES/GPL-3.0-only.txt` (full) | — |
| NoSneakAnim (adapted) | Apache-2.0 | No (source only) | `LICENSES/Apache-2.0.txt` | §1.2 attribution |
| Shaders | mixed | resources | per file + `SHADERS.md` | `SHADERS.md` |
| Fabric Loader / Fabric API | Apache-2.0 | **No** (user environment) | upstream | — |
