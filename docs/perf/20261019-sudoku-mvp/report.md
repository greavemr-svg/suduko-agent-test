# Performance report 20261019-sudoku-mvp (head 212365a)

Result: NO_BASELINE. `docs/perf/baseline.json` does not exist, so no regression comparison is possible. A proposed baseline is in `proposed-baseline.json`; the owner promotes it.

| Spec NFR | Evidence | Verdict |
|---|---|---|
| New puzzle within 2 s | JVM: mean 1.2 ms, p95 1.5 ms, max 76.7 ms (200 seeds, agent run). Max nodes 1382 of 20000 budget. First-launch Fully drawn (includes generation) median 722 ms, worst 1107 ms (tester, emulator). | Met, large margin |
| Tap result within 100 ms | Tester observed instant feedback; not measured, GPU frame bars NOT RUN. | Not quantified |
| Restore adds at most 1 s | Restore Fully drawn median 716 ms, worst 749 ms; overhead about 100 ms median (tester). | Met |

Caveats: device numbers come from the task 7 tester, not re-run here. Emulator, not physical hardware. JVM max 76.7 ms is likely JIT warm-up. Tap latency lacks a measurement.
