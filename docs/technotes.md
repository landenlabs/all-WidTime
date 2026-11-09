# WidTime technical notes

How the widget keeps its clocks current, how it watches the battery and charger, and how the
charging features (pings, unplug alarm, bell mute, USB data check, spoken battery level) work.
Paths are under `app/src/main/java/com/landenlabs/widtime/`.

## Components

| Component | Kind | Role |
|---|---|---|
| `widget/TimeWidgetProvider` | `AppWidgetProvider` | Builds the widget (`RemoteViews`), owns the per-minute tick, draws battery bars and the charging / alarm panels. |
| `widget/CityRemoteViewsService` + `CityRemoteViewsFactory` | `RemoteViewsService` | Fills the city `ListView`; formats each city's time with `ZonedDateTime.now(zone)`. |
| `widget/BootReceiver` | manifest receiver | `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`: resets session state, restarts the monitor, re-sends `APPWIDGET_UPDATE` (which re-arms the tick). |
| `alerts/ChargeMonitorService` | foreground service (`specialUse`) | Registers at runtime for `POWER_CONNECTED`, `POWER_DISCONNECTED`, `BATTERY_CHANGED`, `USB_STATE`. |
| `alerts/ChargeAlerts` | static helper | All charging state and rules; state in SharedPreferences `charge_alerts`. |
| `alerts/ChargeAlertReceiver` | non-exported receiver | AlarmManager ping / alarm events, notification Stop, widget bell tap. |
| `alerts/SoundPlayer`, `alerts/Speaker` | helpers | Ping tone and alarm sound; text-to-speech announcements. |
| `util/ChargeLogic`, `util/DischargeEstimator` | pure Java | Unit-tested rules (ping intervals, speed labels, USB label, speak steps, discharge math). |
| `util/ChargingInfo`, `util/BatteryStatus` | readers | Snapshot of charger state; level plus time-left estimate. |

## Time updates

- **Per-minute tick.** `TimeWidgetProvider.scheduleTick` sets a repeating `AlarmManager.RTC` alarm
  (60 s, non-wakeup) that broadcasts `ACTION_TICK` to the provider. It's armed in `onEnabled` and
  `onUpdate`, and cancelled in `onDisabled` or when a tick finds no widgets.
- **On each tick:**
  1. `notifyAppWidgetViewDataChanged(ids, R.id.widget_list)` makes the factory's
     `onDataSetChanged` / `getViewAt` re-run, so every city row is re-formatted from the current
     time in its zone.
  2. `refreshAll` does a `partiallyUpdateAppWidget` of the battery bars and panels, and calls
     `BatteryStatus.refresh`, which also records a discharge sample (see below).
- **Why it's inexact.** `setRepeating` has been inexact since Android 4.4, and an `RTC` alarm
  doesn't wake the device. While the screen is off or the phone is dozing, ticks are deferred and
  then delivered when it wakes. That's acceptable because nobody is looking at the home screen
  then. The first tick after unlock brings the times up to date.
- **Backup.** `widget_time_info.xml` sets `updatePeriodMillis = 30 min`. The system's periodic
  `onUpdate` rebuilds the widget and re-arms the tick if it was lost.
- **After reboot or app update.** Alarms don't survive a reboot. `BootReceiver` re-sends
  `APPWIDGET_UPDATE` to re-arm the tick.
- **Settings changes** call `TimeWidgetProvider.updateAppWidget` directly for a full rebuild.

## Battery bars

- **Top bar (level).** Shown when the per-widget setting is on, and always shown while plugged in,
  whatever the setting (`applyBatteryBars(..., plugged)`). The fill is `setImageLevel` on a clip
  drawable. The color comes from `DischargeEstimator.colorForPercent`: green above 60%, yellow
  40–60%, red below 40%.
- **Bottom bar (time left, full = 24 h).** Android has no "time to empty" API, so
  `BatteryStatus.refresh` estimates one:
  - Each call while unplugged records a `(time, level)` sample. It keeps only level changes,
    drops samples older than 6 h, and resets the samples if the level went up.
  - The rate is trusted after at least a 2% drop over at least 10 min.
  - While charging, the last measured rate is applied to the current level.
  - Before any rate exists, it uses the charge counter ÷ current draw if the device reports them.
    Otherwise the bar shows an empty track.

