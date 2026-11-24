# Ai4Me --- UI/UX Design System & AI Coder Quality Gates

**Document 3 of 3 \| Visual direction, interaction rules, skills, review
process and anti-slop gates**

## 0. Instructions to the coding agent

The UI must look and behave like a deliberate, professionally designed
Android product. Do not generate generic AI-dashboard layouts.

Before implementing any screen: 1. Inspect the existing UI and
navigation. 2. Read this document. 3. Use the UI UX Pro Max skill for
Jetpack Compose guidance. 4. Use Emil Kowalski's design-engineering
skills for motion decisions/reviews. 5. Use the Agency Agents UI/UX
specialists for design critique where the coding environment supports
them. 6. Create or update the design tokens before styling individual
screens. 7. Build the screen with real states and data. 8. Run a visual
and interaction review before calling it complete.

Do not copy a screenshot or reference app pixel-for-pixel. Use proven
patterns, then create a coherent Ai4Me identity.

## 1. Product visual direction

Ai4Me should feel: - Calm. - Intelligent. - Private. - Precise. -
Modern. - Confident. - Premium without being flashy.

Avoid: - Generic purple/blue AI gradients. - Excessive glassmorphism. -
Random floating cards. - Glowing borders on every component. - Oversized
headings that waste space. - Decorative charts with no meaning. - Fake
status indicators. - Huge empty hero sections. - Inconsistent corner
radii. - Too many accent colors. - Repeated pill-shaped controls. -
Every screen looking like the same dashboard template.

The assistant orb may be a recognizable brand element, but it must not
dominate every screen. It is a functional state indicator, not a
substitute for information architecture.

## 2. Design system before screens

Create a central design system in code.

Suggested structure:

``` text
ui/
  theme/
    ColorTokens.kt
    Typography.kt
    Spacing.kt
    Shapes.kt
    Elevation.kt
    Motion.kt
    Ai4MeTheme.kt
  components/
    buttons/
    cards/
    dialogs/
    indicators/
    model/
    voice/
    conversation/
  screens/
```

### 2.1 Tokens

Define semantic tokens: - background - surface - surfaceVariant -
elevatedSurface - primary - onPrimary - secondary - onSecondary -
outline - divider - success - warning - error - listening - processing -
speaking - disabled

Do not scatter raw hex values throughout components.

Support light and dark themes. Test both.

### 2.2 Typography

Use a restrained type scale: - Display: only for rare onboarding/brand
moments. - Headline: screen titles. - Title: section and card titles. -
Body: primary content. - Label: controls and metadata. - Caption:
secondary details.

Use readable line heights and weights. Avoid tiny gray text for
important model sizes, download status, permissions or errors.

### 2.3 Spacing and layout

Use a consistent 4 dp base spacing system. Define named spacing tokens,
not arbitrary values everywhere.

Use: - Clear page margins. - Consistent vertical rhythm. - Logical
grouping. - Content-first sizing. - Responsive layouts for different
screen sizes and font scales. - Proper system-bar and edge-to-edge
handling. - IME-safe input layouts. - Scroll behavior that preserves
context.

Avoid cramming settings into dense, ungrouped lists.

### 2.4 Shapes and elevation

Define a small radius scale and apply consistently. Use elevation and
borders only when they clarify hierarchy. Do not put every section in a
separate card.

## 3. Information architecture

Use a simple top-level navigation. Do not create a bottom navigation
with too many items.

Recommended destinations: 1. **Home:** Assistant status, quick
interaction and recent task. 2. **Chat:** Conversation history and
active session. 3. **Models:** Featured, browse, downloads and installed
models. 4. **Activity:** Tasks, tool execution and audit history. 5.
**Settings:** Voice, permissions, memory, privacy and appearance.

If the navigation pattern needs to change based on usability testing,
document the reason.

## 4. Screen specifications

### 4.1 Home screen

Purpose: make it immediately obvious whether Ai4Me is ready, listening,
processing or speaking.

Required elements: - Assistant identity. - One central interaction
surface. - Clear current state. - Model readiness indicator. -
Microphone/listening control. - Recent task or conversation summary. -
Quick actions that are genuinely useful.

Do not show fake "online", "connected", "thinking" or model status.

Orb behavior: - Idle: restrained, static or subtly breathing. -
Listening: waveform/ring with live state. - Processing: restrained
progress animation. - Speaking: audio-reactive visual only if actual
audio amplitude is available. - Error: explicit state and recovery
action.

The orb must have an accessible text label. Do not rely on color alone.

### 4.2 Chat screen

