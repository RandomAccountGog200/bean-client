"""Generate MODULES.md from DefaultModules.java, so the doc cannot drift."""
import pathlib
import re

repo = pathlib.Path(r"D:\code stuff\dma ai\bean-client")
src = (repo / "src/main/java/club/bean/client/module/DefaultModules.java").read_text(encoding="utf-8")

# Category order comes from the enum, not from this file.
cat_src = (repo / "src/main/java/club/bean/client/module/Category.java").read_text(encoding="utf-8")
order = re.findall(r"^\s{4}([A-Z_]+)\(\"([^\"]+)\"", cat_src, re.M)
labels = {name: label for name, label in order}

TAGLINE = {
    "HUD": "Readouts drawn on your own screen.",
    "COMBAT": "Acts on your behalf. Every one of these is what an anticheat is looking for.",
    "MOVEMENT": "Changes how you move, and what the server is told about it.",
    "RENDER": "Draws what the client knew but had decided not to show you.",
    "VISUAL": "How your own client renders.",
    "SMP": "Server quality of life.",
    "MISC": "Everything else.",
}

# --- parse the registration table -----------------------------------------
modules = []          # (category, name, description, [(kind, label, detail)])
current = None
pending_desc = []


def to_id(name):
    return re.sub(r"[^a-z0-9]+", "_", name.lower())


def setting_id(name):
    return name.lower().replace(" ", "_")


# Slice between registrations rather than reading to the next semicolon: a
# toggle lambda can contain statements of its own, and an early cut would drop
# the chained .setting() calls that follow it.
starts = [(m.start(), m.group(1), m.group(2) or "HUD") for m in re.finditer(
    r'(?:module|hud)\(\s*"([^"]+)"(?:\s*,\s*Category\.([A-Z_]+))?', src)]

for index, (start, name, category) in enumerate(starts):
    end = starts[index + 1][0] if index + 1 < len(starts) else len(src)
    chunk = src[start:end]

    # Description: the last quoted run that is not a Setting label.
    body = re.sub(r'Setting\.\w+\([^)]*\)', '', chunk)
    strings = re.findall(r'"((?:[^"\\]|\\.)*)"', body)
    desc_parts = [s for s in strings[1:]]
    description = "".join(desc_parts).replace('\\"', '"').strip()

    settings = []
    for sm in re.finditer(r'Setting\.(toggle|slider|mode)\(\s*"([^"]+)"\s*(?:,\s*([^)]*))?\)', chunk):
        kind, label, rest = sm.group(1), sm.group(2), (sm.group(3) or "").strip()
        args = [a.strip() for a in rest.split(",")] if rest else []
        if kind == "toggle":
            detail = f"toggle, default {args[0]}" if args else "toggle"
        elif kind == "slider":
            if len(args) >= 3:
                detail = f"slider {args[1]}\u2013{args[2]}, default {args[0]}"
            else:
                detail = "slider"
        else:
            detail = "mode: " + " / ".join(a.strip('"') for a in args)
        settings.append((kind, label, detail))
    modules.append((category, name, description, settings))

by_cat = {}
for category, name, description, settings in modules:
    by_cat.setdefault(category, []).append((name, description, settings))

cats = [c for c, _ in order if c in by_cat]
total_modules = len(modules)
total_settings = sum(len(s) for _, _, _, s in modules)

# --- render ---------------------------------------------------------------
out = []
out.append("# Modules\n")
out.append("Every module Bean Client registers, generated from the registry itself by")
out.append("`tools/gen_modules.py` \u2014 so this file cannot drift from the code.\n")
out.append("**Every one of them works.** There is no placeholder list \u2014 if a row is in the")
out.append("menu, toggling it changes something.\n")
out.append("The set splits in two, and the split matters more than the tab names:\n")
out.append("- **HUD, Visual, SMP and Misc** read state the vanilla client already has and draw")
out.append("  it on your own screen, or move a vanilla option you could have moved yourself.")
out.append("  Nothing is sent to the server and nothing is automated on your behalf.")
out.append("- **Combat, Movement and Render** do none of that. They act for you, show you what")
out.append("  the client had decided not to draw, and in several cases tell the server")
out.append("  something untrue. Any server running an anticheat is looking for them.\n")
out.append("There are still **no mixins**. Everything goes through a public API \u2014 an entity")
out.append("attribute, a packet the client already sends, or a projection onto the HUD \u2014 which")
out.append("is a real constraint on what these can do. Reach is the clearest case: it lengthens")
out.append("the client raycast, and the server throws the result away.\n")
out.append(f"There are **{total_modules} modules** across {len(cats)} categories.\n")

out.append("| Category | Modules | Settings |")
out.append("| --- | ---: | ---: |")
for c in cats:
    rows = by_cat[c]
    out.append(f"| [{labels[c]}](#{labels[c].lower().replace(' ', '-')}) | {len(rows)} "
               f"| {sum(len(s) for _, _, s in rows)} |")
out.append(f"| **Total** | **{total_modules}** | **{total_settings}** |\n")
out.append("---\n")

for c in cats:
    out.append(f"## {labels[c]}\n")
    out.append(f"_{TAGLINE.get(c, '')}_\n")
    out.append("| Module | What it does | Settings |")
    out.append("| --- | --- | --- |")
    for name, description, settings in by_cat[c]:
        if settings:
            cell = "<br>".join(f"`{label}` \u2014 {detail}" for _, label, detail in settings)
        else:
            cell = "_none_"
        out.append(f"| **{name}**<br>`module.{to_id(name)}` | {description} | {cell} |")
    out.append("")

out.append("---\n")
out.append("## Themes tab\n")
out.append("The last rail tab is not a module list. It holds the theme dropdown, the live")
out.append("accent colour picker, and the Reset / Save file / Reload buttons that connect the")
out.append("GUI to your `config/beanclient/themes/` folder. See")
out.append("[README.md](README.md#writing-a-theme).\n")
out.append("## Where the values live\n")
out.append("Toggles persist to `config/beanclient/config.json` under `module.<id>`, and each")
out.append("setting under `setting.<module id>.<setting id>`. Ids are the lowercased name with")
out.append("non-alphanumerics collapsed to `_`, so *FPS Limiter* is `fps_limiter` and its")
out.append("*Limit* slider is `setting.fps_limiter.limit`.\n")
out.append("A module that borrows a vanilla video option also parks the displaced value under")
out.append("`vanilla.<option>` while it holds it, so a session that never shuts down cleanly")
out.append("can still hand your own setting back on the next launch. Attribute-based modules")
out.append("need no such thing: transient modifiers are never serialised.\n")

(repo / "MODULES.md").write_text("\n".join(out), encoding="utf-8")
print(f"MODULES.md: {total_modules} modules, {total_settings} settings, {len(cats)} categories")
for c in cats:
    print(f"  {labels[c]:9} {len(by_cat[c]):2}")
