# Porting queue

Status of the requested module list. Ported files carry a header naming the
upstream class; see [NOTICE.md](NOTICE.md).

**The headline finding:** roughly a third of the list cannot be done as things
stand, because it needs **mixins** — and this project has deliberately never had
any. Everything so far has gone through a public API: an entity attribute, a
packet the client already sends, or a projection onto the HUD. That constraint
bought a lot (no mappings, no remap step, nothing to break on a Minecraft
update) but it has now been reached. Adding mixin support is a real
architectural decision, not a detail, so it is flagged here rather than taken
quietly.

Legend: **done** · **partial** · **todo** (portable, not yet done) ·
**mixin** (blocked on the above) · **declined** · **n/a**

## Combat

| Module | Status | Notes |
| --- | --- | --- |
| KillAura / Forcefield | done | Original. Now turns via Wurst's rotation maths instead of snapping. |
| AutoCrystal | done | Original, as **Crystal Aura**. Placement rules and damage model from vanilla. |
| AimAssist / Aimbot | **done** | Ported from `AimAssistHack`. |
| Triggerbot | done | Original. |
| Velocity / Anti-Knockback | done | Original, damps knockback after a hit. Upstream's packet-level version is better and needs a mixin. |
| Reach | done | Original, via the interaction-range attribute. The server validates against its own copy, so it changes what you can aim at, not what lands. |
| Criticals | **done** | Ported from `CriticalsHack`. Replaced a two-packet version that never worked. |
| AutoTotem | done | Original, via container clicks. |
| BowAimbot | todo | Portable — needs projectile trajectory maths. |
| TargetHUD | todo | Portable — 2D, same path as the existing HUD. |

## Movement

| Module | Status | Notes |
| --- | --- | --- |
| Fly / Flight | done | Original. Upstream's is far richer (622 lines, several modes) and worth revisiting. |
| Speed | done | Original. Now clamps to a ceiling instead of compounding. |
| BHop | **done** | Ported from `BunnyHopHack`. |
| Jesus / WaterWalk | todo | Upstream is 308 lines; the solid-water mode needs a mixin, the motion mode does not. |
| NoFall | done | Original. Tells the server you are grounded; it does not stop the damage, it changes the sum. |
| Step | **done** | Ported from `StepHack` as **Legit** mode; the attribute version is kept as **Simple**. |
| Spider | **done** | Ported from `SpiderHack`. |
| NoSlow | **mixin** | Needs the movement-slowdown multiplier. |
| SafeWalk | **partial** | Ported the "sneak at edges" behaviour. The better mode — not walking off without visibly sneaking — overrides `isStayingOnGroundSurface` and needs a mixin. |
| Phase / VClip | mixin | Needs collision to be bypassed. |
| ElytraFly | todo | Portable. |
| BoatFly | mixin | Needs the boat's physics. |

## Render & visual

| Module | Status | Notes |
| --- | --- | --- |
| ESP | done | Original. Projects to the HUD rather than drawing in the world — 26.2 removed the world-render hooks. |
| Tracers | done | Original. |
| Nametags | done | Original, with health and distance. |
| Trajectories | todo | Portable — the existing projection already does the hard part. |
| Fullbright | **partial** | **Brightness** drives the vanilla gamma option, which caps where the game does. A true fullbright needs the lightmap. |
| X-Ray | **mixin** | Needs block-face culling and a chunk rebuild. |
| Freecam | **mixin** | Needs to detach the camera. |

## Player & utility

| Module | Status | Notes |
| --- | --- | --- |
| ChestStealer / AutoLoot | todo | Portable — same container-click path as AutoTotem. |
| AutoTool | todo | Portable. |
| AutoEat | todo | Portable. |
| AutoArmor | todo | Portable. |
| Scaffold | todo | Portable — needs upstream's `BlockPlacer`. |
| MiddleClickPearl | todo | Portable. |
| InventoryMove | mixin | Needs screen input handling. |
| FastPlace / FastBreak | mixin | Needs the placement and breaking cooldowns. |
| Blink | mixin | Needs to intercept outgoing packets. |

## World

| Module | Status | Notes |
| --- | --- | --- |
| Nuker | todo | Portable — needs upstream's `BlockBreaker`. |
| Baritone | **n/a** | A separate project with its own repository and licence, not a Wurst module. It can be added as a dependency, but that is a dependency decision rather than a port. |
| Packet Canceller / Lag Exploits | **declined** | Described as triggering server desyncs, godmode states and vehicle bugs. That is inducing faults in someone else's server rather than playing the game differently. |
| Spammer / Annoy | **declined** | Automated chat spam and advertising is aimed at the other people on the server, not at the game. |

## Shared code ported

| File | From | Used by |
| --- | --- | --- |
| `feature/Rotation.java` | `util/Rotation` | Aim Assist, Killaura |
| `feature/Rotations.java` | `util/RotationUtils` | Aim Assist, Killaura; will be needed by BowAimbot, Scaffold, Nuker |

Upstream's `BlockPlacer`, `BlockBreaker`, `InventoryUtils` and `ItemUtils` are
the next util classes worth bringing over — between them they unblock Scaffold,
Nuker, ChestStealer, AutoTool, AutoEat and AutoArmor, which is most of the
remaining **todo** list.

## Caveat

Nothing here has been run. Every release so far has been compile-verified
against the 26.2 jar and shipped without anyone launching it.
