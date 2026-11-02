# Daycare Sleep Check Log 1.3.3 release record

- Package: `com.daycare.sleepcheck.log`
- Version: `1.3.3 (7)`; previous Play Production: `1.3.2 (6)`.
- Change: backup import now clears and restores all tables inside a Room transaction; malformed data rolls the operation back.
- Verification: `verifyApplicationIds`, `testDebugUnitTest` (25/25), `lintRelease`, `bundleRelease`, and `assembleRelease` passed. Full connected instrumentation previously passed twice on three devices (24 device test executions); the release branch carries the same tested restore fix.
- AAB SHA-256: `5e65e07fcf5f50cb7eaf9e83f1373ba4e2c2628ea0b0d471545a0d9076f35baf`.
- APK SHA-256: `d9005a899c0ea0dee811303e44d972a5202093d367f2d67d3c0c105d77ccb4a0`.
- Release certificate SHA-256: `06c7004132f9ff275e7039dd71f8adc5074090f660279dd4cb28e46ade4560ee`.
- Real childcare-provider workflow, purchase/restore billing and full entitlement matrix remain unverified; these are not described as passed by this release.
