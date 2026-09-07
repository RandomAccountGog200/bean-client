# Modules

Every module Bean Client registers, generated from the registry itself by
`tools/gen_modules.py` — so this file cannot drift from the code.

**Every one of them works.** There is no placeholder list — if a row is in the
menu, toggling it changes something.

The set splits in two, and the split matters more than the tab names:

- **HUD, Visual, SMP and Misc** read state the vanilla client already has and draw
  it on your own screen, or move a vanilla option you could have moved yourself.
  Nothing is sent to the server and nothing is automated on your behalf.
- **Combat, Movement and Render** do none of that. They act for you, show you what
  the client had decided not to draw, and in several cases tell the server
  something untrue. Any server running an anticheat is looking for them.

There are still **no mixins**. Everything goes through a public API — an entity
attribute, a packet the client already sends, or a projection onto the HUD — which
is a real constraint on what these can do. Reach is the clearest case: it lengthens
the client raycast, and the server throws the result away.

There are **38 modules** across 7 categories.

| Category | Modules | Settings |
| --- | ---: | ---: |
| [HUD](#hud) | 13 | 2 |
| [Combat](#combat) | 7 | 18 |
| [Movement](#movement) | 6 | 5 |
| [Render](#render) | 5 | 13 |
| [Visual](#visual) | 2 | 2 |
| [SMP](#smp) | 3 | 2 |
| [Misc](#misc) | 2 | 1 |
| **Total** | **38** | **43** |

---

## HUD

_Readouts drawn on your own screen._

| Module | What it does | Settings |
| --- | --- | --- |
| **Watermark**<br>`module.watermark` | The Bean Client mark in the corner. | _none_ |
| **Module List**<br>`module.module_list` | Lists every module you have switched on. | `Corner` — mode: Top right / Top left |
| **FPS Display**<br>`module.fps_display` | Your current frame rate. | _none_ |
| **Coordinates**<br>`module.coordinates` | Your position, and the matching Nether coordinates. | `Nether conversion` — toggle, default true |
| **Ping Display**<br>`module.ping_display` | Your latency to the server. | _none_ |
| **CPS Counter**<br>`module.cps_counter` | Clicks per second, left and right. Counts clicks you made - it never makes one. | _none_ |
| **Speedometer**<br>`module.speedometer` | How fast you are actually moving, in blocks per second. | _none_ |
| **Clock**<br>`module.clock` | The real-world time, so you know when to stop. | _none_ |
| **Session Timer**<br>`module.session_timer` | How long this session has been running. | _none_ |
| **Keystrokes**<br>`module.keystrokes` | WASD, the mouse buttons and jump, lit while held. | _none_ |
| **Armour HUD**<br>`module.armour_hud` | Your armour and held item with durability remaining. | _none_ |
| **Effects HUD**<br>`module.effects_hud` | Your active potion effects and how long they have left. | _none_ |
| **Server Info**<br>`module.server_info` | Which server you are on and how many players are online. | _none_ |

## Combat

_Acts on your behalf. Every one of these is what an anticheat is looking for._

| Module | What it does | Settings |
| --- | --- | --- |
| **Killaura**<br>`module.killaura` | Attacks the nearest target on a timer. Rotation is real - your camera turns. | `Range` — slider 3–6, default 4.2<br>`Delay` — slider 0–1000, default 100<br>`Targets` — mode: Players / Mobs / All<br>`Rotate` — toggle, default true<br>`Wait for cooldown` — toggle, default true |
| **Trigger Bot**<br>`module.trigger_bot` | Attacks whatever your crosshair is already on. You still aim it. | `Delay` — slider 0–1000, default 150<br>`Targets` — mode: All / Players / Mobs |
| **Criticals**<br>`module.criticals` | Two small position packets before a swing, so the hit lands while the server has you airborne. | _none_ |
| **Auto Clicker**<br>`module.auto_clicker` | Clicks at a set rate while you hold the attack button down. | `CPS` — slider 1–20, default 10<br>`Jitter` — toggle, default true |
| **Reach**<br>`module.reach` | Lengthens the client interaction raycast. The server validates against its own copy, so past vanilla range the attack is simply dropped. | `Extra blocks` — slider 0–3, default 1.0 |
| **Crystal Aura**<br>`module.crystal_aura` | Places end crystals next to a target and breaks them. Scores every legal position by what the blast would do to them and to you, and acts on the best one that clears both thresholds. | `Range` — slider 3–6, default 4.5<br>`Target range` — slider 4–16, default 12<br>`Min damage` — slider 1–20, default 6<br>`Max self damage` — slider 0–20, default 8<br>`Delay` — slider 0–500, default 50<br>`Place` — toggle, default true<br>`Break` — toggle, default true |
| **Auto Totem**<br>`module.auto_totem` | Moves a totem to your off hand when your health drops, using ordinary container clicks. | `Health` — slider 1–20, default 10 |

## Movement

_Changes how you move, and what the server is told about it._

| Module | What it does | Settings |
| --- | --- | --- |
| **Sprint**<br>`module.sprint` | Sprints whenever you are walking forward. | _none_ |
| **Step**<br>`module.step` | Walks up a full block. Uses the vanilla step-height attribute, so the physics that follow are the ones vanilla would have run. | `Height` — slider 0.6–2.5, default 1.0 |
| **Fly**<br>`module.fly` | Zeroes gravity and drives your motion from the movement keys. | `Speed` — slider 0.1–2.0, default 0.5 |
| **Speed**<br>`module.speed` | Scales your horizontal motion after vanilla has computed it. | `Multiplier` — slider 1.0–3.0, default 1.4 |
| **Velocity**<br>`module.velocity` | Damps knockback. 0% takes all of it off that axis. | `Horizontal` — slider 0–100, default 0<br>`Vertical` — slider 0–100, default 100 |
| **No Fall**<br>`module.no_fall` | Claims to be on the ground while falling. Fall damage is the server sum, not yours. | _none_ |

## Render

_Draws what the client knew but had decided not to show you._

| Module | What it does | Settings |
| --- | --- | --- |
| **ESP**<br>`module.esp` | Boxes around entities, projected onto the HUD. | `Targets` — mode: Players / Mobs / All<br>`Range` — slider 8–128, default 64<br>`Fill` — toggle, default false |
| **Tracers**<br>`module.tracers` | Lines from the bottom of the screen to each target. | `Targets` — mode: Players / Mobs / All<br>`Range` — slider 8–128, default 64 |
| **Name Tags**<br>`module.name_tags` | Names, health and distance above every entity, through walls. | `Targets` — mode: All / Players / Mobs<br>`Range` — slider 8–128, default 48<br>`Show health` — toggle, default true<br>`Show distance` — toggle, default true |
| **Item ESP**<br>`module.item_esp` | Labels dropped items with their name and stack size. | `Range` — slider 8–64, default 32 |
| **Player Radar**<br>`module.player_radar` | A top-down radar, rotated so your facing is up. Shares the bottom-right corner with the Effects HUD. | `Targets` — mode: Players / Mobs / All<br>`Range` — slider 16–128, default 64<br>`Size` — slider 60–160, default 100 |

## Visual

_How your own client renders._

| Module | What it does | Settings |
| --- | --- | --- |
| **Brightness**<br>`module.brightness` | The vanilla Brightness slider on a toggle. Tops out where the game does, and restores your own value when switched off. | `Level` — slider 0.0–1.0, default 1.0 |
| **Zoom**<br>`module.zoom` | Hold C to narrow your FOV, by moving the vanilla FOV option. Floors at the game's own minimum of 30. | `Factor` — slider 1–4, default 2 |

## SMP

_Server quality of life._

| Module | What it does | Settings |
| --- | --- | --- |
| **Playtime Tracker**<br>`module.playtime_tracker` | Counts how long you have spent on each server, and remembers it. | _none_ |
| **Death Coords**<br>`module.death_coords` | Records where you died and prints it to your own chat. | _none_ |
| **Chat Filter**<br>`module.chat_filter` | Hides chat you have already seen. A display filter - nothing is sent back. | `Hide duplicates` — toggle, default true<br>`Hide links` — toggle, default false |

## Misc

_Everything else._

| Module | What it does | Settings |
| --- | --- | --- |
| **FPS Limiter**<br>`module.fps_limiter` | Caps your frame rate, and puts the vanilla setting back when you switch it off. | `Limit` — slider 10–260, default 60 |
| **Toggle Sounds**<br>`module.toggle_sounds` | Plays a click when you toggle a module, so you can feel the GUI respond. | _none_ |

---

## Themes tab

The last rail tab is not a module list. It holds the theme dropdown, the live
accent colour picker, and the Reset / Save file / Reload buttons that connect the
GUI to your `config/beanclient/themes/` folder. See
[README.md](README.md#writing-a-theme).

## Where the values live

Toggles persist to `config/beanclient/config.json` under `module.<id>`, and each
setting under `setting.<module id>.<setting id>`. Ids are the lowercased name with
non-alphanumerics collapsed to `_`, so *FPS Limiter* is `fps_limiter` and its
*Limit* slider is `setting.fps_limiter.limit`.

A module that borrows a vanilla video option also parks the displaced value under
`vanilla.<option>` while it holds it, so a session that never shuts down cleanly
can still hand your own setting back on the next launch. Attribute-based modules
need no such thing: transient modifiers are never serialised.
