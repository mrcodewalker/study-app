---
name: Serene Study
colors:
  surface: '#f8fafa'
  surface-dim: '#d8dada'
  surface-bright: '#f8fafa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f4'
  surface-container: '#eceeee'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e3'
  on-surface: '#191c1d'
  on-surface-variant: '#414846'
  inverse-surface: '#2e3131'
  inverse-on-surface: '#eff1f1'
  outline: '#717976'
  outline-variant: '#c0c8c5'
  surface-tint: '#3f665c'
  primary: '#3f665c'
  on-primary: '#ffffff'
  primary-container: '#b8e2d6'
  on-primary-container: '#3f665c'
  inverse-primary: '#a6cfc3'
  secondary: '#5e5b7a'
  on-secondary: '#ffffff'
  secondary-container: '#ded9fd'
  on-secondary-container: '#605d7c'
  tertiary: '#4d616e'
  on-tertiary: '#ffffff'
  tertiary-container: '#c6dceb'
  on-tertiary-container: '#4d616e'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c1ebdf'
  primary-fixed-dim: '#a6cfc3'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#274e45'
  secondary-fixed: '#e4dfff'
  secondary-fixed-dim: '#c7c2e6'
  on-secondary-fixed: '#1b1833'
  on-secondary-fixed-variant: '#464361'
  tertiary-fixed: '#d0e6f5'
  tertiary-fixed-dim: '#b4cad8'
  on-tertiary-fixed: '#071e29'
  on-tertiary-fixed-variant: '#354955'
  background: '#f8fafa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e3'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  margin-edge: 24px
  gutter: 16px
  stack-sm: 12px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

The design system is anchored in "Cognitive Calm"—a philosophy aimed at reducing the mental load of students through extreme clarity and soft visual feedback. The brand personality is encouraging, organized, and breathable, moving away from the high-pressure aesthetics typical of productivity tools.

The design style is **Minimalist with Tactile Softness**. It blends the structural logic of a card-based layout with a gentle, organic feel. By utilizing generous whitespace and a restricted pastel palette, the UI directs focus solely on the content, mirroring the organized simplicity of a fresh notebook. The emotional response is one of focus and tranquility, ensuring that the interface never competes with the user's study material for attention.

## Colors

The color palette is derived from natural, desaturated tones to prevent eye strain during long study sessions. 

- **Soft Mint (Primary):** Used for primary actions, progress indicators, and success states. It represents growth and focus.
- **Lavender (Secondary):** Used for creative tasks, tags, and secondary navigation elements.
- **Pale Blue (Tertiary):** Applied to informational backgrounds and calm deep-work timers.
- **Warm Sand (Accent):** Reserved for highlights, reminders, and "aha!" moments.
- **Surface & Neutral:** The background uses a nearly-white mint tint to reduce glare, while text is a deep charcoal rather than pure black to maintain softness.

## Typography

The typography system utilizes **Inter** for its exceptional legibility and modern, neutral character. It mimics the functional clarity of Notion.

Headlines use a tighter letter-spacing and heavier weight to create a clear information hierarchy against the soft UI. Body text is optimized for readability with a generous line-height. Labels use all-caps in specific contexts (like tags or small metadata) with increased letter-spacing to ensure they remain distinct even at small sizes.

## Layout & Spacing

This design system follows a **Fluid Grid** model optimized for mobile. The layout relies on a 4-column grid for standard portrait views.

Margins are set to a generous 24px to enforce the feeling of "breathability." Vertical spacing (stacking) is categorized into three tiers to separate unrelated content blocks clearly. Components within cards should use 16px internal padding to maintain the minimalist aesthetic. Whitespace is treated as a functional element, used to group related items without the need for heavy dividers.

## Elevation & Depth

Hierarchy is established through **Tonal Layers** and **Ambient Shadows**. 

Instead of traditional Material Design shadows, this system uses very soft, low-opacity "Diffusion Shadows" (Blur: 20px, Opacity: 4-6%, Color: Primary-Dark). Surfaces are tiered as follows:
1. **Level 0 (Canvas):** The base neutral background.
2. **Level 1 (Cards):** Pure white surfaces with subtle shadows to appear slightly "lifted."
3. **Level 2 (Overlays/Modals):** Surfaces with slightly more shadow depth and a subtle backdrop blur on the canvas behind them.

Outlines are avoided unless used for "Ghost" buttons, in which case they use a 1px stroke in a very pale version of the primary color.

## Shapes

The shape language is characterized by "Smooth Continuity." All containers use a **Large Border-Radius** to evoke a friendly, non-threatening atmosphere. 

Standard cards and input fields use a 16px (1rem) radius. Smaller elements like chips and tags use an 8px (0.5rem) radius. Buttons are often pill-shaped to differentiate them from informational cards. The goal is to eliminate all sharp 90-degree angles from the interface to sustain the "Serene Study" aesthetic.

## Components

- **Rounded Cards:** The core of the UI. Cards have a 16px corner radius and no border. They utilize the "Diffusion Shadow" to stand out from the light mint background.
- **Minimalist Icons:** Use thin-stroke (1.5px) linear icons. Avoid filled icons unless indicating an active state in the bottom navigation.
- **Bottom Navigation:** A floating bar with a 24px radius, using high transparency and a backdrop blur (Glassmorphism light). Active states are indicated by a soft pastel glow behind the icon.
- **Soft Progress Indicators:** Progress bars use a thick 8px height with fully rounded caps. The track is a very pale version of the primary color, and the filler is the Primary Mint.
- **Buttons:** Primary buttons are pill-shaped with a solid soft-mint background. Text is center-aligned and slightly bold.
- **Inputs:** Fields are defined by a light-grey background rather than a border, turning into a soft-mint tint when focused.
- **Study Chips:** Small, highly rounded labels used for subjects or tags, using the Secondary and Tertiary pastel colors to color-code information.