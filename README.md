# GramoBudík

Jednoduchá Android aplikace (Kotlin + Jetpack Compose + Material 3) s ikonou gramofonové desky s notami, která v nastavený čas spustí vámi vybranou písničku vždy na **100 % hlasitosti** — i když je vypnutý displej — a umožní ji kdykoli snadno zastavit.

## Hlavní funkce

- **Ikona gramofonové desky s notami** – vlastní adaptivní vektorová ikona (`ic_launcher_foreground.xml` / `ic_launcher_background.xml`) i notifikační ikona (`ic_notification_vinyl.xml`).
- **Více nezávislých budíků** – přidejte libovolný počet budíků s vlastním časem, písničkou, zapnutím/vypnutím i smazáním.
- **Plánování Pondělí – Neděle (`Po`–`Ne`)** – opakování v libovolné dny v týdnu nebo jednorázové spuštění, včetně automatického obnovení po restartu telefonu (`BootReceiver`).
- **Volba vibrací** – možnost zapnout či vypnout vibrace pro každý budík zvlášť (`VIBRATE` / `USAGE_ALARM`).
- **Výběr libovolné písničky z telefonu** – přes systémový dialog pro výběr zvuku (`audio/*`) s trvalým přístupovým oprávněním (`takePersistableUriPermission`).
- **Přesný čas spuštění** – 24hodinový výběr času (`TimePicker`) naplánovaný přes `AlarmManager.setAlarmClock`.
- **Spuštění i při vypnutém displeji** – kombinace `setAlarmClock`, `PARTIAL_WAKE_LOCK`, foreground služby `PlaybackService` (`mediaPlayback`) a celoobrazovkové zamykací aktivity `AlarmRingingActivity` (`setShowWhenLocked`, `setTurnScreenOn`).
- **Vždy 100 % hlasitost** – před spuštěním `MediaPlayer` (`USAGE_ALARM`) se hlasitost systémového proudu budíku (`AudioManager.STREAM_ALARM`) automaticky nastaví na maximum (`getStreamMaxVolume`).
- **Snadné zastavení** – velké červené tlačítko **ZASTAVIT PÍSNIČKU** na zamykací obrazovce, v hlavní obrazovce aplikace i akce **Zastavit** v oznamovací liště.
- **Okamžité vyzkoušení** – tlačítko **Vyzkoušet** u každého budíku pro ověření hlasitosti, vibrací a přehrávání předem.

## Požadavky a oprávnění

- **Android 8.0 Oreo (API 26)** nebo novější (`targetSdk = 37`).
- Po spuštění aplikace doporučujeme potvrdit karty oprávnění (pokud se zobrazí):
  - **Povolit oznámení** (`POST_NOTIFICATIONS`, Android 13+) – pro zobrazení oznámení s tlačítkem **Zastavit**.
  - **Povolit přesný budík** (`SCHEDULE_EXACT_ALARM`, Android 12+) – pro přesné spuštění v nastavený čas i při uspaném telefonu.
  - **Optimalizace baterie – Bez omezení** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) – aby systém proces budíku neuspal při vypnuté obrazovce.

## Nastavení oprávnění v Xiaomi HyperOS / MIUI

Telefony Xiaomi, Redmi a POCO s nadstavbou **HyperOS / MIUI** agresivně omezují aplikace na pozadí, když je vypnutý displej. Pro 100% spolehlivost nastavte v **Nastavení → Aplikace → Správa aplikací → GramoBudík** (přímo z aplikace přes tlačítko **Otevřít nastavení aplikace** v kartě *Návod pro HyperOS / Xiaomi*):

1. **Automatické spuštění na pozadí (*Autostart*)** → **Zapnout**  
   Zajišťuje, že systém nezablokuje `AlarmReceiver` a spuštění přehrávání po uzamčení telefonu.
2. **Baterie / Spořič baterie** → vybrat **Bez omezení**  
   Zabraňuje správci napájení (`SmartPower`) uspat foreground službu `PlaybackService` při déle vypnuté obrazovce.
3. **Další oprávnění** → nastavit na **Vždy povolit**:
   - **Zobrazovat na zamykací obrazovce** – umožní zobrazit celoobrazovkovou obrazovku budíku s velkým červeným tlačítkem **ZASTAVIT PÍSNIČKU** nad zamčeným displejem.
   - **Spouštět vyskakovací okna na pozadí** – umožní systému otevřít obrazovku budíku i v případě, že telefon leží se zhasnutou obrazovkou.
4. **Oznámení** → **Povolit** (včetně zamykací obrazovky) a **Alarmy a připomenutí** → **Povolit**  
   Zaručí přesné buzení (`setAlarmClock`) i možnost zastavit písničku přímo z notifikační lišty.

## Instalace

### Z GitHub Releases (doporučeno)
1. Otevřete sekci [Releases](https://github.com/PatrikKM/GramoBudik/releases) a stáhněte nejnovější podepsaný soubor `GramoBudik-v*-signed.apk`.
2. Otevřete stažené APK v telefonu se systémem Android a povolte instalaci z neznámých zdrojů, pokud systém vyzve.

### Sestavení ze zdrojového kódu
```bash
git clone https://github.com/PatrikKM/GramoBudik.git
cd GramoBudik
./gradlew assembleDebug
```
Na Windows v PowerShellu případně nastavte `JAVA_HOME` (JDK 17–25, např. přibalený JBR z Android Studia) a spusťte `.\gradlew.bat assembleDebug`.

## Digitální podpis release sestavení

Projekt podporuje podepisování release artefaktů (`.apk` i `.aab`) přes lokální soubor `keystore.properties` (není součástí Gitu):

1. Zkopírujte šablonu `keystore.properties.example` na `keystore.properties`:
   ```properties
   storeFile=gramobudik-release.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=gramobudik
   keyPassword=YOUR_KEY_PASSWORD
   ```
2. Vytvořte vlastní podpisový klíč (`keytool`) nebo použijte svůj stávající `.jks` keystore:
   ```bash
   keytool -genkeypair -v -keystore gramobudik-release.jks -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 10000 -alias gramobudik
   ```
3. Sestavte podepsané release balíčky:
   ```bash
   ./gradlew assembleRelease bundleRelease
   ```
   Výstupy budou uloženy v `app/build/outputs/apk/release/app-release.apk` a `app/build/outputs/bundle/release/app-release.aab`.

> **Upozornění:** Soubory `*.jks`, `*.keystore` a `keystore.properties` jsou uvedeny v `.gitignore`, aby nikdy neunikly do veřejného repozitáře.

## Licence

Tento projekt je licencován pod licencí **MIT** – podrobnosti najdete v souboru [LICENSE](LICENSE).
