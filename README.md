# firetv-launcher

Eigener, schlanker Launcher für den Fire TV Stick: Kacheln für Plex, Prime Video, Netflix, Disney+, YouTube (SmartTube) und Browser,
oben Uhr und Infos aus Home Assistant, dazu das Zuhause-Dashboard im Vollbild.

- Bedienung komplett per D-Pad.
- Home-Assistant-URL und Token werden in der App (Einstellungen-Kachel) eingegeben und nur lokal gespeichert. Es steht kein Geheimnis im Code.
- Build: GitHub Actions (`.github/workflows/build.yml`), Ergebnis als Release `latest` (Debug-APK).

Rückweg: App deinstallieren (Einstellungen → Anwendungen) und ADB-Umleitung der Home-Taste entfernen.
