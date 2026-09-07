# Notices and attribution

## This project's licence

Bean Client is licensed under the **GNU General Public License, version 3**
(GPL-3.0-only). The full text is in [LICENSE](LICENSE).

It was **MIT licensed up to and including v1.20.0**. That release, and every
release before it, remains available under MIT — relicensing applies going
forward, not retroactively. From v1.22.0 onward the project is GPL-3.0.

The relicence was not cosmetic. It happened because the project began
incorporating work derived from Wurst 7, which is GPL-3.0, and GPL-3.0 is a
copyleft licence: code taken from it can only be redistributed as part of a
GPL-3.0 work, with the licence preserved and the source available. Continuing to
ship that work under MIT would have stripped the licence its authors chose.

## Work derived from Wurst 7

Some modules are derived from the **Wurst 7** Minecraft client and the
**Wurst7-CevAPI** fork:

- Wurst 7 — <https://github.com/Wurst-Imperium/Wurst7>
  Copyright (c) 2014-2026 Wurst-Imperium and contributors.
- Wurst7-CevAPI — <https://github.com/cev-api/Wurst7-CevAPI>
  An independent fork of Wurst 7 by cev-api.

Both are licensed under the GNU General Public License, version 3.

Every file in this repository that is derived from that work carries a header
saying so, naming the upstream class it came from and stating that it has been
modified. Files without such a header are original to this project.

Modules derived from upstream have been adapted rather than copied verbatim:
Wurst organises features as `Hack` subclasses driven by its own event manager
and settings system, while this project registers modules against
`ModuleRegistry` and reads values through `Settings`. The algorithms are
upstream's; the wiring is not.

## Original work

The GUI, the anti-aliased scanline renderer (`gui/Draw.java`, `gui/Shapes.java`),
the vector icon set, the JSON theming system, the HUD readouts and the
world-to-screen projection used by the Render modules are original to this
project and are not derived from Wurst.

## No affiliation

This project is not affiliated with, endorsed by, or supported by
Wurst-Imperium, cev-api, Mojang, or Microsoft.
