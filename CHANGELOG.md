# Changelog

## Stage 2 — Extensible study cycles (2026-08-31)

- **Causa raiz:** o domínio representava ciclos como um enum com somente `A` e `B`; `Cycle.values()` limitava a UI, a criação selecionava silenciosamente o primeiro enum ausente, a validação tratava qualquer valor não-A como B e o carregamento JSON enumerava apenas os dois valores.
- **Decisão:** `Cycle` agora é uma identidade extensível com `id`, label, descrição e focos requeridos. `Cycle.A` e `Cycle.B` continuam como presets compatíveis, enquanto ciclos customizados são persistidos no template local e não poluem uma enumeração global.
- **Persistência:** o ScheduleStore mantém leitura de documentos legados com dias diretamente dentro de cada chave de ciclo e passa a escrever metadados (`label`, `subjects`, `requiredFocuses`) e um objeto `days` explícito. A ordem do mapa JSON é preservada para determinar o avanço circular.
- **UI:** o botão de novo ciclo abre o mesmo sistema modal usado por identidade e blocos. O usuário escolhe id estável, label, descrição e comportamento inicial: sequência central, cópia do ciclo ativo ou preset acadêmico. A seleção manual do menu é gerada a partir dos ciclos presentes.
- **Validação:** ciclos customizados seguem as regras gerais de duração, pausas e focos estruturais; somente presets que declaram focos adicionais os exigem. Foram adicionados testes de round-trip de metadados e de avanço por três ciclos com wrap-around.
- **Verificação:** `./gradlew clean test --no-daemon --console=plain` passou com 26 testes. Não foi executado rebuild/switch do sistema.

## Stage 5 — JavaFX motion system (2026-08-31)

The planner already animated checkbox pulses, progress-ring updates, detail expansion and session refreshes, but dashboard entry, manager-page navigation, modal presentation and card population remained static. The root cause was that transitions were implemented only at isolated interaction points rather than through a reusable entrance layer.

The UI now uses short fade/slide/scale entrances for the dashboard and manager pages, staggered card and section entrances, subtle modal presentation, and animated session rebuilds for cycle changes, reloads and resets. The implementation reuses JavaFX `ParallelTransition`, `FadeTransition`, `TranslateTransition`, and `ScaleTransition` without changing domain state or control labels. `./gradlew clean test --no-daemon --console=plain` passes with Java 21.
