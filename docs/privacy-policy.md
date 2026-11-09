---
# GitHub Pages (Jekyll) front matter: publishes this file as
# https://landenlabs.github.io/all-WidTime/privacy-policy.html - the URL given to Google Play.
layout: default
title: All World Time Privacy Policy
permalink: /privacy-policy.html
---

# All World Time Privacy Policy

_Last updated: 2026-11-08_

All World Time ("the app", also called WidTime) is a home screen clock widget published by LanDen Labs. This policy explains what the app does with data. The app has no user account, shows no ads, does not use an advertising ID and does not sell or share data.

## City search is the only network use
When you type in the **Add city** screen, the city name you type (the part before any comma, for example "Salem" from "Salem, NH") is sent over HTTPS to the free [Open-Meteo geocoding service](https://open-meteo.com/en/docs/geocoding-api) to find matching cities, their coordinates and time zones. As with any web request, Open-Meteo receives your device's IP address; see the [Open-Meteo terms and privacy policy](https://open-meteo.com/en/terms). Nothing else is sent: no location, widget settings, battery data, advertising ID or other identifiers. If the search can't be reached, the app falls back to its built-in city list.

The app contains no analytics, crash reporting or advertising libraries. We never receive any of your data.

## Data stored only on your device
- **Widget settings:** the cities and time zones you add, text and background colors, text size, the date/time format and which battery bars are shown, saved separately for each widget.
- **Battery history:** recent battery level readings, taken while the phone is unplugged, used to estimate the battery time left. Old readings are discarded automatically.
- **Charging alerts:** whether charging pings and the unplug alarm are on, and whether a charging session or alarm is in progress.

Cities you pick are stored on your device only. The app does not read your location.

If Android backup is turned on for your device, Android may back up these settings to your Google account under Google's own privacy policy. We cannot see that backup.

## Permissions
- **Internet:** used only for the city search described above
- **Notifications:** shows the unplug alarm, with its Stop button, and the small "Watching charger" notification while charging alerts are on
- **Foreground service (special use):** keeps a small service running so the app is told the moment the charger is connected or disconnected
- **Alarms & reminders (exact alarms):** plays the charging pings and repeats the unplug alarm on time
- **Full-screen notifications:** shows the unplug alarm screen when the phone is locked
- **Wake lock:** keeps the phone awake for the few seconds a ping or alarm sound plays
- **Run at startup:** restarts the widget clock and the charger watch after the phone restarts

The app does not use location, camera, microphone, contacts, photos or storage permissions.

## Children
The app is not directed to children under 13.

## Retention and deletion
Removing a widget from your home screen deletes that widget's settings. All remaining data is removed when you clear the app's storage or uninstall the app.

## Changes
We will update this page when the policy changes and revise the date above.

## Contact
Email: landenlabs.app@gmail.com
