---
target: launcher UI
total_score: 30
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:/Users/sunnypc/Documents/GitHub/minimalistphone/app/src/main/java/com/pocketjarvis/launcher/ui"
timestamp: 2026-10-27T19-14-39Z
slug: app-src-main-java-com-pocketjarvis-launcher-ui
---
Method: design review only (detector not applicable, native platform)

# Critique: launcher UI (Pocket JARVIS), Operate mode. Score 30/40 (Good)

Heuristics: 1=3, 2=3, 3=3, 4=3, 5=3, 6=3, 7=3, 8=3, 9=4, 10=2.

## Design specificity
Authored for this product: dial with graduated battery arc, chamfered tiles with glyphs redrawn from the user's apps, battery level as the colour of the whole UI. Identity is concentrated on Home; app list, Setup and drawer are generic lists. Type hierarchy is flat (tracked uppercase everywhere).
Contrast computed from palette math: tile border vs bg 1.55:1 (yellow 1.70, red 1.29); tile fill vs bg 1.07:1; inactive page dot 1.55:1; muted text 8.36:1 (yellow 9.20, red 6.93).

## Priority issues
1. [P1] Borders and the page indicator are nearly invisible (1.55:1; dots 6 dp x 28 dp, no accessible name). Fix: raise line mix to about 3:1, brighter inactive dot, 48 dp touch height, "Page 1 of 2". Commands: colorize, audit.
2. [P1] Supporting text 11-12 sp (date, ADD, hints, Setup captions) against a "no tiny text" brief. Fix: 13 sp floor, date 14 sp. Command: typeset.
3. [P1] Long-press, page 2 and the unset Tools tile are undiscoverable. Fix: dismissible first-run hint, "Choose app" empty state on Tools. Command: onboard.
4. [P2] Secondary screens lose the instrument language: per-row "..." buttons in the app list, no icons in list or drawer, 7 options in the drawer. Commands: distill, layout.
5. [P2] Charging stacks four effects (beams+bloom, sonar rings, tick wave, breathing glow) over the clock. Fix: keep beams and glow, make rings/wave optional or fainter. Command: quieter.

## Persona red flags
Casey: Back/Setup/search top of screen, borders wash out outdoors, red mode borders 1.29:1. Sam: 11-12 sp captions, Home caps font scale at 1.3x, dots have Role.Tab but no name, charging signalled by "+" and motion only. Jordan: Tools opens a chooser unexplained, Tools vs Utilities naming, nothing teaches long-press, "+" means charging and add.

## Minor
Tile top highlight 9% white (invisible). Naming overlap (Tools / Utilities / Settings / Launcher setup / Setup). Shrink-to-fit makes sibling labels uneven at 2.0x. "..." row button styled like a primary action. Setup banner pushes dial down on first run.

## Questions
Bright borders and dark fills as the identity? Tone only on dial/glyphs instead of panels at 15%? What should the phone show at 2 a.m. on the charger?
