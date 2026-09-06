# Modules

Every module Bean Client registers, generated from the registry itself.

**None of them do anything.** Bean Client is a GUI shell — each row below is a
placeholder whose toggle flips a boolean, writes `[shell] <name> -> ON` to the
log, and changes no game behaviour. The settings are real in the sense that they
hold a value, persist to disk and drive the widgets; they are simply not wired to
anything. See [README.md](README.md#adding-a-module) for how to attach behaviour.

There are **36 modules** across 6 categories.

| Category | Modules | Settings |
| --- | ---: | ---: |
| [Combat](#combat) | 6 | 14 |
| [Movement](#movement) | 6 | 11 |
| [Visual](#visual) | 6 | 14 |
| [Player](#player) | 6 | 7 |
| [World](#world) | 6 | 11 |
| [Misc](#misc) | 6 | 8 |
| **Total** | **36** | **65** |

---

## Combat

| Module | Config key | Settings |
| --- | --- | --- |
| **Aimbot** | `module.aimbot` | `Range` slider 1–6, default 3.0<br>`FOV` slider 10–180, default 90<br>`Target` mode: Closest / Lowest health / Angle<br>`Through walls` toggle, default off |
| **Killaura** | `module.killaura` | `CPS` slider 1–20, default 8<br>`Sort` mode: Distance / Health / Armour<br>`Players only` toggle, default on |
| **Auto Crystal** | `module.auto_crystal` | `Place range` slider 1–6, default 4.5<br>`Min damage` slider 0–20, default 6<br>`Anti-suicide` toggle, default on |
| **Silent Aim** | `module.silent_aim` | `Smoothing` slider 0–1, default 0.40<br>`Only while attacking` toggle, default on |
| **Hitflick** | `module.hitflick` | `Flick angle` slider 0–180, default 35 |
| **Reach Display** | `module.reach_display` | `Units` mode: Blocks / Metres |

## Movement

| Module | Config key | Settings |
| --- | --- | --- |
| **Sprint** | `module.sprint` | `Keep while sneaking` toggle, default off<br>`Mode` mode: Legit / Always |
| **Speed** | `module.speed` | `Multiplier` slider 1–3, default 1.40<br>`Mode` mode: Vanilla / Strafe / Bhop |
| **No Slow** | `module.no_slow` | `Eating` toggle, default on<br>`Shields` toggle, default on<br>`Cobwebs` toggle, default off |
| **Step** | `module.step` | `Height` slider 0.5–2.5, default 1.0 |
| **Velocity** | `module.velocity` | `Horizontal` slider 0–100, default 0<br>`Vertical` slider 0–100, default 0 |
| **Jesus** | `module.jesus` | `Mode` mode: Solid / Dolphin |

## Visual

| Module | Config key | Settings |
| --- | --- | --- |
| **Fullbright** | `module.fullbright` | `Gamma` slider 1–20, default 15 |
| **ESP** | `module.esp` | `Shape` mode: Box / Outline / Glow<br>`Players` toggle, default on<br>`Mobs` toggle, default off<br>`Items` toggle, default off |
| **Chams** | `module.chams` | `Opacity` slider 0–1, default 0.60 |
| **Nametags** | `module.nametags` | `Scale` slider 0.5–3, default 1.0<br>`Show health` toggle, default on<br>`Show ping` toggle, default on |
| **HUD** | `module.hud` | `Watermark` toggle, default on<br>`Module list` toggle, default on<br>`Corner` mode: Top left / Top right / Bottom left / Bottom right |
| **Zoom** | `module.zoom` | `Factor` slider 1–10, default 4.0<br>`Smooth camera` toggle, default on |

## Player

| Module | Config key | Settings |
| --- | --- | --- |
| **Auto Tool** | `module.auto_tool` | `Avoid breaking tools` toggle, default on |
| **Auto Eat** | `module.auto_eat` | `Hunger threshold` slider 1–20, default 12 |
| **Fast Place** | `module.fast_place` | `Delay` slider 0–10, default 2 |
| **Inventory Manager** | `module.inventory_manager` | `Auto sort` toggle, default on<br>`Drop junk` toggle, default off |
| **No Fall** | `module.no_fall` | `Mode` mode: Packet / Edit / Motion |
| **Freecam** | `module.freecam` | `Speed` slider 0.2–5, default 1.0 |

## World

| Module | Config key | Settings |
| --- | --- | --- |
| **Chest ESP** | `module.chest_esp` | `Range` slider 8–128, default 32<br>`Trapped chests` toggle, default on<br>`Barrels` toggle, default on |
| **Nuker** | `module.nuker` | `Radius` slider 1–8, default 4<br>`Order` mode: Nearest / Top down |
| **Scaffold** | `module.scaffold` | `Tower` toggle, default on<br>`Rotation` mode: None / Snap / Smooth |
| **Auto Farm** | `module.auto_farm` | `Range` slider 1–6, default 4.5<br>`Replant` toggle, default on |
| **Xray** | `module.xray` | `Opacity` slider 0–1, default 0.25 |
| **Terrain Blend** | `module.terrain_blend` | `Blend` mode: Off / Soft / Hard |

## Misc

| Module | Config key | Settings |
| --- | --- | --- |
| **Auto GG** | `module.auto_gg` | `Delay` slider 0–3000, default 500 |
| **Discord RPC** | `module.discord_rpc` | `Show server` toggle, default off<br>`Show elapsed time` toggle, default on |
| **Announcer** | `module.announcer` | `Voice` mode: Classic / Robot / Off |
| **Client Chat** | `module.client_chat` | `Prefix messages` toggle, default on |
| **FPS Limiter** | `module.fps_limiter` | `Unfocused cap` slider 5–260, default 30 |
| **Name Protect** | `module.name_protect` | `Hide own name` toggle, default on<br>`Hide others` toggle, default off |

---

## Themes tab

The seventh rail tab is not a module list. It holds the theme dropdown, the live
accent colour picker, and the Reset / Save file / Reload buttons that connect the
GUI to your `config/beanclient/themes/` folder. See
[README.md](README.md#writing-a-theme).

## Where the values live

Toggles persist to `config/beanclient/config.json` under `module.<id>`, and each
setting under `setting.<module id>.<setting id>`. Ids are the lowercased name with
non-alphanumerics collapsed to `_`, so *Auto Crystal* is `auto_crystal` and its
*Min damage* slider is `setting.auto_crystal.min_damage`.