Must support: - User and assistant messages. - Streaming response. -
Tool activity with understandable labels. - Expandable task details. -
Stop generation. - Retry. - Edit/resend user message. - Copy response. -
Text input. - Voice input. - Follow-up context. - Empty, loading, error
and offline states.

Do not display raw chain-of-thought or private internal reasoning. Show
concise explanations and observable task progress.

Tool activity examples: - "Reading notifications" - "Opening WhatsApp" -
"Preparing message" - "Verifying action"

### 4.3 Model library

This screen is critical. Follow the usability lessons from PocketPal.

Tabs/sections: - Featured. - Browse Hugging Face. - Downloads. -
Installed.

Model card must show: - Model name. - Publisher. - Parameter count. -
Quantization. - File size. - Format. - Compatibility indicator. -
Download/load state. - License indicator where relevant.

Model detail screen: - Description. - Repository link. - Exact file
name. - File size. - Quantization. - Context information. - License. -
Estimated memory range, explicitly labeled as estimate. - Compatibility
warning. - Download/bookmark action. - Load/unload action. - Benchmark
results if available.

Download UI: - Real progress. - Download speed. - Remaining bytes/time
when calculable. - Pause/resume/cancel. - Retry. - Clear error
message. - Storage warning. - Verification state.

Never show a Download button for an unsupported file as if it is
loadable.

No-login public browsing must be obvious. Authentication is only for
gated/private models.

### 4.4 Settings

Organize by user goal: - Models and inference. - Voice and wake word. -
Device permissions. - Automation and autonomy. - Memory and privacy. -
Appearance and accessibility. - Diagnostics.

Each permission row must show: - What the permission enables. - Current
status. - Why Ai4Me needs it. - Open system settings action. - How to
disable it.

Do not use a generic "Grant all" button.

### 4.5 Activity and task history

Show: - Task name. - Current status. - Time. - Steps completed. -
Failed/blocked step. - Verification result. - Cancel/retry where safe.

Use plain language. Do not expose developer stack traces to ordinary
users.

## 5. Interaction and motion standards

Use motion to communicate state, cause and effect. Do not animate
everything.

Rules: - Most interface transitions should be short and purposeful. -
Prefer transform/opacity or Compose-native performant animation APIs. -
Avoid layout-heavy animation on frequently updated lists. - Rapid
interactions must be interruptible. - Loading indicators must represent
real work. - Respect Android animation scale and reduced-motion
preferences where available. - Avoid perpetual decorative motion that
drains battery. - Do not animate frequently used controls without a
usability reason. - Use spring motion only when the interaction benefits
from physical continuity. - Keep state transitions understandable if
animations are disabled.

Use Emil Kowalski's skills as a review standard: - `emil-design-eng` -
`animate` - `review-animations` - `improve-animations` -
`animation-performance` - `remove-animations` when the interface is
over-animated.

His published review guidance is strict: flag motion that is
unnecessary, slow, non-interruptible, poorly eased or expensive. Adapt
web-specific CSS guidance to Compose concepts rather than copying CSS
rules literally.

## 6. Accessibility and usability

Required: - Minimum comfortable touch targets (follow current Android
accessibility guidance). - Semantic labels for icon-only controls. -
Screen-reader order. - Sufficient contrast. - Dynamic type/font
scaling. - Keyboard and IME behavior. - Focus management. - Clear error
messages. - No color-only state communication. - TalkBack testing. -
Landscape and small-screen checks. - Long text and localization checks.

Never truncate important model names, file names, permissions or error
messages without a way to inspect the full value.

## 7. Design workflow required from the AI coder

Before coding a new screen:

1.  Identify the user goal and primary action.
2.  Inspect existing design tokens and components.
3.  Research relevant platform patterns.
4.  Use UI UX Pro Max with the explicit stack
    `Android / Jetpack Compose`.
5.  Produce a short screen specification:
    -   Information hierarchy.
    -   Primary/secondary actions.
    -   States.
    -   Error cases.
    -   Accessibility requirements.
6.  Implement using existing components.
7.  Compare the actual screen against the specification.
8.  Run the UI finish-gate review.
9.  Fix all high-severity findings.
10. Capture screenshots on at least one real device/emulator size.
11. Record unresolved visual issues.

Do not create a new design language for every screen.

## 8. Required design and coding-agent repositories

### 8.1 UI UX Pro Max

Repository: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

Use it as the primary design intelligence skill.

It supports Jetpack Compose and provides searchable styles, palettes,
typography, UX guidelines, stack-specific rules and design-system
generation.