## Charger monitoring

- **Why a foreground service.** Since Android 8, `ACTION_POWER_CONNECTED` and
  `ACTION_POWER_DISCONNECTED` are only delivered to receivers registered at runtime, so
  `ChargeMonitorService` stays running with a silent `IMPORTANCE_MIN` "Watching charger"
  notification.
- **When it runs.** `ChargeAlerts.syncService` starts it when at least one widget exists and
  either "Charging pings & unplug alarm" is on or "Speak battery %" is not Off. Otherwise it stops
  the service.
- **Android 12+ background starts.** Android 12 and later refuse foreground-service starts from
  the background. The `ForegroundServiceStartNotAllowedException` is swallowed. The real starts
  come from the settings screen, `BOOT_COMPLETED`, and `MY_PACKAGE_REPLACED`, which are all
  allowed.
- **Broadcasts it handles:**
  - `POWER_CONNECTED` calls `ChargeAlerts.onPowerConnected`.
  - `POWER_DISCONNECTED` calls `ChargeAlerts.onPowerDisconnected`.
  - `BATTERY_CHANGED`, only when the level or plug type changed: works out the percent from
    `EXTRA_LEVEL / EXTRA_SCALE`, calls `ChargeAlerts.onBatteryLevel` (speech), then `refreshAll`.
  - `android.hardware.usb.action.USB_STATE` calls `refreshAll` (USB data line, below).
- **Catch-up.** On start, `ChargeAlerts.reconcile` compares the real plug state with the saved
  session flag and replays a missed connect or disconnect.
- **State survives restarts.** It's all in SharedPreferences (`charge_alerts`), so events
  delivered by AlarmManager behave correctly after the process was killed. `onBoot` clears
  session, alarm, mute and speak state.

### Charging panel (widget)

- **When it shows.** It replaces the city list while plugged in, unless the unplug alarm is
  active, in which case the alarm panel wins. It takes the first visible city's color and size.
- **Layout.** Left-justified text column; bell `ImageView` on the right edge.
  - Headline: `⚡ 85% · Charging` or `Fully charged`.
  - Detail: speed, source and current, e.g. `Fast · USB · 1.85 A`.
    - Speed comes from `max_charging_current × max_charging_voltage` (AOSP SystemUI thresholds:
      under 5 W slow, 7.5 W+ fast, 20 W+ super fast). Without those, it falls back to measured
      current × battery voltage.
    - Current comes from `BATTERY_PROPERTY_CURRENT_NOW`, normalized because some devices report
      mA instead of µA.
  - USB data line (wired only), described below.
- **Bell sizing.** It's tinted with the text color. On API 31+ it's sized to 2× the text size
  (24 sp minimum) through `setViewLayoutWidth/Height`. Below API 31 it's a fixed 32 dp.

### USB data vs power only

- **Source.** `ChargingInfo.readUsbData` reads the sticky (hidden-API)
  `android.hardware.usb.action.USB_STATE` broadcast through `registerReceiver(null, filter)`.
- **What `connected` means.** The `connected` extra is true only once a USB host has enumerated
  the phone over the data lines. A charge-only cable or a wall charger never gets there.
- **Labels** (from `ChargeLogic.usbDataLabel`):
  - `Power + data · ADB, MTP`: connected, and these function extras are true. The functions
    checked are `adb`, `mtp`, `ptp`, `rndis`, `ncm`, `midi`, `uvc`, `accessory`.
  - `Power + data`: connected, but in "No data transfer" mode with debugging off.
  - `Power only · no data`: not connected.
  - Hidden: wireless charging, or the device never sent `USB_STATE`.
- **Live updates.** Enumeration lands about a second after plug-in, and the broadcast also fires
  on USB mode changes. Both trigger a refresh through the monitor service.
- **Limitation.** A wall charger or power bank always shows "Power only", even with a data cable,
  because there's no host to talk to. To check a cable, plug it into a computer.

## Charging pings and the unplug alarm

