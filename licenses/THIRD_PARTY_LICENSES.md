# Third-party licenses

SkySens itself is licensed under the MIT License (see `LICENSE.txt`). The release jar bundles the
following third-party library, which keeps its own license.

## MoulConfig

- Copyright (C) NotEnoughUpdates contributors
- License: GNU Lesser General Public License v3.0 or later
  (`MoulConfig-LGPL-3.0.txt`, which builds on `MoulConfig-GPL-3.0.txt`)
- Source: https://github.com/NotEnoughUpdates/MoulConfig
- Bundled as `org.notenoughupdates.moulconfig:modern-26.2`, unmodified, with its packages relocated from
  `io.github.notenoughupdates.moulconfig` to `de.vantrex.skysens.dependencies.moulconfig`.

You may replace the bundled MoulConfig with a modified version by rebuilding SkySens from its source
against your copy of MoulConfig.
