# Dashboard architecture

## Product language

All visible strings, descriptions, menu items, status messages, validation dialogs and template labels are English. Persistent identifiers remain unchanged so existing schedule JSON files stay compatible.

## Composition

The root is a vertical dashboard shell with four rows: a compact app bar, a hero widget, a responsive widget grid, and a small status line. The main widget grid is built with `GridPane` and two column constraints on wide windows: a flexible primary column and a bounded secondary column. At the compact breakpoint, the grid becomes one column and the secondary widgets move below the session widget. No visible template selector is placed in the main workflow; the template is an app-level setting exposed in the menu.

The hero widget contains only the current workflow title, cycle badge, progress summary and a single compact menu action. The first widget row contains session progress and rhythm metrics. The second row contains the full-width `Today's flow` widget and a `Vault` widget. Every widget uses the same surface, border, radius, padding and title-row contract.

## Session cards

A session item is a normal `VBox`/`HBox` card, not a `TitledPane`. The card owns its detail region, so expanded and collapsed states share one exact width, border and surface. There is no disclosure arrow. The expand/collapse action is the card itself or a small text action only when needed; the checkbox is 18px and the active state is expressed by a left accent rail and background token, never by an opaque near-white surface. The list is a `VBox` with `fillWidth` and every card uses `setMaxWidth(Double.MAX_VALUE)`.

## Responsive behavior

The window has a practical minimum of 720×620 rather than a desktop-only 980px width. The wide layout is used at 1040px and above; the compact layout is used below it. At compact widths, widgets stack, the Vault folder labels remain ellipsized, the hero action wraps into its own row, and the flow card text can wrap without horizontal overflow. A single internal flow scroll viewport is allowed only when the session content exceeds the available height; it has no horizontal scrollbar and uses a 6px thumb. No nested widget adds its own viewport.

## Tokens

The stylesheet defines a small token layer for spacing, radii, surfaces, borders, muted text, accent, success and focus. Controls use explicit compact sizes. The same tokens are applied to widget panels, cards, buttons, menu/context-menu surfaces, combo popups and scrollbars. No generic `.button` rule assigns a large padding to all controls.

## Layout translation from CSS

The Vault reference uses CSS Grid for macro regions and Flexbox for one-dimensional card rows. JavaFX has no CSS Grid; the direct equivalent is `GridPane` with `ColumnConstraints`/`RowConstraints`, plus `HBox`/`VBox` with grow priorities and fill widths. The implementation must keep these responsibilities separate: GridPane places widgets; HBox/VBox aligns the contents of each widget.
