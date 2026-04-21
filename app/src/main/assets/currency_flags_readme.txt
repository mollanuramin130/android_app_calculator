Currency flag SVGs (bundled)
============================

Files use **ISO 3166-1 alpha-2** keys:

  `currency_flags/<cc>.svg`   (lowercase, e.g. `us.svg`, `in.svg`, `eu.svg`)

Source set: **4×3** aspect ratio (ISO flag SVGs). UI frames and SVG render sizes use 4:3 (see `currency_flag_render_*` in `values/dimens.xml`).

Mapping:

- Each `CurrencyItem` uses `flag` from `assets/currencies_iso4217.json` when present (2-letter code).
- Otherwise `CurrencyRegistry.guessFlagCountryCode(iso4217)` supplies a country code (e.g. USD→`us`, EUR→`eu`).
- `CurrencySvgFlagLoader` renders with AndroidSVG at width×height from dimens; missing/invalid files fall back to `CurrencyFlagResolver` drawables.
