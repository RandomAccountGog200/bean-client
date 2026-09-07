# Modules

Every module Bean Client registers, generated from the registry itself.

Modules marked **live** are wired to real behaviour and carry a dot next to their
name in the GUI. Everything else is a **placeholder**: its toggle flips a boolean,
writes `[shell] <name> -> ON` to the log, and changes no game behaviour. Their
settings still hold a value, persist to disk and drive the widgets - they are just
not wired to anything. See [README.md](README.md#adding-a-module) to attach behaviour.

Combat automation, movement exploits and anything that would reveal what the game
did not show you are placeholders and stay that way. They are labels in a menu.

There are **60 modules** across 7 categories, of which **6 are live**.

| Category | Modules | Live | Settings |
| --- | ---: | ---: | ---: |
| [Combat](#combat) | 18 | 0 | 36 |
| [Movement](#movement) | 6 | 0 | 11 |
| [Visual](#visual) | 9 | 4 | 12 |
| [Player](#player) | 6 | 0 | 7 |
| [World](#world) | 6 | 0 | 10 |
| [SMP](#smp) | 9 | 1 | 13 |
| [Misc](#misc) | 6 | 1 | 6 |
| **Total** | **60** | **6** | **95** |

---

## Combat

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **Aimbot** |  | `module.aimbot` | `Range` slider 1–6, default 3.0<br>`FOV` slider 10–180, default 90<br>`Target` mode: Closest / Lowest health / Angle<br>`Through walls` toggle, default off |
| **Killaura** |  | `module.killaura` | `CPS` slider 1–20, default 8<br>`Sort` mode: Distance / Health / Armour<br>`Players only` toggle, default on |
| **Auto Totem** |  | `module.auto_totem` | `Health threshold` slider 1–20, default 8<br>`Delay` slider 0–20, default 2<br>`Keep in offhand` toggle, default on<br>`Move mode` mode: Instant / Legit / Packet |
| **Auto Crystal** |  | `module.auto_crystal` | `Place range` slider 1–6, default 4.5<br>`Min damage` slider 0–20, default 6<br>`Anti-suicide` toggle, default on |
| **Auto Anchor** |  | `module.auto_anchor` | `Range` slider 1–6, default 4.5<br>`Auto glowstone` toggle, default on |
| **Auto Armour** |  | `module.auto_armour` | `Delay` slider 0–20, default 3<br>`Prefer enchanted` toggle, default on |
| **Offhand** |  | `module.offhand` | `Item` mode: Totem / Crystal / Gapple / Shield<br>`Swap on low health` toggle, default on |
| **Auto Pot** |  | `module.auto_pot` | `Health threshold` slider 1–20, default 10<br>`Type` mode: Healing / Regen / Both |
| **Silent Aim** |  | `module.silent_aim` | `Smoothing` slider 0–1, default 0.40<br>`Only while attacking` toggle, default on |
| **Trigger Bot** |  | `module.trigger_bot` | `Delay` slider 0–20, default 4<br>`Players only` toggle, default on |
| **Criticals** |  | `module.criticals` | `Mode` mode: Packet / Jump / Mini-jump |
| **Bow Aimbot** |  | `module.bow_aimbot` | `Prediction` slider 0–3, default 1.0<br>`Only when drawn` toggle, default on |
| **Shield Breaker** |  | `module.shield_breaker` | `Method` mode: Axe swap / Disabler |
| **Surround** |  | `module.surround` | `Centre first` toggle, default on<br>`Blocks per tick` slider 1–8, default 4 |
| **Burrow** |  | `module.burrow` | `Mode` mode: Instant / Packet |
| **Hitflick** |  | `module.hitflick` | `Flick angle` slider 0–180, default 35 |
| **Anti Bot** |  | `module.anti_bot` | `Ignore no-armour` toggle, default on |
| **Reach Display** |  | `module.reach_display` | `Units` mode: Blocks / Metres |

## Movement

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **Sprint** |  | `module.sprint` | `Keep while sneaking` toggle, default off<br>`Mode` mode: Legit / Always |
| **Speed** |  | `module.speed` | `Multiplier` slider 1–3, default 1.40<br>`Mode` mode: Vanilla / Strafe / Bhop |
| **No Slow** |  | `module.no_slow` | `Eating` toggle, default on<br>`Shields` toggle, default on<br>`Cobwebs` toggle, default off |
| **Step** |  | `module.step` | `Height` slider 0.5–2.5, default 1.0 |
| **Velocity** |  | `module.velocity` | `Horizontal` slider 0–100, default 0<br>`Vertical` slider 0–100, default 0 |
| **Jesus** |  | `module.jesus` | `Mode` mode: Solid / Dolphin |

## Visual

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **HUD** | live | `module.hud` | `Watermark` toggle, default on<br>`Module list` toggle, default on<br>`Corner` mode: Top right / Top left |
| **FPS Display** | live | `module.fps_display` | _none_ |
| **Coordinates** | live | `module.coordinates` | `Show nether` toggle, default on |
| **Ping Display** | live | `module.ping_display` | _none_ |
| **Fullbright** |  | `module.fullbright` | `Gamma` slider 1–20, default 15 |
| **ESP** |  | `module.esp` | `Shape` mode: Box / Outline / Glow<br>`Players` toggle, default on<br>`Mobs` toggle, default off |
| **Chams** |  | `module.chams` | `Opacity` slider 0–1, default 0.60 |
| **Nametags** |  | `module.nametags` | `Scale` slider 0.5–3, default 1.0<br>`Show health` toggle, default on |
| **Zoom** |  | `module.zoom` | `Factor` slider 1–10, default 4.0 |

## Player

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **Auto Tool** |  | `module.auto_tool` | `Avoid breaking tools` toggle, default on |
| **Auto Eat** |  | `module.auto_eat` | `Hunger threshold` slider 1–20, default 12 |
| **Fast Place** |  | `module.fast_place` | `Delay` slider 0–10, default 2 |
| **Inventory Manager** |  | `module.inventory_manager` | `Auto sort` toggle, default on<br>`Drop junk` toggle, default off |
| **No Fall** |  | `module.no_fall` | `Mode` mode: Packet / Edit / Motion |
| **Freecam** |  | `module.freecam` | `Speed` slider 0.2–5, default 1.0 |

## World

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **Chest ESP** |  | `module.chest_esp` | `Range` slider 8–128, default 32<br>`Trapped chests` toggle, default on |
| **Nuker** |  | `module.nuker` | `Radius` slider 1–8, default 4<br>`Order` mode: Nearest / Top down |
| **Scaffold** |  | `module.scaffold` | `Tower` toggle, default on<br>`Rotation` mode: None / Snap / Smooth |
| **Auto Farm** |  | `module.auto_farm` | `Range` slider 1–6, default 4.5<br>`Replant` toggle, default on |
| **Xray** |  | `module.xray` | `Opacity` slider 0–1, default 0.25 |
| **Terrain Blend** |  | `module.terrain_blend` | `Blend` mode: Off / Soft / Hard |

## SMP

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **Session Timer** | live | `module.session_timer` | _none_ |
| **Playtime Tracker** |  | `module.playtime_tracker` | `Per server` toggle, default on |
| **Death Coords** |  | `module.death_coords` | `Copy to clipboard` toggle, default on |
| **Waypoints** |  | `module.waypoints` | `Render distance` slider 32–1024, default 256<br>`Show distance` toggle, default on |
| **Inventory Value** |  | `module.inventory_value` | `Source` mode: Auction house / Shop<br>`Include hotbar` toggle, default on |
| **AH Price Lookup** |  | `module.ah_price_lookup` | `On hover` toggle, default on |
| **Auto Reconnect** |  | `module.auto_reconnect` | `Delay` slider 1–60, default 5<br>`Attempts` slider 1–100, default 10 |
| **Chat Filter** |  | `module.chat_filter` | `Hide duplicates` toggle, default on<br>`Hide advertisements` toggle, default on |
| **Server Stats** |  | `module.server_stats` | `Show TPS` toggle, default on<br>`Show player count` toggle, default on |

## Misc

| Module | | Config key | Settings |
| --- | --- | --- | --- |
| **FPS Limiter** | live | `module.fps_limiter` | `Limit` slider 10–260, default 60 |
| **Auto GG** |  | `module.auto_gg` | `Delay` slider 0–3000, default 500 |
| **Discord RPC** |  | `module.discord_rpc` | `Show server` toggle, default off |
| **Announcer** |  | `module.announcer` | `Voice` mode: Classic / Robot / Off |
| **Client Chat** |  | `module.client_chat` | `Prefix messages` toggle, default on |
| **Name Protect** |  | `module.name_protect` | `Hide own name` toggle, default on |

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
