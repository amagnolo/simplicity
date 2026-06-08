# Download &amp; Install SimpliCity

All downloads live on the **[releases page](https://github.com/amagnolo/simplicity/releases)**. Open the latest release and pick the file for your system from the **Assets** list.

[![Latest release](https://img.shields.io/github/v/release/amagnolo/simplicity?label=latest&amp;color=159957)](https://github.com/amagnolo/simplicity/releases/latest) [![Total downloads](https://img.shields.io/github/downloads/amagnolo/simplicity/total?label=downloads&amp;color=159957)](https://github.com/amagnolo/simplicity/releases)

Every installer and the portable build come with their own bundled Java runtime, so **you do not need to install Java** - just download and run. (The only exception is the universal `.jar`, see below.)

---

## Pick your download

| Your system                      | Download | What to do |
|----------------------------------|---|---|
| **Linux** (Debian / Ubuntu)      | `simplicity_<version>_amd64.deb` | `sudo apt install ./simplicity_<version>_amd64.deb` |
| **Linux** (Fedora / RHEL)        | `simplicity-<version>.x86_64.rpm` | `sudo dnf install ./simplicity-<version>.x86_64.rpm` |
| **Windows** (installer)          | `SimpliCity-<version>.msi` | Double-click and follow the wizard. Adds a Start-menu shortcut. |
| **Windows** (portable)           | `SimpliCity-<version>-windows-portable.zip` | Extract anywhere, then run **`SimpliCity.exe`**. Nothing is installed. |
| **macOS** (Apple Silicon, M1–M5) | `SimpliCity-<version>-arm64.dmg` | Open the `.dmg`, drag **SimpliCity** into **Applications**. |
| **macOS** (Intel)                | `SimpliCity-<version>-x64.dmg` | Open the `.dmg`, drag **SimpliCity** into **Applications**. |
| **Any** (needs Java 25)          | `SimpliCity-<version>.jar` | `java -jar SimpliCity-<version>.jar` |

> Not sure which Mac you have? Click the Apple menu → About This Mac. A "Chip" starting with **Apple M** means Apple Silicon; an **Intel** processor means the Intel build.

---

## Universal `.jar` (advanced)

If you already have **Java 25** (or newer) installed, the small platform-independent jar runs quickly everywhere:

```bash
java -jar SimpliCity-<version>.jar
```

Don't have Java? Get a free build from [Adoptium / Eclipse Temurin](https://adoptium.net/temurin/releases/?version=25). The installers and the portable zip above already include Java, so this step is only needed for the bare jar.

---

## First launch: "unknown developer" warnings

SimpliCity is a free hobby project and the builds are **not code-signed**, so your OS may warn you the first time. This is expected — here's how to proceed:

- **Windows** — if you see *"Windows protected your PC"*, click **More info → Run anyway**.
- **macOS** — if macOS says the app *"cannot be opened because the developer cannot be verified"*, **right-click (or Control-click) the app → Open**, then confirm. You only need to do this once. (Alternatively: **System Settings → Privacy &amp; Security → Open Anyway**.)
- **Linux** — no warning; the `.deb` / `.rpm` install normally.

---

## Uninstalling

- **Windows** — *Settings → Apps → Installed apps → SimpliCity → Uninstall* (or just delete the extracted folder for the portable build).
- **macOS** — drag **SimpliCity** from Applications to the Trash.
- **Linux** — `sudo apt remove simplicity` or `sudo dnf remove simplicity`.

---

Found a problem? Please report it on the [GitHub issues page](https://github.com/amagnolo/simplicity/issues).

![SimpliCity logo](images/logo_universal_transparent.png)
