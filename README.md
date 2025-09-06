# Star-SFTP ✨

<p align="center">
  <img src="assets/logo.png" alt="Star SFTP Logo" width="200" />
</p>

<p align="center">
  <a href="https://www.java.com" title="Java Version"><img src="https://img.shields.io/badge/Java-17+-red" alt="Java 17"></a>
  <a href="LICENSE" title="MIT License"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT License"></a>
</p>

Star-SFTP ist ein moderner, erweiterbarer SFTP-Client in Java. Das Projekt befindet sich noch im Aufbau – helf mit und gestalte ihn mit!

## 📚 Inhaltsverzeichnis

- [🚀 Schnellstart](#-schnellstart)
- [🌐 Features](#-features)
- [🛠 Geplante Funktionen](#-geplante-funktionen)
- [🎨 Eigene Themes](#-eigene-themes)
- [🖥 Beispiel](#-beispiel)
- [📦 Installation](#-installation)
- [🧪 Entwicklung & Tests](#-entwicklung--tests)
- [❓ FAQ](#-faq)
- [🤝 Beitragen](#-beitragen)
- [📜 Lizenz](#-lizenz)

## 🚀 Schnellstart

```bash
./mvnw clean install
java -jar target/star-sftp.jar
```

## 🌐 Features

- Sicheres SFTP über SSH
- Konfigurierbare Shortcuts
- Anpassbare Benutzeroberfläche
- Drag & Drop Unterstützung
- Mehrere Sitzungen in Tabs
- Dunkle und helle Themes

## 🛠 Geplante Funktionen

| Status | Funktion | Beschreibung |
| ------ | -------- | ------------ |
| ✅ | Grundlegende SFTP-Operationen | Upload, Download, Löschen |
| 🟡 | Plug-in Architektur | Erweiterungen über Module |
| 🔁 | Synchronisation lokaler Ordner | Lokale und entfernte Verzeichnisse aktuell halten |
| 💡 | Skriptbare Aktionen | Automatisierung per Groovy/JS |
| 🧪 | Integrierter Terminal | Shell direkt in der Anwendung |
| 🔒 | SSH-Agent Unterstützung | Nutzung vorhandener SSH-Keys |

## 🎨 Eigene Themes

Star-SFTP besitzt ein Standard-Theme in `src/main/resources/ui/app.css`, doch du kannst problemlos eigene Themes erstellen.

```css
/* my-theme.css */
.themed-dialog {
  -fx-background-color: #202020;
  -fx-text-fill: #f9d71c;
}
```

Sende mir dein Theme per Pull-Request oder direkt per Mail an `star@example.com`.

## 🖥 Beispiel

```java
SftpService service = new SftpService();
service.connect("example.com", "user", "password");
service.upload(Path.of("/tmp/file.txt"), "/remote/file.txt");
```

## 📦 Installation

1. Lade das Release-JAR oder baue es selbst:

   ```bash
   ./mvnw package
   ```

2. Starte die Anwendung mit:

   ```bash
   java -jar target/star-sftp.jar
   ```

## 🧪 Entwicklung & Tests

```bash
./mvnw test
```

## ❓ FAQ

**Welche Java-Version wird benötigt?**  
Java 17 oder höher ist erforderlich.

**Wie kann ich mein Theme teilen?**  
Erstelle ein Issue oder sende einen Pull-Request mit deiner CSS-Datei.

## 🤝 Beitragen

1. Repository forken
2. Dein Theme oder Feature entwickeln
3. Pull-Request erstellen

## 📜 Lizenz

Dieses Projekt steht unter der MIT Lizenz.