- **Pings.** On a new charging session, `onPowerConnected` sets `charging_session` and schedules
  `ACTION_PING`.
  - Intervals (`ChargeLogic.pingDelayMillis`): every 2 min × 5, then 5 min × 5, then every 10 min.
  - Scheduled with `setExactAndAllowWhileIdle` (Doze doesn't engage while charging).
  - `onPingDue` re-checks the plug, plays a `ToneGenerator` ping on the notification stream, and
    reschedules itself.
- **Unplug alarm.** `onPowerDisconnected` ends the session and sets `alarm_active`. It then:
  - plays the default alarm sound on the alarm stream for 5 s;
  - schedules the next round 5 min later with `setAlarmClock`, which wakes the device even in deep
    Doze;
  - posts a high-priority full-screen notification with a Stop action.

  It repeats until Stop (from the notification, `AlarmActivity`, or the widget's alarm panel) or
  until the charger is reconnected.
- **Exact alarms.** `SCHEDULE_EXACT_ALARM` is off by default on Android 14+. Without it,
  scheduling falls back to `setAndAllowWhileIdle`, which can run minutes late. Settings shows an
  "Allow exact alarms" button in that case.
- **Keeping the process alive.** `ChargeAlertReceiver` uses `goAsync()` to keep the process alive
  while a sound plays. `SoundPlayer` holds a partial wake lock for the length of the sound.

## Bell: mute for this charge

- **What it does.** Tapping the bell sends `ACTION_TOGGLE_MUTE` (broadcast `PendingIntent`, request
  code 5) to `ChargeAlertReceiver`, which flips `session_muted` and refreshes the widgets.
- **While muted:** pings, speech and the unplug alarm are all silent.
  - Muted pings keep their schedule, so unmuting resumes the cadence.
  - Muted speech steps still raise the high-water mark (below), so unmuting doesn't announce a
    stale level.
- **When it's cleared:** on disconnect (before deciding whether to alarm), at a new session start,
  on boot, and when the monitor service is stopped.
- **When it's hidden:** when neither pings/alarm nor speech is enabled
  (`ChargeAlerts.hasChargingSounds`).

## Speak battery %

- **Setting.** Charging section of settings: Off (default) or every 5 / 10 / 20 / 25%. It's
  app-wide (`speak_step`). Changing it resets the mark.
- **Rule** (in `ChargeLogic`, unit-tested). The mark is the highest `level / step` reached this
  charge (`speak_mark`, -1 = unarmed).
  - The first reading only arms the mark, so plugging in at 47% stays quiet until 50%.
  - A reading speaks when `level / step > mark`. It announces the actual level once:
    9 → 22 says "Charged to 22 percent", not 10 and then 20.
  - Drops never speak and never lower the mark, so a dip from 50 to 49 and back doesn't repeat.
  - 100% says "Fully charged".
  - The mark is reset when unplugged, on boot, and when the service stops.
  - Charging only: nothing is announced while discharging.
- **Trigger.** It runs from the monitor's `BATTERY_CHANGED` handler. Android reports only
  whole-percent levels.
- **Playback.** `Speaker` lazily creates `TextToSpeech` and holds a phrase requested during engine
  start-up.
  - Audio attributes are `USAGE_NOTIFICATION` / `CONTENT_TYPE_SPEECH`, so it follows the
    notification volume and Do Not Disturb.
  - It uses the default locale's voice. If the engine or language data is missing, it plays a
    ping instead.
  - A ping due within 60 s of an announcement is silent so they don't overlap.
  - The engine is shut down in the service's `onDestroy`.
- **Manifest.** `<queries>` for `android.intent.action.TTS_SERVICE` is required on Android 11+
  (package visibility) for `TextToSpeech` to find the engine. It's not a permission.

## Testing tips

The commands below fake the battery level:

```
adb shell dumpsys battery set ac 1
adb shell dumpsys battery set level 9
adb shell dumpsys battery set level 22
adb shell dumpsys battery reset
```

The `set ac 1` line keeps the phone reporting itself as plugged in while the level is faked.

To inspect the USB state, alarms and the monitor service:

```
adb shell dumpsys usb | grep -i -E 'connected|functions'
adb shell dumpsys alarm | grep -A3 com.landenlabs.widtime
adb shell dumpsys activity services com.landenlabs.widtime
```

Unit tests (`ChargeLogicTest`, `DischargeEstimatorTest`, …) cover the pure rules:
`./gradlew :app:testDebugUnitTest`.
