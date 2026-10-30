# Bibliotheksverwaltung – Microservices

[![CI](https://github.com/Albatros46/library-management/actions/workflows/ci.yml/badge.svg)](https://github.com/Albatros46/library-management/actions/workflows/ci.yml)

> 🇹🇷 Türkçe sürüm: [README.tr.md](README.tr.md)

Ein Bibliothekssystem für das Personal, gebaut als Microservice-Architektur mit Spring Boot 3.5, MongoDB und React (Vite). Das Personal verwaltet Mitglieder und den Katalog, gibt Ausleihen und Rückgaben frei und behält überfällige Bücher im Blick. Mitglieder erhalten Erinnerungen und Mahnungen per E-Mail. Die Oberfläche ist auf Türkisch.

## Architektur

```
React (5173 / 3000) ──► API-Gateway :8080 ──┬─► auth-service         :8081  staff_users
   (JWT Bearer)          prüft JWT,          ├─► member-service       :8082  members
                         setzt X-User-*      ├─► catalog-service      :8083  books, authors, publishers
                                             ├─► loan-service         :8084  loans
                                             └─► notification-service :8085  notification_logs
loan-service ──REST──► member-service, catalog-service (Mitgliedsprüfung, Bestand reservieren/zurückbuchen)
notification-service ──REST──► loan-service, member-service ──SMTP──► E-Mail
```

| Service | Aufgabe |
|---|---|
| **api-gateway** | Spring Cloud Gateway (WebMVC). Leitet `/api/**` weiter, prüft das JWT und gibt den Benutzer als `X-User-Id/Name/Role`-Header an die Services weiter. Vom Client gesendete `X-User-*`-Header werden verworfen; interne Endpunkte (`/books/*/checkout`, `/books/*/return`, `/loans/*/reminded`) sind von außen gesperrt (403). |
| **auth-service** | Personalkonten (ADMIN / STAFF), BCrypt-Passwörter, Ausgabe des JWT. Legt beim ersten Start ein Admin-Konto an. |
| **member-service** | Mitglieder: Vor- und Nachname, E-Mail, Telefon, Adresse, Status, Erinnerungs-Einstellung. Suche. |
| **catalog-service** | Bücher, Autoren, Verlage, Kategorien. Autorenseite mit Biografie und allen Büchern. Exemplare und Verfügbarkeit (atomar per `findAndModify`). |
| **loan-service** | Ausleih-Workflow mit Freigaben, Fälligkeitsdaten, Liste der überfälligen Ausleihen, Leseprofil pro Mitglied. |
| **notification-service** | Täglich um 09:00: Erinnerung kurz vor dem Fälligkeitsdatum, Mahnung bei Überfälligkeit (höchstens alle 3 Tage pro Ausleihe). Manuelle Mahnung und Benachrichtigungsprotokoll. |

Alle Services nutzen die Datenbank `mongodb://localhost:27017/library_management`; jeder Service schreibt nur in seine eigenen Collections (siehe oben). Soll ein Service eine eigene Datenbank bekommen, genügt eine andere `MONGO_URI`. Die Services sprechen in dieser Version per REST miteinander; ein Umstieg auf Kafka/RabbitMQ-Events ist möglich.

## Ausleih-Workflow

```
PENDING ──freigeben──► ACTIVE ──Fälligkeit überschritten──► OVERDUE
   │                     │  ▲                                   │
ablehnen          Rückgabe│  │abbrechen                  Rückgabe│
   ▼                     ▼  │                                   ▼
REJECTED          RETURN_PENDING ──Rückgabe freigeben──────► RETURNED
```

- Das Personal legt eine Ausleihe an (**PENDING**). Erst mit der **Freigabe** wird ein Exemplar abgebucht und die Leihfrist beginnt.
- Rückgabe: „İade al“ setzt die Ausleihe auf **RETURN_PENDING**, nach der Freigabe geht das Exemplar zurück in den Katalog. Beides geht auch in einem Schritt.
- Regeln: Das Mitglied muss aktiv sein; wer ein überfälliges Buch hat, bekommt kein neues; höchstens 5 offene Ausleihen; kein Buch doppelt; einmal verlängern (`loan-service/application.yml`).
- Ausleihen mit überschrittenem Fälligkeitsdatum werden alle 15 Minuten auf **OVERDUE** gesetzt und erscheinen in einer eigenen Liste („Gecikenler“).

## Oberfläche

Übersicht · Ausleihen (offene Freigaben, ausgeliehene Bücher) · Überfällige (Mahnstatus, manuelle Mahnung, Rückgabe) · Mitglieder / Mitgliedsdetails (Kontaktdaten, aktuelle Bücher mit Fälligkeit, Verlauf, **Anzahl Bücher, Seiten und Kategorien**) · Katalog / Buchdetails (Autor, Seitenzahl, Verlag, Kategorie, Verfügbarkeit, wer das Buch hat) · Autoren / Autorendetails (Infos + Bücherliste) · Benachrichtigungen · Personal (nur Admin).

> Das Leseprofil wird aus den Kategorien der ausgeliehenen Bücher berechnet; es zeigt nicht, ob ein Buch tatsächlich gelesen wurde.

## Starten

### Docker Compose (empfohlen)

```bash
docker compose up --build
```

- Oberfläche: http://localhost:3000
- Gateway: http://localhost:8080
- Versendete E-Mails (Mailpit): http://localhost:8025
- Login: `admin@library.local` / `admin123` (änderbar über `ADMIN_EMAIL`, `ADMIN_PASSWORD`)

Für einen echten SMTP-Server die Umgebungsvariablen `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true` und `MAIL_FROM` setzen. In Produktion unbedingt `JWT_SECRET` (mindestens 32 Byte) setzen.

### Ein Befehl im Terminal (IntelliJ-Terminal / PowerShell)

```powershell
.\start-all.ps1
```

Startet MongoDB und Mailpit per Docker, baut mit Maven, startet die 6 Services und die Vite-Oberfläche im Hintergrund und gibt die Adressen aus, sobald alles bereit ist. Logs liegen in `logs\`.

| Befehl | Wirkung |
|---|---|
| `.\start-all.ps1 -SkipBuild` | Ohne Build, mit den vorhandenen JARs (schneller) |
| `.\start-all.ps1 -NoFrontend` | Nur Backend |
| `.\stop-all.ps1` | Stoppt alles (inkl. MongoDB und Mailpit) |
| `.\stop-all.ps1 -KeepInfra` | Stoppt die Services, MongoDB und Mailpit laufen weiter |

Ist ein Port belegt (z. B. weil der komplette Stack per `docker compose up` läuft), startet das Skript nichts und meldet es; vorher `docker compose down` ausführen.

### IntelliJ IDEA mit einem Klick

Die Run-Konfigurationen in `.run/` erscheinen beim Öffnen des Projekts automatisch. In der Liste oben **All Services** wählen und ▶ (Run) oder 🐞 (Debug) klicken:

1. `infra (MongoDB + Mailpit)`: `docker compose up -d mongo mailpit` (Docker Desktop muss laufen)
2. `auth-service`, `member-service`, `catalog-service`, `loan-service`, `notification-service`, `api-gateway`
3. `frontend (Vite)`: http://localhost:5173

Einzelne Services im Fenster **Services** (Alt+8) steuern. Die MongoDB- und Mailpit-Container stoppen nicht mit Stop, sondern mit `docker compose stop`.

### Manuell

Voraussetzungen: JDK 21+, Maven, Node 20+, MongoDB (27017) und ein SMTP-Server (z. B. `docker run -p 1025:1025 -p 8025:8025 axllent/mailpit`).

```bash
mvn package -DskipTests
java -jar auth-service/target/app.jar
java -jar member-service/target/app.jar
java -jar catalog-service/target/app.jar
java -jar loan-service/target/app.jar
java -jar notification-service/target/app.jar
java -jar api-gateway/target/app.jar
cd frontend && npm install && npm run dev     # http://localhost:5173
```

Beim ersten Start werden Beispieldaten geladen (3 Mitglieder, 4 Autoren, 3 Verlage, 8 Bücher).

## Tests und CI

`e2e/smoke_test.py` prüft den laufenden Stack von außen durch das Frontend (nginx → Gateway → Services) mit 39 Prüfungen:

- Sicherheit: ohne Token 401, falsches Passwort 401, interne Endpunkte 403, gefälschter `X-User-Role`-Header wirkungslos, nur Admins legen Personal an
- Katalog: Autorenseite mit allen Büchern, Suche nach Autorenname
- Ausleihe: Antrag → Freigabe → Bestand sinkt; doppelte Ausleihe wird abgelehnt (409); das Buch zeigt, wer es hat
- Rückgabe: Rückgabe → Freigabe → Bestand steigt wieder
- Leseprofil: Anzahl Bücher, Seiten und Kategorien
- Benachrichtigung: Mahnung wird gesendet, in der Ausleihe vermerkt und kommt in Mailpit an

```bash
docker compose up -d --build
BASE_URL=http://localhost:3000 python e2e/smoke_test.py
```

Der Test erwartet eine frische Datenbank mit den Beispieldaten.

GitHub Actions (`.github/workflows/ci.yml`) baut bei jedem Push alle Services und das Frontend, startet anschließend den kompletten Stack mit Docker Compose und führt den End-to-End-Test aus.

## Wichtige API-Endpunkte (über das Gateway)

| Methode | Pfad | Beschreibung |
|---|---|---|
| POST | `/api/auth/login` | Login, liefert das JWT |
| GET/POST/PUT | `/api/members`, `/api/members/{id}` | Mitglieder (`?q=` Suche) |
| GET/POST/PUT/DELETE | `/api/books`, `/api/books/{id}` | Bücher (`?q=&category=&authorId=&availableOnly=`) |
| GET | `/api/books/categories` | Kategorien |
| GET | `/api/authors/{id}` | Autor + seine Bücher |
| POST | `/api/loans` | Ausleihantrag `{memberId, bookId, days}` |
| POST | `/api/loans/{id}/approve` · `/reject` | Ausleihe freigeben / ablehnen |
| POST | `/api/loans/{id}/return-request` · `/return-approve` · `/return-cancel` | Rückgabe |
| POST | `/api/loans/{id}/extend?days=7` | Verlängern |
| GET | `/api/loans/pending` · `/active?memberId=` · `/overdue` · `/due-soon` | Listen |
| GET | `/api/loans/books/{bookId}/active` | Wer hat das Buch? |
| GET | `/api/loans/members/{memberId}/stats` | Anzahl Bücher, Seiten, Kategorien und Autoren |
| POST | `/api/notifications/loans/{loanId}/reminder` | Manuelle Mahnung |
| POST | `/api/notifications/run` | Täglichen Benachrichtigungs-Job sofort ausführen |
| GET | `/api/notifications?loanId=&memberId=` | Benachrichtigungsprotokoll |
