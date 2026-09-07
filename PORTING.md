# Porting queue

Status of the requested module list. Ported files carry a header naming the
upstream class; see [NOTICE.md](NOTICE.md).

**Everything asked for is in, except the three that were declined or are not
ports.** The list ran to 45 entries; 42 of them now exist.

Adding mixins was the decision that unblocked the last third. It was made
reluctantly and the cost is real: every mixin anchors to a vanilla method, so a
Minecraft update can now stop the client launching rather than merely making a
module stop working. All eleven target whole methods rather than instructions
inside them, which is the safer end of that trade.

## Combat

| Module | Status | Notes |
| --- | --- | --- |
| KillAura / Forcefield | done | Turns via Wurst's rotation maths rather than snapping. |
| AutoCrystal | done | As **Crystal Aura**. Placement rules and damage model from vanilla. |
| AimAssist / Aimbot | done | Ported from `AimAssistHack`. |
| Triggerbot | done | |
| Velocity / Anti-Knockback | done | Damps knockback after a hit. |
| Reach | done | Via the interaction-range attribute; the server validates against its own copy. |
| Criticals | done | Ported from `CriticalsHack`. |
| AutoTotem | done | Container clicks. |
| BowAimbot | done | Ported. Solves the launch angle for drop and target movement. |
| TargetHUD | done | Reads whatever the combat modules already chose. |

## Movement

| Module | Status | Notes |
| --- | --- | --- |
| Fly / Flight | done | |
| Speed | done | Clamps to a ceiling rather than compounding. |
| BHop | done | Ported from `BunnyHopHack`. |
| Jesus / WaterWalk | done | Ported. Bob and Walk modes. |
| NoFall | done | |
| Step | done | Ported from `StepHack` as **Legit**; attribute version kept as **Simple**. |
| Spider | done | Ported from `SpiderHack`. |
| NoSlow | done | Mixin on the item-use speed multiplier. |
| SafeWalk | done | Mixin on the player's own edge test; **Visible sneak** keeps the old key-pressing mode. |
| Phase / VClip | done | No mixin needed - `noPhysics` is a public field. |
| ElytraFly | done | Ported. |
| BoatFly | done | No mixin needed - a vehicle's motion is writable. |

## Render & visual

| Module | Status | Notes |
| --- | --- | --- |
| ESP | done | Projects to the HUD; 26.2 removed the world-render hooks. |
| Tracers | done | |
| Nametags | done | With health and distance. |
| Trajectories | done | Ported. Simulated, then projected onto the HUD. |
| Fullbright | done | Mixin on the lightmap. **Brightness** remains the vanilla-capped version. |
| X-Ray | done | Mixin on face culling, plus a chunk rebuild on toggle. |
| Freecam | done | Mixin at the tail of the camera update. |

## Player & utility

| Module | Status | Notes |
| --- | --- | --- |
| ChestStealer / AutoLoot | done | Finds the container's own slots as everything before the trailing 36. |
| AutoTool | done | Compares with vanilla's own `getDestroySpeed`. |
| AutoEat | done | |
| AutoArmor | done | Scores from the item's own attribute modifiers. |
| Scaffold | done | Built on the ported `BlockPlacer`. |
| MiddleClickPearl | done | As **Pearl Key**. Not a port - upstream has no equivalent. |
| InventoryMove | done | Mixin on `KeyMapping.isDown`, movement keys only, chat excluded. |
| FastPlace / FastBreak | done | Accessors onto two private cooldown counters. |
| Blink | done | Mixin on `Connection.send`, movement packets only, bounded queue. |

## World

| Module | Status | Notes |
| --- | --- | --- |
| Nuker | done | Built on the ported `BlockBreaker`. |
| Baritone | **n/a** | A separate project with its own repository and licence, not a Wurst module. It could be added as a dependency; that is a dependency decision rather than a port. |
| Packet Canceller / Lag Exploits | **declined** | Described as triggering server desyncs, godmode states and vehicle bugs. That is inducing faults in someone else's server rather than playing the game differently. |
| Spammer / Annoy | **declined** | Automated chat spam and advertising is aimed at the other people on the server, not at the game. |

## Shared code ported

| File | From |
| --- | --- |
| `feature/Rotation.java`, `Rotations.java` | `util/Rotation`, `util/RotationUtils` |
| `feature/Blocks.java` | `util/BlockUtils` |
| `feature/BlockPlacer.java` | `util/BlockPlacer` |
| `feature/BlockBreaker.java` | `util/BlockBreaker` |
| `feature/Inventories.java` | `util/InventoryUtils` |

## The caveat that has not changed

**None of this has been run.** Every release has been compile-verified against
the 26.2 jar and shipped without anyone launching it. That gap matters more now
than it did: a mixin that fails to apply crashes the game on startup, and
compiling proves only that the Java is valid, not that the injection points
still exist at runtime. The GUI can be checked with `tools/preview/run.sh`;
nothing else can.
