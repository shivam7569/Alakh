# Alakh Readiness Score — design spec (v1)

**Status:** design only — implemented in **Phase 5**, not yet built.
**Scope:** computed entirely on the **phone**, shown **only** in the phone app, **independent** of Google/Fitbit's proprietary readiness score.

> Provenance note: this spec came from a research pass in which 2 of 3 web-research agents failed, so it leans partly on model knowledge + the surviving load-research + a verification pass. The math matches established methods, but **sanity-check the constants and references when we actually implement it.**

## Philosophy

A transparent **0–100** score from two channels, combined by taking the **worse** of the two (`min`), all normalized against the user's **own rolling baseline** (z-scores) — never population thresholds.

- **Recovery channel** — overnight physiology (HRV, resting HR, sleep), read from Health Connect.
- **Load channel** — the app's *own* logged training load (which Google can't see). A load spike can dampen readiness *before* the autonomic markers crash.

Using `min()` means a bad-recovery night can't be averaged away by an easy training week, and a load spike can't be hidden by good sleep.

## Inputs

| Input | Source | Normalization | Weight |
|---|---|---|---|
| Overnight HRV (RMSSD, ms) | Health Connect `HeartRateVariabilityRmssd` | z-score vs robust 14–21d baseline; higher = better | 0.50 (recovery) |
| Resting HR (bpm) | Health Connect `RestingHeartRate` | z-score; **lower = better** (`z_rhr = -z`) | 0.30 (recovery) |
| Sleep (got-vs-needed or total min) | Health Connect `SleepSessionRecord` | z-score; higher = better | 0.20 (recovery) |
| Internal load: sRPE × duration (AU) | **Our own** logged sessions (session RPE on Borg CR-10 × minutes) | EWMA acute/chronic → ACWR → penalty | drives load channel |
| External load: tonnage (sets×reps×kg) | **Our own** logged sets | **displayed only** in v1 | 0.0 (shown, not scored) |

## The formula

**Step 1 — Recovery channel.** Per metric, keep a robust rolling baseline (median for μ, `1.4826 × MAD` for σ) over a 14–21 day window *excluding today*. Compute `z = (today − μ)/σ`, clamp to `[-3, +3]`, orient so positive = better recovery.

```
Z_recovery = (0.50·z_hrv + 0.30·z_rhr + 0.20·z_sleep) / (Σ weights present)
Recovery_score = 100 / (1 + exp(-1.2 · Z_recovery))   # z=0→50, z=+1.5→~86, z=-1.5→~14
```
(Renormalize weights over whichever metrics are present that night.)

**Step 2 — Load channel.** Daily load series (0 on rest days), two EWMAs:
```
λ_acute = 2/(7+1) = 0.25      λ_chronic = 2/(28+1) = 0.069
EWMA_today = Load_today·λ + (1-λ)·EWMA_yesterday
ACWR = EWMA_acute / EWMA_chronic     # "uncoupled" (exclude last 7d from chronic) preferred
```
Ramped penalty (no magic cliff):
```
ACWR ≤ 1.3            → penalty_acwr = 0          (well-tolerated band)
1.3 < ACWR ≤ 1.8      → ramp 0 → 30 linearly
ACWR > 1.8            → 30 (cap)
spike = Load_today / (EWMA_chronic + ε)
penalty_spike = clamp((spike - 1.5)·20, 0, 30)
penalty_load = max(penalty_acwr, penalty_spike)
```

**Step 3 — Combine (gate on the worse channel).**
```
Load_score = 100 - penalty_load
Readiness  = round( min(Recovery_score, Load_score) )
UI buckets: ≥67 Green (push) · 34–66 Amber (maintain) · <34 Red (back off)
```

**Transparency (the differentiator).** Always show *why*: e.g. *"Readiness 48 (Amber) — limited by RECOVERY: HRV 12% below your 14-day baseline (z=-1.6)"* or *"Readiness 55 — limited by LOAD: training climbing faster than your 4-week base (ACWR 1.6)."* Show each input's value, personal baseline, and z. Let the user tune weights in v1.1.

## Cold start (first ~2–3 weeks)

- **Days 0–6:** no numeric score. Show raw inputs + "building your baseline — N/14 nights." Hide ACWR (undefined).
- **Days 7–13:** optional *provisional* low-confidence score (z-scores once ≥7 nights/metric). Load channel uses only the same-day spike penalty until the chronic EWMA has ~21–28 days.
- **Days 14+:** full score; ACWR available ~day 21–28.
- **Seeding:** seed each EWMA with the mean of its first days (not 0); optionally blend gentle population priors decaying to 0 as personal data accrues. Missing night = skip & renormalize, never impute 0.

## Caveats (do not skip)

- **Not a medical or injury-prediction device.** Never present population injury probabilities as personal predictions.
- **ACWR is statistically contested** (mathematical coupling, a null cluster-RCT, high meta-analysis heterogeneity). Use it only as a soft *monitoring/explanation* nudge; let overnight physiology be the harder gate.
- **sRPE behaves differently in strength** than endurance (HR dissociates from heavy low-rep effort). Collect RPE ~15–30 min post-session for the whole session.
- **Single-night readings are noisy** — the personal rolling baseline + robust stats (median/MAD) + z-clamping are what make it safe.
- The exact weights (0.50/0.30/0.20), logistic slope (1.2), and penalty ramps are **tunable defaults**, not validated constants — calibrate against the user's felt experience.
- This is an **n=1 personal** model — don't generalize across users.

## References (verify on implementation)

- Foster et al. — session-RPE (sRPE × duration) internal load.
- Haddad et al. 2017, *Frontiers in Neuroscience* (PMC5673663) — sRPE validity; weak HR-concurrence in strength.
- Williams/Murray et al. 2017 — EWMA ACWR method.
- Impellizzeri et al. 2020, *Sports Medicine* — "ACWR: Conceptual Issues and Fundamental Pitfalls."
- Bjørneboe/Bahr et al. 2020 — cluster-RCT: ACWR-guided management did **not** reduce injuries (PMID 33036995).
- 2025 BMC meta-analysis (PMC12487117) — combined ES 0.72, high heterogeneity, "use with caution."
- RMSSD overnight reliability ICC ~0.92–0.94 — MDPI *Sensors*.
- WHOOP Recovery design (two-channel + gate on worse) — vendor heuristic, not peer-reviewed.
