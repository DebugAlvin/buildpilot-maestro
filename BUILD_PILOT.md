# BuildPilot capture distribution

Status: local maintained patch, based on upstream `cli-2.11.0`, commit
`c8c361be1cc89b2e742f185466a302a22778be6b`. Apache-2.0 licensing and upstream
copyright notices are retained. Version: `2.11.0-buildpilot.1`.

Changes:

- `test --capture-step-screenshots` captures command boundaries without AI Insights.
- `test --[no-]capture-screen-recording` controls automatic full-run recording;
  when unspecified, it retains upstream `--analyze` behavior.
- Step screenshot metadata records `phase` and `capturedAtEpochMs` only for the new
  screenshot mode. A failed replacement retains the preceding frame's phase.
- `--buildpilot-capabilities` is a local-only discovery entry point before network,
  analytics, dependency or device initialization.

Existing Flow commands, drivers, assertions, retry and JUnit behavior use upstream
implementations. Successful frames are pre-command; failures/warnings replace them
only when capture succeeds. `screenshots/final.png` is after completion hooks.

Build with JDK 17, Gradle wrapper 8.13 and the Node version specified by `.nvmrc`:

```sh
./gradlew :maestro-orchestra:test --tests 'maestro.orchestra.debug.*' \
  :maestro-cli:installDist -PCLI_VERSION=2.11.0-buildpilot.1 --no-daemon
```

The complete CLI build includes upstream platform resources. Building those
resources does not certify iOS or Web execution. BuildPilot presently validates
this patch using Flutter/Android only.

Keep the checked-in driver binaries unchanged. BuildPilot's `tools/maestro`
contains the portable patch, packaging utility, package checksums and rollout
instructions. Never replace the host-wide Maestro installation: Flow uses an
explicit package path and exploration retains its separately validated CLI.

Before upgrading, replay the patch onto a fixed upstream commit, rerun the
artifact tests and the Android exit-code/JUnit/media comparison, then validate
BuildPilot readers and rollback. If upstream gains compatible independent capture
flags, remove the matching patch after that same comparison. Changes to execution
scheduling, assertions, retries or drivers require a new architecture decision.
