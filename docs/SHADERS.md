# Shader License Inventory

Location: `src/client/resources/shaders/`

File headers are authoritative where present. This inventory lists license information known at the time of writing so reviewers do not have to open every file.

**Risk legend:** `OK` — license identified; `Review` — license partial / mixed; `Unknown` — license or author not confirmed (treat as high risk; replace or obtain permission before commercial redistribution).

---

## Per-file inventory

| File | License (SPDX / short) | Author / source | Risk | Notes |
|---|---|---|---|---|
| `BasewarpFBM.frag.glsl` | Unspecified | References iquilezles.org/articles/warp | Unknown | No license header in file |
| `BlackHole.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `BlueGrid.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `BlueLandscape.frag.glsl` | Unspecified (Shadertoy) | https://www.shadertoy.com/view/NsS3Dt | Unknown | Forked; Shadertoy default terms apply unless stated |
| `Circuits.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `CubeCave.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `CyberFuji2020.frag.glsl` | CC-BY-3.0 | Jan Mróz (jaszunio15) | OK | Header: "Shader License: CC BY 3.0" |
| `DefaultVertex.vert.glsl` | Unspecified (project-local) | PVPUtils | Review | Shared vertex shader; confirm if original |
| `Galaxy.frag.glsl` | Mixed (see below) | Multiple | Review | Composition of several Shadertoy snippets |
| `GreenNebula.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `GridCave.frag.glsl` | Unspecified (Shadertoy) | https://www.shadertoy.com/view/fd23zz | Unknown | Original Shadertoy block retained |
| `Matrix.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `Minecraft.frag.glsl` | CC-BY-NC-SA-4.0 | Necip (Shadertoy MdlGz4); original code Markus Persson (notch) | Review | NonCommercial + ShareAlike |
| `NeonwaveSunrise.frag.glsl` | Mixed (see below) | Multiple | Review / Unknown | Contains Unknown snippets |
| `Planet.frag.glsl` | Unspecified (multiple Shadertoy IDs) | Multiple | Unknown | Several Shadertoy sources listed in file |
| `PurpleGrid.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `RectWaves.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `RedLandscape.frag.glsl` | Unspecified (Shadertoy) | https://www.shadertoy.com/view/NsS3Dt | Unknown | Same source as BlueLandscape |
| `Seascape.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `Shadertoy.frag.glsl` | Unspecified (emulation shell) | — | Review | Shadertoy emulation helper; confirm no third-party body |
| `Space.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `SquaresBackground.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `TheSea2d.frag.glsl` | Unspecified | — | Unknown | No license header in file |
| `Tube.frag.glsl` | Unspecified (Shadertoy) | https://www.shadertoy.com/view/wl2SRK | Unknown | Original Shadertoy block retained |

---

## `Galaxy.frag.glsl` fragment attributions (from file header)

| Fragment | License stated in header | Author | Source |
|---|---|---|---|
| Stars / galaxy | CC0 | (as stated) | header |
| Snippet | Unknown | nmz (@stormoid) | https://www.shadertoy.com/view/NdfyRM |
| Snippet | Unknown | Matt Taylor | https://64.github.io/tonemapping/ |
| Snippet | Unknown | Unknown | "don't remember" |
| Snippet | WTFPL | sam hocevar | https://stackoverflow.com/a/17897228/418488 |
| Snippet | MIT OR CC-BY-NC-4.0 | mercury | https://mercury.sexy/hg_sdf/ |
| Snippet | Unknown | Unknown | "don't remember" |
| Snippet | CC-BY-NC-SA-3.0 | Stephane Cuillerdier (Aiekick) | https://www.shadertoy.com/view/Mt3GW2 |
| Snippet | MIT | Inigo Quilez | https://www.shadertoy.com/view/XslGRr / iquilezles.org spherefunctions |

## `NeonwaveSunrise.frag.glsl` fragment attributions (from file header)

| Fragment | License stated in header | Author | Source |
|---|---|---|---|
| Snippet | WTFPL | sam hocevar | Stack Overflow 418488 |
| Snippets | Unknown | Unknown | "don't remember" (multiple) |
| Value noise | (link only) | Inigo Quilez | https://iquilezles.org/articles/morenoise |
| Snippet | MIT | Inigo Quilez | spherefunctions |
| Snippets | MIT OR CC-BY-NC-4.0 | mercury | hg_sdf |
| Snippet | Unknown | nmz (@stormoid) | Shadertoy NdfyRM |
| Snippet | Unknown | Matt Taylor | tonemapping |

---

## Known non-commercial / share-alike constraints

- **CC-BY-NC-SA-3.0 / CC-BY-NC-SA-4.0 / CC-BY-NC-4.0:** NonCommercial and/or ShareAlike terms may apply to redistributions that are commercial or that combine these snippets into a collective work. PVPUtils is distributed under a non-commercial source-available license; if that status changes, re-affect these shaders first. Each file must list **author, source URL, license version, and link** — see table above; incomplete rows are not cleared for release.
- **MIT OR CC-BY-NC-4.0:** The licensor offers a choice. PVPUtils **selects MIT** for these snippets when used under this project’s terms (permissive option). Record that election in the file header when touching the shader (e.g. `License: MIT (elected from MIT OR CC-BY-NC-4.0)`).
- **Unknown / "don't remember":** No reliable grant. **Release action required:** (1) replace with clearly licensed originals, (2) contact the author and keep written permission, or (3) remove the shader from the distribution. Do not ship Unknown-attribution snippets in a “cleared” build.

## Maintenance

When adding or editing a shader:

1. Put author, source URL, and SPDX (or short license name) in the file header.
2. Add or update a row in the table above.
3. Prefer CC0 / MIT / Apache-2.0 / BSD sources.
4. Do not add snippets marked Unknown without a recorded permission note.