Instructions: - Read its current README and installation guide. -
Install the supported skill for the actual coding agent. - Explicitly
request the Jetpack Compose stack. - Use it before new screen design and
during redesign/review. - Keep the generated design system in the Ai4Me
repository. - Do not accept a generic web/Tailwind recommendation for a
native Compose screen.

### 8.2 Emil Kowalski Skills

Repository: https://github.com/emilkowalski/skills

Install using the repository's documented Skills CLI workflow where
supported:

``` bash
npx skills@latest add emilkowalski/skills
```

Use: - `emil-design-eng` for design-engineering principles. - `animate`
for motion design decisions. - `review-animations` for strict animation
review. - `improve-animations` for auditing existing motion. -
`animation-performance` for performance investigations. -
`remove-animations` to remove unnecessary movement.

Do not blindly apply React Native/Expo-specific implementation code to
Jetpack Compose. Transfer the design principles and implement with
native Compose APIs.

### 8.3 Awesome AI Tools for UI

Repository: https://github.com/maxbogo/awesome-ai-tools-for-ui

Use this as a discovery index, not as a dependency list. Evaluate
relevant tools/skills before adding them. Prefer maintained,
open-source, license-compatible tools that support native
Android/Compose.

Do not install every listed tool.

### 8.4 Agency Agents

Repository: https://github.com/msitarzewski/agency-agents

Use its specialist-agent approach for focused reviews. Relevant roles
include: - UI Designer. - UX Researcher. - UX Architect. - Brand
Guardian. - UI Finish-Gate Reviewer. - Android/engineering specialists
where available. - Reality Checker / security reviewer where available.

For OpenCode, inspect the current README and use the supported
installer. The repository documents project-scoped installation:

``` bash
git clone https://github.com/msitarzewski/agency-agents.git tools/agency-agents
cd tools/agency-agents
./scripts/install.sh --tool opencode
```

Then inspect available agents:

``` bash
./scripts/install.sh --list agents
```

Install or invoke only the relevant specialists. Do not dump every agent
prompt into every coding session. Use specialist agents for discrete
deliverables and return their findings to the main implementation agent.

Agency Agents are development-time prompts/workflows, not runtime
dependencies of the Ai4Me APK.

## 9. Required specialist review sequence

For each major UI feature:

1.  **UX Architect:** Check information architecture, states and
    implementation feasibility.
2.  **UI Designer:** Define visual hierarchy, tokens and component
    behavior.
3.  **Frontend/Android Engineer:** Implement the Compose screen using
    the agreed design.
4.  **Accessibility Reviewer:** Check semantics, contrast, scaling and
    touch targets.
5.  **Motion Reviewer:** Check whether motion is necessary and whether
    it performs well.
6.  **UI Finish-Gate Reviewer:** Reject generic, inconsistent or
    unfinished design.
7.  **Reality Checker:** Verify the screen actually works with real data
    and real app states.

The same person/agent may perform multiple roles if the environment does
not support subagents, but each review must be explicitly completed.

## 10. Anti-slop rejection checklist

Reject a screen if: - It looks like a generic AI dashboard. - It has
random gradients or decorative glow. - It uses too many cards. - It has
inconsistent spacing/radii/type. - It uses placeholder text where real
state is available. - Buttons do not work. - Loading indicators are
fake. - Empty/error/offline states are missing. - Model download status
is unclear. - Important content is clipped. - It fails TalkBack or font
scaling. - Animations are decorative, slow or distracting. - The design
does not match other Ai4Me screens.

A screen is not complete because it compiles.

## 11. UI acceptance gate

Before merging a screen, provide: - Screenshot(s). - Screen
specification. - List of real interactions tested. - Accessibility
review. - Dark/light mode review. - Small/large screen review. -
Animation review if motion exists. - Known issues.

No "looks good" self-approval. Findings must be concrete and resolved or
documented.

## 12. Design references

PocketPal: https://github.com/a-ghorbani/pocketpal-ai

UI UX Pro Max: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

Emil Kowalski Skills: https://github.com/emilkowalski/skills

Agency Agents: https://github.com/msitarzewski/agency-agents

Awesome AI Tools for UI:
https://github.com/maxbogo/awesome-ai-tools-for-ui

Material Design 3: https://m3.material.io/

Android design: https://developer.android.com/design

Compose accessibility:
https://developer.android.com/develop/ui/compose/accessibility

## 13. Final design principle

Ai4Me should feel like one coherent product. The home screen, chat,
model library, downloads, activity and settings must share the same
visual language and interaction logic.

Do not optimize for screenshots alone. Optimize for clarity, speed,
accessibility, consistency and real task completion.

**A polished interface is not decoration. It is part of the product's
reliability.**
