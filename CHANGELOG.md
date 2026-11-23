# Changelog

Všechny významné změny v aplikaci **GramoBudík** jsou zaznamenány v tomto souboru.

## [1.0.0] - 2026-11-23

### Přidáno
- Adaptivní vektorová ikona aplikace zobrazující gramofonovou desku s notami (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`) a monochromatická notifikační ikona (`ic_notification_vinyl.xml`).
- Výběr vlastní zvukové stopy z telefonu pomocí `OpenDocument` (`audio/*`) s trvalým oprávněním ke čtení (`takePersistableUriPermission`).
- Nastavení času spuštění pomocí 24hodinového dialogu `TimePicker` a přesného plánování přes `AlarmManager.setAlarmClock`.
- Spuštění písničky i při vypnutém displeji a v režimu Doze pomocí `AlarmReceiver`, `PARTIAL_WAKE_LOCK`, foreground služby `PlaybackService` (`mediaPlayback`) a obnovení budíku po restartu zařízení (`BootReceiver`).
- Automatické nastavení hlasitosti streamu budíku (`AudioManager.STREAM_ALARM`) vždy na 100 % (`getStreamMaxVolume`) před každým spuštěním přehrávání.
- Možnost okamžitého zastavení přehrávání ze zamykací obrazovky (`AlarmRingingActivity`), z banneru v hlavní aplikaci (`MainActivity`) nebo tlačítkem **Zastavit** přímo v oznámení.
- Konfigurace digitálního podpisu release sestavení (`.apk` i `.aab`) pomocí `keystore.properties`.
