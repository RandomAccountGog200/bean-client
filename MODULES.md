# Modules

Every module Bean Client registers, generated from the registry itself.

**Every one of them works.** There is no placeholder list - if a row is in the
menu, toggling it changes something.

They all share one property: each reads state the vanilla client already has and
draws it on your own screen, or flips a vanilla option. Nothing is sent to the
server, nothing is automated on your behalf, and nothing reveals what the game did
not already send you. That is why there is no Combat tab.

There are **20 modules** across 4 categories.

| Category | Modules | Settings |
| --- | ---: | ---: |
| [HUD](#hud) | 13 | 2 |
| [Visual](#visual) | 2 | 2 |
| [SMP](#smp) | 3 | 2 |
| [Misc](#misc) | 2 | 1 |
| **Total** | **20** | **7** |

---

## HUD

| Module | What it does | Settings |
| --- | --- | --- |
| **Watermark**<br>`module.watermark` | The Bean Client mark in the corner. | _none_ |
| **Module List**<br>`module.module_list` | Lists every module you have switched on. | `Corner` mode: Top right / Top left |
| **FPS Display**<br>`module.fps_display` | Your current frame rate. | _none_ |
| **Coordinates**<br>`module.coordinates` | Your position, and the matching Nether coordinates. | `Nether conversion` toggle, default on |
| **Ping Display**<br>`module.ping_display` | Your latency to the server. | _none_ |
| **CPS Counter**<br>`module.cps_counter` | Clicks per second, left and right. Counts clicks you made - it never makes one. | _none_ |
| **Speedometer**<br>`module.speedometer` | How fast you are actually moving, in blocks per second. | _none_ |
| **Clock**<br>`module.clock` | The real-world time, so you know when to stop. | _none_ |
| **Session Timer**<br>`module.session_timer` | How long this session has been running. | _none_ |
| **Keystrokes**<br>`module.keystrokes` | WASD, the mouse buttons and jump, lit while held. | _none_ |
| **Armour HUD**<br>`module.armour_hud` | Your armour and held item with durability remaining. | _none_ |
| **Effects HUD**<br>`module.effects_hud` | Your active potion effects and how long they have left. | _none_ |
| **Server Info**<br>`module.server_info` | Which server you are on and how many players are online. | _none_ |

## Visual

| Module | What it does | Settings |
| --- | --- | --- |
| **Brightness**<br>`module.brightness` | The vanilla Brightness slider on a toggle. Tops out where the game does, and restores your own value when switched off. | `Level` slider 0–1, default 1.00 |
| **Zoom**<br>`module.zoom` | Hold C to narrow your FOV, by moving the vanilla FOV option. Floors at the game's own minimum of 30. | `Factor` slider 1–4, default 2.0 |

## SMP

| Module | What it does | Settings |
| --- | --- | --- |
| **Playtime Tracker**<br>`module.playtime_tracker` | Counts how long you have spent on each server, and remembers it. | _none_ |
| **Death Coords**<br>`module.death_coords` | Records where you died and prints it to your own chat. | _none_ |
| **Chat Filter**<br>`module.chat_filter` | Hides chat you have already seen. A display filter - nothing is sent back. | `Hide duplicates` toggle, default on<br>`Hide links` toggle, default off |

## Misc

| Module | What it does | Settings |
| --- | --- | --- |
| **FPS Limiter**<br>`module.fps_limiter` | Caps your frame rate, and puts the vanilla setting back when you switch it off. | `Limit` slider 10–260, default 60 |
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
