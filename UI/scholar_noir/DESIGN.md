# Design System Specification: The Academic North Star

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Digital Mentor."** 

Moving away from the cold, industrial feel of traditional academic portals, this system embraces an "Editorial Tech" aesthetic. We are not just building an app; we are crafting a sophisticated, guided journey for the next generation of Tunisian leaders. 

To break the "template" look, we utilize **Intentional Asymmetry** and **Tonal Depth**. Layouts should avoid rigid, centered grids in favor of dynamic compositions where content "floats" on layers of deep space. By using high-contrast typography scales and overlapping glass elements, we create an environment that feels both authoritative (Academic) and cutting-edge (AI-driven).

---

## 2. Colors & The "Atmospheric" Palette
We do not use "gray." We use depths of navy and light.

### Core Tokens
*   **Background / Surface Dim:** `#0B0E14` (The void of possibility)
*   **Primary (Academic Blue):** `#85ADFF` (Vibrant, optimized for dark-mode legibility)
*   **Secondary:** `#7F98FF` (Deep iris for supportive actions)
*   **Tertiary (Success Green):** `#B7FFBC` (Fresh growth/acceptance)

### The "No-Line" Rule
**Explicit Instruction:** Prohibit the use of 1px solid borders for sectioning. 
Boundaries must be defined solely through background color shifts or subtle tonal transitions. For example, a student’s profile summary (using `surface-container-low`) should sit directly on the `surface` background. The change in hex value is the border.

### The "Glass & Gradient" Rule
To achieve the "High-Tech AI" feel, floating elements (modals, navigation bars, or featured course cards) must use **Glassmorphism**:
*   **Fill:** `surface-container` at 60% opacity.
*   **Effect:** Background Blur (20px - 40px).
*   **Signature Texture:** Use a linear gradient on Primary CTAs (from `primary` to `primary-container`) to provide a "glow" that feels liquid and premium rather than flat.

---

## 3. Typography: Editorial Authority
We use **Plus Jakarta Sans** for its geometric clarity and modern Tunisian appeal. The tone is "Pro-Friendly"—incorporating Tunisian Darija phrases like *"Win nhebou nemshiw?"* (Where do we want to go?) in high-contrast headlines.

*   **Display (The Hook):** `display-lg` (3.5rem) should be used sparingly for "Welcome" moments or major milestones. Use tight letter-spacing (-0.02em).
*   **Headline (The Guide):** `headline-sm` (1.5rem) is our workhorse for section titles. It conveys academic weight.
*   **Body (The Mentor):** `body-lg` (1rem) for all descriptive text. Ensure a line height of at least 1.6 to maintain "breathing room" in complex academic descriptions.
*   **Label (The Detail):** `label-sm` (0.6875rem) in `on-surface-variant` for metadata (e.g., "Duration: 3 years").

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are too "heavy" for this dark aesthetic. We use **Tonal Layering** to create hierarchy.

### The Layering Principle
Stack surfaces to create a natural lift:
1.  **Base:** `surface` (#0B0E14)
2.  **Section:** `surface-container-low` (#10131A)
3.  **Card/Component:** `surface-container-high` (#1C2028)
4.  **Floating/Active:** `surface-bright` (#282C36)

### Ambient Shadows
If a floating effect is required (e.g., a Bottom Sheet for university filters), use an **Ambient Shadow**:
*   **Blur:** 40px.
*   **Spread:** 0.
*   **Color:** `surface-container-highest` at 15% opacity. 
*   **The Ghost Border:** If accessibility requires a stroke, use `outline-variant` at 15% opacity. Never use 100% opaque lines.

---

## 5. Components
### Buttons (The "Action" Glow)
*   **Primary:** Filled with `primary` (#85ADFF). Text in `on-primary` (#002C65). Apply a subtle `primary` outer glow (8px blur, 10% opacity) to mimic a self-illuminated AI interface.
*   **Secondary:** Glass-filled. `surface-container-highest` at 40% with a `primary` "Ghost Border."

### Cards (Academic Orientation)
*   **Rule:** No dividers. Use `surface-container-low` for the card body and `surface-container-high` for the header area of the card.
*   **Interaction:** On hover, a card should shift from `surface-container-low` to `surface-container-highest` and scale by 1.02x.

### Input Fields (The Focused Student)
*   **State:** Default fields should be "Unfilled/Ghost" styles. Only the bottom stroke or the container background shift (`surface-container-highest`) indicates the hit area. 
*   **Validation:** Error states use `error` (#FF716C) text, but the container should use a subtle `error_container` (#9F0519) background at 20% opacity.

### Navigation (The "Compass")
*   Use a bottom navigation bar with a high `backdrop-blur` (32px). The active icon should use the `primary` color with a small dot indicator below it; no labels for a cleaner, high-end feel.

---

## 6. Do's and Don'ts

### Do
*   **Use Local Tone:** Use Darija for micro-copy (e.g., *"Saffi el ikhtiyarat"* instead of just "Filter").
*   **Embrace Negative Space:** Let the academic content breathe. If a screen feels crowded, increase the `surface` padding rather than adding lines.
*   **Layer the AI:** Use `tertiary` (#B7FFBC) for AI-driven recommendations to make them feel distinct from standard academic data.

### Don't
*   **Don't use Pure White:** Avoid `#FFFFFF`. Use `on-surface` (#ECEDF6) for text to prevent eye strain in dark mode.
*   **Don't use Sharp Corners:** Stick to the `md` (0.75rem) or `lg` (1rem) roundedness scale. Sharp corners feel clinical and unfriendly.
*   **Don't use Default Shadows:** Standard "Drop Shadows" look muddy on deep navy. Always use tonal shifts or blurs.

### Accessibility Note
Ensure that all `primary` text on `surface` backgrounds maintains a contrast ratio of at least 4.5:1. Use `primary_dim` if the vibrant blue vibrates too much against the deep navy for long-form reading.