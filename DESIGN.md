# Design System

## Direction

AirShare is presented as a compact, trustworthy network appliance: deep navy carries the page, aqua signals a live local connection, and quiet blue-white surfaces keep product information easy to scan.

## Color

- Navy: `oklch(24% .055 239)`
- Aqua: `oklch(80% .13 175)`
- Blue: `oklch(58% .15 239)`
- Mist: `oklch(97% .012 232)`
- Ink: `oklch(22% .035 240)`

## Typography

Use Figtree for the Latin interface with Noto Sans TC and Noto Sans JP as language-aware fallbacks. Headlines are compact, bold, balanced, and never tighter than `-0.04em`.

## Layout

Use a 1180px maximum content width, decisive dark/light section changes, thin dividers, and two-column structures that collapse cleanly below 900px. Avoid repeated floating cards; reserve contained surfaces for platform comparisons and the product screens.

## Components

Buttons are pill-shaped because they are actions. Content containers stay square or moderately rounded. Language selection is keyboard-operable. Device frames use real iOS imagery and a faithful Android UI preview derived from the product interface.

## Motion

Use a single restrained reveal system with exponential easing. Respect `prefers-reduced-motion` and never hide content when scripting is unavailable.
