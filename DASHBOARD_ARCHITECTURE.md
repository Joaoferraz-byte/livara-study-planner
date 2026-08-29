# Dashboard architecture

## Visual direction

The dashboard follows the visual language of the historical Livara Vault study-cycle dashboard, not a generic admin panel. It uses a dark ambient shell, subtle geometry, semantic accent colors, controlled density, layered surfaces, compact metadata and deliberate state transitions. Decorative elements are allowed only when they communicate progress, focus, or navigation.

## JavaFX layout responsibilities

`StackPane` owns the root background and non-interactive ambient decoration. A background layer contains a low-opacity `Canvas`/`Pane` with radial and orbital shapes; the content layer remains independent so decoration never changes measurement or hit-testing. `VBox` owns the macro page flow. `HBox` owns one-dimensional rows such as the app bar, action rows, metadata chips and card headers. `FlowPane` owns metric tiles where items may wrap; it is preferable to hard-coded columns for compact windows. `GridPane` is reserved for the stable three-tile overview strip where equal-width alignment is meaningful. There is no attempt to copy the Vault's CSS Grid for every section.

## Widget hierarchy

The top bar is compact and contains brand context plus one `MenuButton`. The hero is a two-zone widget: the left side holds an English eyebrow, title, explanatory copy, current workflow/cycle metadata and a small action row; the right side holds a progress ring with completed blocks and a short cycle label. The main content is a wide vertical flow widget beside a stack of status widgets on desktop, and a single vertical flow on compact windows.

The status stack contains `Current rotation`, `Completed cycles`, and `Session rhythm`. Each panel uses the same widget factory but may apply one semantic accent token. A compact Vault action panel uses the same action-row pattern as the historical dashboard and avoids unnecessary folder glyphs.

## Rich study block contract

Every focus block is a stateful card. Its header contains a dedicated 24–28px circular check action, a semantic 16–18px icon glyph in a bounded surface, an eyebrow such as `BLOCK 01 · FOCUS`, a title, a one-line status and a duration pill. The card body is hidden by default, but the card remains a single measured component when opened; the body contains focus/break metadata chips, a strategy callout and three short task rows. A completed block changes the check action to a success state, shows a check glyph, applies a restrained success border and collapses the body. An unfinished block can be expanded by clicking its header; there is no visible chevron/expand icon.

A transparent accessible `CheckBox` remains in the scene graph for keyboard and screen-reader semantics, but its visual representation is a custom `StackPane`/CSS pseudo-surface. Its dimensions are explicit and compact; the hit area may be 28px while the drawn mark is 16–18px. This avoids the oversized native checkbox seen in the previous version.

## Responsive behavior

The window chooses its initial size from the monitor bounds. Above 1040px, the flow widget receives the flexible primary width and the status stack receives a bounded secondary width. Below 1040px, the status stack follows the flow vertically. Below 820px, the hero becomes one column and the progress ring moves below the copy. Below 600px, the metric tiles wrap through `FlowPane`, card text wraps, metadata chips wrap and the app keeps only one internal vertical `ScrollPane` for the sequence. No horizontal scrollbar is permitted.

## Visual tokens

The CSS token layer owns background, surface, surface-raised, surface-highlight, border, border-strong, text, muted, faint, accent, accent-soft, secondary, success, warning, radius and spacing. All widgets use these tokens. No rule maps a looked-up color to a lightened near-white surface. The selected state uses a dark surface plus an accent rail; success uses a dark green-tinted surface. Menu, dialog and combo popup surfaces explicitly share the widget surface and radius so no square white parent leaks outside a rounded child.
