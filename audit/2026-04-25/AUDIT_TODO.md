## Appendix: Priority Fix Order

If you can only fix 10 things, fix these first (ordered by impact):

| Priority | Finding | Impact |
|----------|---------|--------|
| 1 | 1.1 — `winner.toString()` | Every game-over crashes the client |
| 2 | 2.1 — SHA-256 without salt | All passwords trivially crackable |
| 3 | 1.4 — `sendMessage()` not thread-safe | Protocol corruption under concurrent writes |
| 4 | 2.3 — XXE vulnerability | Server file read by malicious clients |
| 5 | 1.3 — No Draw model | Tied game crashes server |
| 6 | 4.13 — Non-atomic file write | Power loss corrupts all user data |
| 7 | 2.2 — No TLS | Credentials sniffable on network |
| 8 | 1.2 — `getAge()` NPE | Crashes for users without DOB |
| 9 | 3.1 — Client thread-safety | Random client crashes |
| 10 | 1.9 — `StateMachineTest` dead code | Zero coverage for state machine |
